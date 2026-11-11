package org.kerifoundation.fortandroid

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrScanPolicyTest {

    @Test
    fun `prompt defaults and is bounded`() {
        assertEquals(QrScanPolicy.DEFAULT_PROMPT, QrScanPolicy.prompt(JSONObject()))
        assertEquals(QrScanPolicy.MAX_PROMPT_CHARS, QrScanPolicy.prompt(JSONObject().put("prompt", "p".repeat(300))).length)
    }

    @Test
    fun `no content means the user cancelled`() {
        assertNull(QrScanPolicy.result(null, "QR_CODE"))
    }

    @Test
    fun `KERI OOBI URLs are recognised`() {
        val result = QrScanPolicy.result(
            "https://witness.example.com/oobi/EKYLUMmNPZeEs77Zvclf0bSN5IN-mLfLpx2ySb-HDlk4/witness",
            "QR_CODE"
        )!!

        assertEquals("QR_CODE", result.getString("format"))
        assertTrue(result.getBoolean("isOobi"))
        assertEquals("EKYLUMmNPZeEs77Zvclf0bSN5IN-mLfLpx2ySb-HDlk4", result.getString("oobiAid"))
    }

    @Test
    fun `plain text is returned but not flagged as OOBI`() {
        val result = QrScanPolicy.result("hello", "QR_CODE")!!

        assertEquals("hello", result.getString("text"))
        assertFalse(result.getBoolean("isOobi"))
        assertFalse(result.has("oobiAid"))
    }

    @Test
    fun `oversized content is truncated and flagged`() {
        val result = QrScanPolicy.result("x".repeat(QrScanPolicy.MAX_TEXT_CHARS + 10), "QR_CODE")!!

        assertEquals(QrScanPolicy.MAX_TEXT_CHARS, result.getString("text").length)
        assertTrue(result.getBoolean("truncated"))
    }
}
