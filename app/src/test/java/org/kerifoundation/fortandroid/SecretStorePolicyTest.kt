package org.kerifoundation.fortandroid

import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecretStorePolicyTest {

    @Test
    fun `valid key is namespaced for storage`() {
        assertEquals("secret.vault-0a1b", SecretStorePolicy.storageKey(JSONObject().put("key", "vault-0a1b")))
    }

    @Test
    fun `missing or unsafe key is rejected`() {
        for (params in listOf(
            JSONObject(),
            JSONObject().put("key", ""),
            JSONObject().put("key", 42),
            JSONObject().put("key", "a b"),
            JSONObject().put("key", "../x"),
            JSONObject().put("key", "k".repeat(SecretStorePolicy.MAX_KEY_CHARS + 1))
        )) {
            assertNull("params $params", SecretStorePolicy.storageKey(params))
        }
    }

    @Test
    fun `secret value must be a non-empty bounded string`() {
        assertEquals("passcode-123", SecretStorePolicy.secretValue(JSONObject().put("value", "passcode-123")))
        assertNull(SecretStorePolicy.secretValue(JSONObject()))
        assertNull(SecretStorePolicy.secretValue(JSONObject().put("value", "")))
        assertNull(SecretStorePolicy.secretValue(JSONObject().put("value", 7)))
        assertNull(
            SecretStorePolicy.secretValue(JSONObject().put("value", "v".repeat(SecretStorePolicy.MAX_VALUE_CHARS + 1)))
        )
    }

    @Test
    fun `blob round-trips iv and ciphertext`() {
        val iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val ciphertext = byteArrayOf(9, 8, 7, 0, -1, -128, 127)

        val decoded = SecretStorePolicy.decodeBlob(SecretStorePolicy.encodeBlob(iv, ciphertext))!!

        assertArrayEquals(iv, decoded.iv)
        assertArrayEquals(ciphertext, decoded.ciphertext)
    }

    @Test
    fun `malformed blob decodes to null`() {
        for (text in listOf(null, "", "nocolon", ":", "AAAA:", ":AAAA", "!!:AAAA", "AAAA:!!", "a:b:c")) {
            assertNull("blob '$text'", SecretStorePolicy.decodeBlob(text))
        }
    }

    @Test
    fun `internal keystore alias cannot be reached by page key aliases`() {
        val pageAlias = KeystorePolicy.keystoreAlias(JSONObject().put("alias", "secret-store"))

        assertEquals(false, pageAlias == SecretStorePolicy.KEYSTORE_ALIAS)
        assertEquals(false, SecretStorePolicy.KEYSTORE_ALIAS.startsWith("fortoid."))
    }
}
