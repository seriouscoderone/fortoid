package org.kerifoundation.fortandroid

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import androidx.biometric.BiometricPrompt
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import org.json.JSONObject
import org.kerifoundation.fort.bridge.NativeBridgeProtocol
import org.kerifoundation.fort.bridge.NativeCapability
import org.kerifoundation.fort.bridge.NativeReply

/**
 * Hardware-backed EC P-256 signing keys in the Android Keystore.
 *
 * Capabilities (all take `alias`):
 *  - keystore.generate  {alias, requireBiometric?=true}
 *  - keystore.publicKey {alias}
 *  - keystore.sign      {alias, data: base64, title?, subtitle?}
 *  - keystore.delete    {alias}
 *
 * Private keys never leave the secure hardware; with requireBiometric each
 * signature needs a fresh strong-biometric match.
 */
internal class HardwareKeystore(
    private val context: Context,
    private val gate: BiometricGate
) {
    private val keyStore: KeyStore by lazy { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) } }

    fun capabilities(): Map<String, NativeCapability> = mapOf(
        "keystore.generate" to NativeCapability(::generate),
        "keystore.publicKey" to NativeCapability(::publicKey),
        "keystore.sign" to NativeCapability(::sign),
        "keystore.delete" to NativeCapability(::delete)
    )

    private fun generate(params: JSONObject, reply: NativeReply) {
        val alias = KeystorePolicy.keystoreAlias(params) ?: return reply.badAlias()
        if (keyStore.containsAlias(alias)) {
            reply.ok(describe(alias, created = false))
            return
        }

        val requireBiometric = params.optBoolean("requireBiometric", true)
        if (requireBiometric) {
            gate.unavailableReason()?.let {
                reply.fail(NativeBridgeProtocol.ERROR_UNAVAILABLE, "$it (try requireBiometric: false)")
                return
            }
        }

        val wantStrongBox = context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        try {
            generateKey(alias, requireBiometric, strongBox = wantStrongBox)
        } catch (e: StrongBoxUnavailableException) {
            generateKey(alias, requireBiometric, strongBox = false)
        }
        reply.ok(describe(alias, created = true))
    }

    private fun publicKey(params: JSONObject, reply: NativeReply) {
        val alias = KeystorePolicy.keystoreAlias(params) ?: return reply.badAlias()
        if (!keyStore.containsAlias(alias)) return reply.noSuchKey()
        reply.ok(describe(alias, created = false))
    }

    private fun sign(params: JSONObject, reply: NativeReply) {
        val alias = KeystorePolicy.keystoreAlias(params) ?: return reply.badAlias()
        val data = KeystorePolicy.dataToSign(params)
            ?: return reply.fail(
                NativeBridgeProtocol.ERROR_BAD_REQUEST,
                "data must be base64, 1..${KeystorePolicy.MAX_SIGN_BYTES} bytes"
            )
        val privateKey = keyStore.getKey(alias, null) as? PrivateKey ?: return reply.noSuchKey()

        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        try {
            signature.initSign(privateKey)
        } catch (e: KeyPermanentlyInvalidatedException) {
            reply.fail(
                NativeBridgeProtocol.ERROR_FAILED,
                "key invalidated by a biometric enrollment change; delete and recreate it"
            )
            return
        }

        if (!keyInfo(privateKey).isUserAuthenticationRequired) {
            reply.finishSignature(signature, data)
            return
        }

        val text = BiometricPolicy.promptText(params.apply { if (!has("title")) put("title", "Sign with your key") })
        gate.authenticate(text, BiometricPrompt.CryptoObject(signature)) { outcome ->
            when (outcome) {
                is BiometricGate.Outcome.Success -> {
                    val unlocked = outcome.cryptoObject?.signature
                    if (unlocked == null) {
                        reply.fail(NativeBridgeProtocol.ERROR_FAILED, "biometric prompt returned no signature")
                    } else {
                        reply.finishSignature(unlocked, data)
                    }
                }
                is BiometricGate.Outcome.Failure -> reply.fail(outcome.code, outcome.message)
            }
        }
    }

    private fun delete(params: JSONObject, reply: NativeReply) {
        val alias = KeystorePolicy.keystoreAlias(params) ?: return reply.badAlias()
        val existed = keyStore.containsAlias(alias)
        if (existed) keyStore.deleteEntry(alias)
        reply.ok(JSONObject().put("deleted", existed))
    }

    private fun generateKey(alias: String, requireBiometric: Boolean, strongBox: Boolean) {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .apply {
                if (requireBiometric) {
                    setUserAuthenticationRequired(true)
                    setInvalidatedByBiometricEnrollment(true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                    } else {
                        @Suppress("DEPRECATION")
                        setUserAuthenticationValidityDurationSeconds(-1)
                    }
                }
                if (strongBox) setIsStrongBoxBacked(true)
            }
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE).run {
            initialize(spec)
            generateKeyPair()
        }
    }

    private fun describe(alias: String, created: Boolean): JSONObject {
        val privateKey = keyStore.getKey(alias, null) as PrivateKey
        val info = keyInfo(privateKey)
        return JSONObject()
            .put("alias", alias.substringAfter('.'))
            .put("created", created)
            .put("algorithm", "ES256 (ECDSA P-256 / SHA-256)")
            .put("publicKey", KeystorePolicy.base64(keyStore.getCertificate(alias).publicKey.encoded))
            .put("publicKeyFormat", "SPKI DER, base64")
            .put("securityLevel", securityLevel(info))
            .put("userAuthRequired", info.isUserAuthenticationRequired)
    }

    private fun keyInfo(privateKey: PrivateKey): KeyInfo =
        KeyFactory.getInstance(privateKey.algorithm, ANDROID_KEYSTORE).getKeySpec(privateKey, KeyInfo::class.java)

    private fun securityLevel(info: KeyInfo): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return when (info.securityLevel) {
                KeyProperties.SECURITY_LEVEL_STRONGBOX -> "strongbox"
                KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> "tee"
                KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE -> "secure-hardware"
                KeyProperties.SECURITY_LEVEL_SOFTWARE -> "software"
                else -> "unknown"
            }
        }
        @Suppress("DEPRECATION")
        return if (info.isInsideSecureHardware) "secure-hardware" else "software"
    }

    private fun NativeReply.finishSignature(signature: Signature, data: ByteArray) {
        try {
            signature.update(data)
            val der = signature.sign()
            ok(
                JSONObject()
                    .put("algorithm", "ES256")
                    .put("signature", KeystorePolicy.base64(der))
                    .put("signatureFormat", "ASN.1 DER, base64")
                    .put("signatureP1363", KeystorePolicy.base64(KeystorePolicy.derToP1363(der, 32)))
            )
        } catch (e: Exception) {
            fail(NativeBridgeProtocol.ERROR_FAILED, "signing failed: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun NativeReply.badAlias() =
        fail(NativeBridgeProtocol.ERROR_BAD_REQUEST, "alias must be 1..${KeystorePolicy.MAX_ALIAS_CHARS} of A-Z a-z 0-9 _ -")

    private fun NativeReply.noSuchKey() = fail(NativeBridgeProtocol.ERROR_BAD_REQUEST, "no key with that alias")

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    }
}
