package org.kerifoundation.fortandroid

import java.util.Base64
import org.json.JSONObject

/** Pure input rules and storage encoding for the biometric secret store. */
internal object SecretStorePolicy {
    const val MAX_KEY_CHARS = 64
    const val MAX_VALUE_CHARS = 1024

    /**
     * AndroidKeyStore alias of the store's AES key. Deliberately outside the
     * `fortoid.` namespace that page-chosen keystore.* aliases live in.
     */
    const val KEYSTORE_ALIAS = "fortoid-internal.secret-store"

    private const val STORAGE_PREFIX = "secret."
    private val KEY_PATTERN = Regex("^[A-Za-z0-9_-]+$")

    data class Blob(val iv: ByteArray, val ciphertext: ByteArray)

    fun storageKey(params: JSONObject): String? {
        val key = params.opt("key") as? String ?: return null
        if (key.isEmpty() || key.length > MAX_KEY_CHARS || !KEY_PATTERN.matches(key)) return null
        return STORAGE_PREFIX + key
    }

    fun secretValue(params: JSONObject): String? {
        val value = params.opt("value") as? String ?: return null
        return value.takeIf { it.isNotEmpty() && it.length <= MAX_VALUE_CHARS }
    }

    fun encodeBlob(iv: ByteArray, ciphertext: ByteArray): String =
        Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(ciphertext)

    fun decodeBlob(text: String?): Blob? {
        val parts = text?.split(':') ?: return null
        if (parts.size != 2 || parts.any { it.isEmpty() }) return null
        return try {
            Blob(Base64.getDecoder().decode(parts[0]), Base64.getDecoder().decode(parts[1]))
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
