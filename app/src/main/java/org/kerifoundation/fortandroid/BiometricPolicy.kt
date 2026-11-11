package org.kerifoundation.fortandroid

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import org.json.JSONObject
import org.kerifoundation.fort.bridge.NativeBridgeProtocol

/** Pure decisions for biometric prompts: page-supplied text and error mapping. */
internal object BiometricPolicy {
    const val DEFAULT_TITLE = "Confirm it's you"
    const val MAX_TEXT_CHARS = 80

    data class PromptText(val title: String, val subtitle: String?)

    fun promptText(params: JSONObject): PromptText =
        PromptText(
            title = boundedText(params.opt("title")) ?: DEFAULT_TITLE,
            subtitle = boundedText(params.opt("subtitle"))
        )

    fun errorCodeFor(biometricErrorCode: Int): String = when (biometricErrorCode) {
        BiometricPrompt.ERROR_USER_CANCELED,
        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
        BiometricPrompt.ERROR_CANCELED -> NativeBridgeProtocol.ERROR_CANCELLED

        BiometricPrompt.ERROR_HW_NOT_PRESENT,
        BiometricPrompt.ERROR_HW_UNAVAILABLE,
        BiometricPrompt.ERROR_NO_BIOMETRICS -> NativeBridgeProtocol.ERROR_UNAVAILABLE

        else -> NativeBridgeProtocol.ERROR_FAILED
    }

    /** Null when a strong-biometric prompt can be shown; otherwise a short reason. */
    fun unavailableReason(canAuthenticateStatus: Int): String? = when (canAuthenticateStatus) {
        BiometricManager.BIOMETRIC_SUCCESS -> null
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "no fingerprint or face enrolled"
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "no biometric hardware"
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "biometric hardware unavailable"
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "security update required"
        else -> "strong biometrics not supported"
    }

    private fun boundedText(value: Any?): String? {
        val text = (value as? String)?.trim()
        if (text.isNullOrEmpty()) return null
        return text.take(MAX_TEXT_CHARS)
    }
}
