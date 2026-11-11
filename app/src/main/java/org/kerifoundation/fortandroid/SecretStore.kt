package org.kerifoundation.fortandroid

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import androidx.biometric.BiometricPrompt
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject
import org.kerifoundation.fort.bridge.NativeBridgeProtocol
import org.kerifoundation.fort.bridge.NativeCapability
import org.kerifoundation.fort.bridge.NativeReply

/**
 * Biometric-gated secret store (e.g. a vault passcode for fingerprint unlock).
 *
 *  - secret.status   {key}                          → {stored, biometricAvailable, reason?}
 *  - secret.store    {key, value, title?, subtitle?} → {stored: true}   (biometric)
 *  - secret.retrieve {key, title?, subtitle?}        → {value}          (biometric)
 *  - secret.delete   {key}                          → {deleted}
 *
 * One AES-256-GCM key lives in AndroidKeyStore (StrongBox when available) and
 * requires a fresh strong-biometric match for every use. Ciphertexts live in
 * app-private SharedPreferences; they are useless without that key. A new
 * biometric enrollment invalidates the key: everything is wiped and the page
 * gets `invalidated`. Secret values are never logged.
 */
internal class SecretStore(
    private val context: Context,
    private val gate: BiometricGate
) {
    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val keyStore: KeyStore by lazy { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) } }

    fun capabilities(): Map<String, NativeCapability> = mapOf(
        "secret.status" to NativeCapability(::status),
        "secret.store" to NativeCapability(::store),
        "secret.retrieve" to NativeCapability(::retrieve),
        "secret.delete" to NativeCapability(::delete)
    )

    private fun status(params: JSONObject, reply: NativeReply) {
        val key = SecretStorePolicy.storageKey(params) ?: return reply.badKey()
        val unavailable = gate.unavailableReason()
        reply.ok(
            JSONObject()
                .put("stored", prefs.contains(key))
                .put("biometricAvailable", unavailable == null)
                .apply { unavailable?.let { put("reason", it) } }
        )
    }

    private fun store(params: JSONObject, reply: NativeReply) {
        val key = SecretStorePolicy.storageKey(params) ?: return reply.badKey()
        val value = SecretStorePolicy.secretValue(params)
            ?: return reply.fail(
                NativeBridgeProtocol.ERROR_BAD_REQUEST,
                "value must be 1..${SecretStorePolicy.MAX_VALUE_CHARS} characters"
            )
        val cipher = initCipher(Cipher.ENCRYPT_MODE, iv = null, reply) ?: return

        gate.authenticate(promptText(params, "Turn on fingerprint unlock"), BiometricPrompt.CryptoObject(cipher)) { outcome ->
            when (outcome) {
                is BiometricGate.Outcome.Failure -> reply.fail(outcome.code, outcome.message)
                is BiometricGate.Outcome.Success -> {
                    val unlocked = outcome.cryptoObject?.cipher
                        ?: return@authenticate reply.fail(NativeBridgeProtocol.ERROR_FAILED, "no cipher returned")
                    try {
                        val ciphertext = unlocked.doFinal(value.toByteArray(Charsets.UTF_8))
                        prefs.edit { putString(key, SecretStorePolicy.encodeBlob(unlocked.iv, ciphertext)) }
                        reply.ok(JSONObject().put("stored", true))
                    } catch (e: Exception) {
                        reply.fail(NativeBridgeProtocol.ERROR_FAILED, "encryption failed: ${e.javaClass.simpleName}")
                    }
                }
            }
        }
    }

    private fun retrieve(params: JSONObject, reply: NativeReply) {
        val key = SecretStorePolicy.storageKey(params) ?: return reply.badKey()
        val blob = SecretStorePolicy.decodeBlob(prefs.getString(key, null))
            ?: return reply.fail(NativeBridgeProtocol.ERROR_BAD_REQUEST, "nothing stored for that key")
        val cipher = initCipher(Cipher.DECRYPT_MODE, blob.iv, reply) ?: return

        gate.authenticate(promptText(params, "Unlock with fingerprint"), BiometricPrompt.CryptoObject(cipher)) { outcome ->
            when (outcome) {
                is BiometricGate.Outcome.Failure -> reply.fail(outcome.code, outcome.message)
                is BiometricGate.Outcome.Success -> {
                    val unlocked = outcome.cryptoObject?.cipher
                        ?: return@authenticate reply.fail(NativeBridgeProtocol.ERROR_FAILED, "no cipher returned")
                    try {
                        val value = String(unlocked.doFinal(blob.ciphertext), Charsets.UTF_8)
                        reply.ok(JSONObject().put("value", value))
                    } catch (e: AEADBadTagException) {
                        prefs.edit { remove(key) }
                        reply.fail(NativeBridgeProtocol.ERROR_FAILED, "stored secret was corrupt and has been removed")
                    } catch (e: Exception) {
                        reply.fail(NativeBridgeProtocol.ERROR_FAILED, "decryption failed: ${e.javaClass.simpleName}")
                    }
                }
            }
        }
    }

    private fun delete(params: JSONObject, reply: NativeReply) {
        val key = SecretStorePolicy.storageKey(params) ?: return reply.badKey()
        val existed = prefs.contains(key)
        if (existed) prefs.edit { remove(key) }
        reply.ok(JSONObject().put("deleted", existed))
    }

    /** Null (after replying) when unavailable or invalidated by a biometric enrollment change. */
    private fun initCipher(mode: Int, iv: ByteArray?, reply: NativeReply): Cipher? {
        gate.unavailableReason()?.let {
            reply.fail(NativeBridgeProtocol.ERROR_UNAVAILABLE, it)
            return null
        }
        return try {
            Cipher.getInstance(TRANSFORMATION).apply {
                if (iv == null) init(mode, secretKey()) else init(mode, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            wipeAll()
            reply.fail(
                NativeBridgeProtocol.ERROR_INVALIDATED,
                "a new fingerprint or face was added to this phone, so saved fingerprint unlocks were removed"
            )
            null
        }
    }

    private fun wipeAll() {
        keyStore.deleteEntry(SecretStorePolicy.KEYSTORE_ALIAS)
        prefs.edit { clear() }
    }

    private fun secretKey(): SecretKey {
        (keyStore.getKey(SecretStorePolicy.KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }
        val wantStrongBox = context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        return try {
            generateKey(strongBox = wantStrongBox)
        } catch (e: StrongBoxUnavailableException) {
            generateKey(strongBox = false)
        }
    }

    private fun generateKey(strongBox: Boolean): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            SecretStorePolicy.KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                } else {
                    @Suppress("DEPRECATION")
                    setUserAuthenticationValidityDurationSeconds(-1)
                }
                if (strongBox) setIsStrongBoxBacked(true)
            }
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(spec)
            generateKey()
        }
    }

    private fun promptText(params: JSONObject, defaultTitle: String): BiometricPolicy.PromptText {
        val text = BiometricPolicy.promptText(params)
        return if (params.has("title")) text else text.copy(title = defaultTitle)
    }

    private fun NativeReply.badKey() =
        fail(NativeBridgeProtocol.ERROR_BAD_REQUEST, "key must be 1..${SecretStorePolicy.MAX_KEY_CHARS} of A-Z a-z 0-9 _ -")

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val PREFS_NAME = "fortoid_secrets"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}
