package org.kerifoundation.fortandroid

import org.json.JSONObject

/** Pure rules for qr.scan: prompt text and how a scan result is reported to the page. */
internal object QrScanPolicy {
    const val DEFAULT_PROMPT = "Point the camera at a QR code"
    const val MAX_PROMPT_CHARS = 80
    const val MAX_TEXT_CHARS = 2048

    // KERI OOBI: http(s)://host[:port]/.../oobi/<44-char CESR AID>[/role[/eid]]
    private val OOBI_PATTERN = Regex("^https?://\\S+?/oobi/([A-Za-z0-9_-]{44})(?:[/?#]\\S*)?$")

    fun prompt(params: JSONObject): String =
        (params.opt("prompt") as? String)?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_PROMPT_CHARS)
            ?: DEFAULT_PROMPT

    /** Null when nothing was scanned (user backed out). */
    fun result(contents: String?, format: String?): JSONObject? {
        if (contents == null) return null

        val text = contents.take(MAX_TEXT_CHARS)
        val oobiAid = OOBI_PATTERN.matchEntire(contents)?.groupValues?.get(1)
        return JSONObject()
            .put("text", text)
            .put("format", format ?: "unknown")
            .put("truncated", contents.length > MAX_TEXT_CHARS)
            .put("isOobi", oobiAid != null)
            .apply { oobiAid?.let { put("oobiAid", it) } }
    }
}
