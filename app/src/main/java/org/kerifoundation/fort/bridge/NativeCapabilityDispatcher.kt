package org.kerifoundation.fort.bridge

import org.json.JSONObject
import org.kerifoundation.fort.bridge.NativeBridgeProtocol.ParseResult

/** A phone capability callable from the WebView. Must reply exactly once via [reply]. */
fun interface NativeCapability {
    fun invoke(params: JSONObject, reply: NativeReply)
}

/** One-shot reply channel for a single native request; later replies are dropped. */
class NativeReply internal constructor(
    private val id: String,
    private val send: (String) -> Unit
) {
    private var replied = false

    fun ok(result: JSONObject) = deliver { NativeBridgeProtocol.success(id, result) }

    fun fail(code: String, message: String) = deliver { NativeBridgeProtocol.failure(id, code, message) }

    private fun deliver(build: () -> String) {
        if (replied) return
        replied = true
        send(build())
    }
}

class NativeCapabilityDispatcher(private val capabilities: Map<String, NativeCapability>) {

    fun dispatch(envelope: JSONObject, send: (String) -> Unit) {
        val request = when (val parsed = NativeBridgeProtocol.parse(envelope)) {
            is ParseResult.Ok -> parsed.request
            is ParseResult.Error -> {
                parsed.id?.let { send(NativeBridgeProtocol.failure(it, parsed.code, parsed.message)) }
                return
            }
        }

        val reply = NativeReply(request.id, send)
        val capability = capabilities[request.capability]
        if (capability == null) {
            reply.fail(NativeBridgeProtocol.ERROR_UNKNOWN_CAPABILITY, "unknown capability ${request.capability}")
            return
        }

        try {
            capability.invoke(request.params, reply)
        } catch (e: Exception) {
            reply.fail(NativeBridgeProtocol.ERROR_FAILED, e.message ?: e.javaClass.simpleName)
        }
    }
}
