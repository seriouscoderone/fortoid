package org.kerifoundation.fortandroid

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.kerifoundation.fort.bridge.NativeBridgeProtocol

class BiometricPolicyTest {

    @Test
    fun `prompt text uses defaults when params are absent`() {
        val text = BiometricPolicy.promptText(JSONObject())

        assertEquals(BiometricPolicy.DEFAULT_TITLE, text.title)
        assertNull(text.subtitle)
    }

    @Test
    fun `prompt text is trimmed and bounded`() {
        val text = BiometricPolicy.promptText(
            JSONObject().put("title", "  Sign in  ").put("subtitle", "y".repeat(500))
        )

        assertEquals("Sign in", text.title)
        assertEquals(BiometricPolicy.MAX_TEXT_CHARS, text.subtitle!!.length)
    }

    @Test
    fun `non-string or blank title falls back to default`() {
        assertEquals(BiometricPolicy.DEFAULT_TITLE, BiometricPolicy.promptText(JSONObject().put("title", 5)).title)
        assertEquals(BiometricPolicy.DEFAULT_TITLE, BiometricPolicy.promptText(JSONObject().put("title", "  ")).title)
    }

    @Test
    fun `user cancellations map to cancelled`() {
        for (code in listOf(
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_CANCELED
        )) {
            assertEquals(NativeBridgeProtocol.ERROR_CANCELLED, BiometricPolicy.errorCodeFor(code))
        }
    }

    @Test
    fun `missing hardware or enrollment maps to unavailable`() {
        for (code in listOf(
            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_HW_UNAVAILABLE,
            BiometricPrompt.ERROR_NO_BIOMETRICS
        )) {
            assertEquals(NativeBridgeProtocol.ERROR_UNAVAILABLE, BiometricPolicy.errorCodeFor(code))
        }
    }

    @Test
    fun `lockout and other errors map to failed`() {
        assertEquals(NativeBridgeProtocol.ERROR_FAILED, BiometricPolicy.errorCodeFor(BiometricPrompt.ERROR_LOCKOUT))
        assertEquals(NativeBridgeProtocol.ERROR_FAILED, BiometricPolicy.errorCodeFor(9999))
    }

    @Test
    fun `availability status explains why a prompt cannot be shown`() {
        assertNull(BiometricPolicy.unavailableReason(BiometricManager.BIOMETRIC_SUCCESS))
        assertEquals(
            "no fingerprint or face enrolled",
            BiometricPolicy.unavailableReason(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
        )
        assertEquals(
            "no biometric hardware",
            BiometricPolicy.unavailableReason(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)
        )
    }
}
