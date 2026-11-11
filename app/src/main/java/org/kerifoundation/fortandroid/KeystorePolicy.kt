package org.kerifoundation.fortandroid

import java.util.Base64
import org.json.JSONObject

/** Pure input rules and encoding helpers for the hardware keystore capability. */
internal object KeystorePolicy {
    const val MAX_ALIAS_CHARS = 48
    const val MAX_SIGN_BYTES = 2048

    /** Page-chosen aliases live under this prefix so the page cannot touch other app keys. */
    private const val ALIAS_PREFIX = "fortoid."
    private val ALIAS_PATTERN = Regex("^[A-Za-z0-9_-]+$")

    fun keystoreAlias(params: JSONObject): String? {
        val alias = params.opt("alias") as? String ?: return null
        if (alias.isEmpty() || alias.length > MAX_ALIAS_CHARS || !ALIAS_PATTERN.matches(alias)) return null
        return ALIAS_PREFIX + alias
    }

    fun dataToSign(params: JSONObject): ByteArray? {
        val encoded = params.opt("data") as? String ?: return null
        val bytes = try {
            Base64.getDecoder().decode(encoded)
        } catch (e: IllegalArgumentException) {
            return null
        }
        return bytes.takeIf { it.isNotEmpty() && it.size <= MAX_SIGN_BYTES }
    }

    fun base64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    /**
     * Converts an ASN.1 DER ECDSA signature (SEQUENCE { INTEGER r, INTEGER s })
     * into fixed-width r||s (IEEE P1363), the form WebCrypto and CESR use.
     */
    fun derToP1363(der: ByteArray, componentBytes: Int): ByteArray {
        var index = 0
        fun readLength(): Int {
            val first = der[index++].toInt() and 0xff
            if (first < 0x80) return first
            var length = 0
            repeat(first and 0x7f) { length = (length shl 8) or (der[index++].toInt() and 0xff) }
            return length
        }
        fun readInteger(): ByteArray {
            require(der[index++].toInt() == 0x02) { "expected INTEGER" }
            val length = readLength()
            val value = der.copyOfRange(index, index + length)
            index += length
            val unsigned = value.dropWhile { it == 0.toByte() }.toByteArray()
            require(unsigned.size <= componentBytes) { "component too large" }
            return ByteArray(componentBytes - unsigned.size) + unsigned
        }

        require(der[index++].toInt() == 0x30) { "expected SEQUENCE" }
        readLength()
        return readInteger() + readInteger()
    }
}
