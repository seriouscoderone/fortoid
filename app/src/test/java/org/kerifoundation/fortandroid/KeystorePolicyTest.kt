package org.kerifoundation.fortandroid

import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeystorePolicyTest {

    @Test
    fun `valid alias is namespaced for the keystore`() {
        assertEquals("fortoid.demo-key_1", KeystorePolicy.keystoreAlias(JSONObject().put("alias", "demo-key_1")))
    }

    @Test
    fun `missing or unsafe alias is rejected`() {
        for (params in listOf(
            JSONObject(),
            JSONObject().put("alias", ""),
            JSONObject().put("alias", 7),
            JSONObject().put("alias", "has space"),
            JSONObject().put("alias", "../x"),
            JSONObject().put("alias", "a".repeat(KeystorePolicy.MAX_ALIAS_CHARS + 1))
        )) {
            assertNull("params $params", KeystorePolicy.keystoreAlias(params))
        }
    }

    @Test
    fun `data to sign is base64 decoded`() {
        val params = JSONObject().put("data", Base64.getEncoder().encodeToString("hello".toByteArray()))

        assertArrayEquals("hello".toByteArray(), KeystorePolicy.dataToSign(params))
    }

    @Test
    fun `invalid empty or oversized data is rejected`() {
        val tooBig = Base64.getEncoder().encodeToString(ByteArray(KeystorePolicy.MAX_SIGN_BYTES + 1))
        for (params in listOf(
            JSONObject(),
            JSONObject().put("data", "not base64!!"),
            JSONObject().put("data", ""),
            JSONObject().put("data", tooBig)
        )) {
            assertNull("params ${params.toString().take(40)}", KeystorePolicy.dataToSign(params))
        }
    }

    @Test
    fun `DER ECDSA signature converts to the P1363 form WebCrypto verifies`() {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = generator.generateKeyPair()
        val message = "fortoid".toByteArray()

        repeat(20) {
            val der = Signature.getInstance("SHA256withECDSA").run {
                initSign(keyPair.private)
                update(message)
                sign()
            }

            val p1363 = KeystorePolicy.derToP1363(der, 32)

            assertEquals(64, p1363.size)
            val verifier = Signature.getInstance("SHA256withECDSAinP1363Format")
            verifier.initVerify(keyPair.public)
            verifier.update(message)
            assertTrue(verifier.verify(p1363))
        }
    }
}
