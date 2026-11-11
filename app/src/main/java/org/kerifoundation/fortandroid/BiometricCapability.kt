package org.kerifoundation.fortandroid

import org.json.JSONObject
import org.kerifoundation.fort.bridge.NativeCapability
import org.kerifoundation.fort.bridge.NativeReply

/** `biometric.authenticate` — params: optional `title`, `subtitle`. */
internal class BiometricCapability(private val gate: BiometricGate) : NativeCapability {
    override fun invoke(params: JSONObject, reply: NativeReply) {
        gate.authenticate(BiometricPolicy.promptText(params), cryptoObject = null) { outcome ->
            when (outcome) {
                is BiometricGate.Outcome.Success -> reply.ok(JSONObject().put("authenticated", true))
                is BiometricGate.Outcome.Failure -> reply.fail(outcome.code, outcome.message)
            }
        }
    }
}
