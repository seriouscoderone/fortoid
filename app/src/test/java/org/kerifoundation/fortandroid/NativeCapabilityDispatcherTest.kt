package org.kerifoundation.fortandroid

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kerifoundation.fort.bridge.NativeCapability
import org.kerifoundation.fort.bridge.NativeCapabilityDispatcher

class NativeCapabilityDispatcherTest {

    private val sent = mutableListOf<JSONObject>()
    private val send: (String) -> Unit = { sent.add(JSONObject(it)) }

    private fun request(capability: String, id: String = "r1") =
        JSONObject().put("type", "native_request").put("id", id).put("capability", capability)

    @Test
    fun `known capability is invoked and its result returned`() {
        val echo = NativeCapability { params, reply -> reply.ok(JSONObject().put("echo", params.length())) }
        val dispatcher = NativeCapabilityDispatcher(mapOf("test.echo" to echo))

        dispatcher.dispatch(request("test.echo"), send)

        assertEquals(1, sent.size)
        assertTrue(sent[0].getBoolean("ok"))
        assertEquals("r1", sent[0].getString("id"))
        assertEquals(0, sent[0].getJSONObject("result").getInt("echo"))
    }

    @Test
    fun `unknown capability gets unknown_capability error`() {
        NativeCapabilityDispatcher(emptyMap()).dispatch(request("nope.nothing"), send)

        assertEquals(1, sent.size)
        assertFalse(sent[0].getBoolean("ok"))
        assertEquals("unknown_capability", sent[0].getJSONObject("error").getString("code"))
    }

    @Test
    fun `invalid request with id gets bad_request error`() {
        NativeCapabilityDispatcher(emptyMap()).dispatch(request("BAD"), send)

        assertEquals("bad_request", sent.single().getJSONObject("error").getString("code"))
    }

    @Test
    fun `invalid request without id sends nothing`() {
        NativeCapabilityDispatcher(emptyMap()).dispatch(JSONObject().put("capability", "a.b"), send)

        assertTrue(sent.isEmpty())
    }

    @Test
    fun `throwing capability gets failed error`() {
        val boom = NativeCapability { _, _ -> throw IllegalStateException("boom") }
        NativeCapabilityDispatcher(mapOf("test.boom" to boom)).dispatch(request("test.boom"), send)

        assertEquals("failed", sent.single().getJSONObject("error").getString("code"))
    }

    @Test
    fun `only the first reply is delivered`() {
        val chatty = NativeCapability { _, reply ->
            reply.ok(JSONObject())
            reply.fail("failed", "late")
            reply.ok(JSONObject())
        }
        NativeCapabilityDispatcher(mapOf("test.chatty" to chatty)).dispatch(request("test.chatty"), send)

        assertEquals(1, sent.size)
        assertTrue(sent[0].getBoolean("ok"))
    }
}
