package org.kerifoundation.fortandroid

import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject
import org.kerifoundation.fort.bridge.NativeBridgeProtocol
import org.kerifoundation.fort.bridge.NativeCapability
import org.kerifoundation.fort.bridge.NativeReply

/** Starts the camera scanner; returns false when a scan is already in progress. */
internal fun interface QrScanLauncher {
    fun launch(options: ScanOptions, onResult: (ScanIntentResult) -> Unit): Boolean
}

/** `qr.scan` — params: optional `prompt`. Camera permission is requested by the scanner. */
internal class QrScanCapability(private val launcher: QrScanLauncher) : NativeCapability {
    override fun invoke(params: JSONObject, reply: NativeReply) {
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt(QrScanPolicy.prompt(params))
            .setBeepEnabled(false)
            .setOrientationLocked(true)

        val started = launcher.launch(options) { scan ->
            val result = QrScanPolicy.result(scan.contents, scan.formatName)
            if (result == null) {
                reply.fail(NativeBridgeProtocol.ERROR_CANCELLED, "scan cancelled")
            } else {
                reply.ok(result)
            }
        }
        if (!started) reply.fail(NativeBridgeProtocol.ERROR_BUSY, "a scan is already in progress")
    }
}
