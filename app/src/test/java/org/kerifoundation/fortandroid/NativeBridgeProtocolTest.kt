package org.kerifoundation.fortandroid

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kerifoundation.fort.bridge.NativeBridgeProtocol
import org.kerifoundation.fort.bridge.NativeBridgeProtocol.ParseResult

class NativeBridgeProtocolTest {

    private fun parse(json: String): ParseResult = NativeBridgeProtocol.parse(JSONObject(json))

    @Test
    fun `valid request parses with params`() {
        val result = parse(
            """{"type":"native_request","id":"r1","capability":"keystore.sign","params":{"alias":"a"}}"""
        )

        assertTrue("expected Ok, got $result", result is ParseResult.Ok)
        val request = (result as ParseResult.Ok).request
        assertEquals("r1", request.id)
        assertEquals("keystore.sign", request.capability)
        assertEquals("a", request.params.getString("alias"))
    }

    @Test
    fun `missing params defaults to empty object`() {
        val result = parse("""{"type":"native_request","id":"r2","capability":"device.info"}""")

        val request = (result as ParseResult.Ok).request
        assertEquals(0, request.params.length())
    }

    @Test
    fun `missing id is rejected without an id to reply to`() {
        val result = parse("""{"type":"native_request","capability":"device.info"}""")

        val error = result as ParseResult.Error
        assertNull(error.id)
        assertEquals(NativeBridgeProtocol.ERROR_BAD_REQUEST, error.code)
    }

    @Test
    fun `overlong id is rejected`() {
        val longId = "x".repeat(NativeBridgeProtocol.MAX_ID_CHARS + 1)
        val result = parse("""{"type":"native_request","id":"$longId","capability":"device.info"}""")

        val error = result as ParseResult.Error
        assertNull(error.id)
        assertEquals(NativeBridgeProtocol.ERROR_BAD_REQUEST, error.code)
    }

    @Test
    fun `malformed capability name is rejected but keeps id for the reply`() {
        for (bad in listOf("", "device", "Device.Info", "device..info", "device.info;rm", "../x")) {
            val result = parse("""{"type":"native_request","id":"r3","capability":"$bad"}""")

            val error = result as ParseResult.Error
            assertEquals("capability '$bad'", "r3", error.id)
            assertEquals(NativeBridgeProtocol.ERROR_BAD_REQUEST, error.code)
        }
    }

    @Test
    fun `non-object params is rejected`() {
        val result = parse("""{"type":"native_request","id":"r4","capability":"device.info","params":"x"}""")

        val error = result as ParseResult.Error
        assertEquals("r4", error.id)
        assertEquals(NativeBridgeProtocol.ERROR_BAD_REQUEST, error.code)
    }

    @Test
    fun `success response envelope`() {
        val json = JSONObject(NativeBridgeProtocol.success("r5", JSONObject().put("ok", 1)))

        assertEquals(NativeBridgeProtocol.RESPONSE_TYPE, json.getString("type"))
        assertEquals("r5", json.getString("id"))
        assertTrue(json.getBoolean("ok"))
        assertEquals(1, json.getJSONObject("result").getInt("ok"))
        assertFalse(json.has("error"))
    }

    @Test
    fun `failure response envelope`() {
        val json = JSONObject(
            NativeBridgeProtocol.failure("r6", NativeBridgeProtocol.ERROR_UNKNOWN_CAPABILITY, "nope")
        )

        assertEquals(NativeBridgeProtocol.RESPONSE_TYPE, json.getString("type"))
        assertEquals("r6", json.getString("id"))
        assertFalse(json.getBoolean("ok"))
        assertEquals("unknown_capability", json.getJSONObject("error").getString("code"))
        assertEquals("nope", json.getJSONObject("error").getString("message"))
        assertFalse(json.has("result"))
    }
}
