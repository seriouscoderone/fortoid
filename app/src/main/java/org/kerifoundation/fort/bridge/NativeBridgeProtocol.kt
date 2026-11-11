package org.kerifoundation.fort.bridge

import org.json.JSONObject

/**
 * Request/response protocol for native phone capabilities over the
 * `window.bridge` handler.
 *
 * JS → Android:
 *   {"type":"native_request","id":"<=64 chars","capability":"area.action","params":{...}}
 * Android → JS (via the bridge reply channel):
 *   {"type":"native_response","id":"...","ok":true,"result":{...}}
 *   {"type":"native_response","id":"...","ok":false,"error":{"code":"...","message":"..."}}
 */
object NativeBridgeProtocol {
    const val REQUEST_TYPE = "native_request"
    const val RESPONSE_TYPE = "native_response"
    const val MAX_ID_CHARS = 64

    const val ERROR_BAD_REQUEST = "bad_request"
    const val ERROR_UNKNOWN_CAPABILITY = "unknown_capability"
    const val ERROR_UNAVAILABLE = "unavailable"
    const val ERROR_CANCELLED = "cancelled"
    const val ERROR_BUSY = "busy"
    const val ERROR_FAILED = "failed"

    private val CAPABILITY_PATTERN = Regex("^[a-z][a-zA-Z0-9]*(\\.[a-z][a-zA-Z0-9]*)+$")
    private const val MAX_CAPABILITY_CHARS = 64

    data class NativeRequest(val id: String, val capability: String, val params: JSONObject)

    sealed class ParseResult {
        data class Ok(val request: NativeRequest) : ParseResult()

        /** [id] is null when the request carried no usable id, so no reply can be addressed. */
        data class Error(val id: String?, val code: String, val message: String) : ParseResult()
    }

    fun parse(envelope: JSONObject): ParseResult {
        val id = envelope.opt("id") as? String
        if (id.isNullOrEmpty() || id.length > MAX_ID_CHARS) {
            return ParseResult.Error(null, ERROR_BAD_REQUEST, "missing or invalid id")
        }

        val capability = envelope.opt("capability") as? String
        if (capability == null ||
            capability.length > MAX_CAPABILITY_CHARS ||
            !CAPABILITY_PATTERN.matches(capability)
        ) {
            return ParseResult.Error(id, ERROR_BAD_REQUEST, "missing or invalid capability")
        }

        val params = when (val raw = envelope.opt("params")) {
            null, JSONObject.NULL -> JSONObject()
            is JSONObject -> raw
            else -> return ParseResult.Error(id, ERROR_BAD_REQUEST, "params must be an object")
        }

        return ParseResult.Ok(NativeRequest(id, capability, params))
    }

    fun success(id: String, result: JSONObject): String =
        JSONObject()
            .put("type", RESPONSE_TYPE)
            .put("id", id)
            .put("ok", true)
            .put("result", result)
            .toString()

    fun failure(id: String, code: String, message: String): String =
        JSONObject()
            .put("type", RESPONSE_TYPE)
            .put("id", id)
            .put("ok", false)
            .put("error", JSONObject().put("code", code).put("message", message))
            .toString()
}
