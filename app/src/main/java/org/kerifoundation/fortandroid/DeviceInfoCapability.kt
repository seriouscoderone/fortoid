package org.kerifoundation.fortandroid

import android.os.Build
import org.json.JSONObject
import org.kerifoundation.fort.bridge.NativeCapability
import org.kerifoundation.fort.bridge.NativeReply

/** `device.info`: basic, non-identifying facts about the phone. */
internal class DeviceInfoCapability : NativeCapability {
    override fun invoke(params: JSONObject, reply: NativeReply) {
        reply.ok(
            JSONObject()
                .put("manufacturer", Build.MANUFACTURER)
                .put("model", Build.MODEL)
                .put("androidRelease", Build.VERSION.RELEASE)
                .put("sdkInt", Build.VERSION.SDK_INT)
        )
    }
}
