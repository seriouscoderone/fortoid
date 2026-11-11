package org.kerifoundation.fortandroid

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.kerifoundation.fort.bridge.NativeBridgeProtocol

/**
 * Shows one strong-biometric prompt at a time. Optionally unlocks a
 * [BiometricPrompt.CryptoObject] (e.g. a Keystore signature) with the prompt.
 */
internal class BiometricGate(private val activity: FragmentActivity) {

    sealed class Outcome {
        data class Success(val cryptoObject: BiometricPrompt.CryptoObject?) : Outcome()
        data class Failure(val code: String, val message: String) : Outcome()
    }

    private var inFlight = false

    fun unavailableReason(): String? =
        BiometricPolicy.unavailableReason(BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG))

    fun authenticate(
        text: BiometricPolicy.PromptText,
        cryptoObject: BiometricPrompt.CryptoObject?,
        onOutcome: (Outcome) -> Unit
    ) {
        if (inFlight) {
            onOutcome(Outcome.Failure(NativeBridgeProtocol.ERROR_BUSY, "a biometric prompt is already showing"))
            return
        }
        unavailableReason()?.let {
            onOutcome(Outcome.Failure(NativeBridgeProtocol.ERROR_UNAVAILABLE, it))
            return
        }

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                inFlight = false
                onOutcome(Outcome.Success(result.cryptoObject))
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                inFlight = false
                onOutcome(Outcome.Failure(BiometricPolicy.errorCodeFor(errorCode), errString.toString()))
            }
            // onAuthenticationFailed = a non-matching finger; the prompt stays up for a retry.
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(text.title)
            .apply { text.subtitle?.let { setSubtitle(it) } }
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setNegativeButtonText("Cancel")
            .build()

        inFlight = true
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        if (cryptoObject != null) prompt.authenticate(promptInfo, cryptoObject) else prompt.authenticate(promptInfo)
    }
}
