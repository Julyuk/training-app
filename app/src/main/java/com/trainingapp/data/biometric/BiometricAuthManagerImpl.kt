package com.trainingapp.data.biometric

import android.content.Context
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager as AndroidBiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.trainingapp.data.local.SecurityStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Production implementation of [BiometricAuthManager].
 *
 * Uses [BiometricPrompt] from [androidx.biometric] to show the system
 * authentication dialog and maps every callback to a [AuthState] emission.
 *
 * Callback contract:
 *  - [onAuthenticationSucceeded]  → [AuthState.Success]
 *  - [onAuthenticationFailed]     → stay [AuthState.Authenticating] (user can retry)
 *  - [onAuthenticationError]      → [AuthState.Cancelled] | [AuthState.Unavailable] |
 *                                   [AuthState.Failed] depending on error code
 */
class BiometricAuthManagerImpl(
    private val context: Context,
    private val securityStore: SecurityStore
) : BiometricAuthManager {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override fun checkAvailability(): BiometricType {
        val manager = AndroidBiometricManager.from(context)
        val canAuth = manager.canAuthenticate(AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (canAuth != AndroidBiometricManager.BIOMETRIC_SUCCESS) return BiometricType.NONE
        return detectSensorType()
    }

    override fun authenticate(activity: FragmentActivity, reason: String) {
        if (_authState.value == AuthState.Authenticating) return
        if (checkAvailability() == BiometricType.NONE) {
            _authState.value = AuthState.Unavailable
            return
        }
        _authState.value = AuthState.Authenticating

        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                _authState.value = AuthState.Success
            }

            override fun onAuthenticationFailed() {
                // Biometric presented but not recognised — prompt stays open, user can retry.
                // Do not change authState; Authenticating remains correct.
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                _authState.value = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> AuthState.Cancelled

                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_HW_UNAVAILABLE,
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> AuthState.Unavailable

                    else -> AuthState.Failed(errString.toString())
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Підтвердження особи")
            .setSubtitle(reason)
            .setNegativeButtonText("Скасувати")
            .setAllowedAuthenticators(AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }

    override fun isEnabledByUser(): Boolean = securityStore.isBiometricEnabled()

    override fun resetState() {
        _authState.value = AuthState.Idle
    }

    // ── Sensor type detection ─────────────────────────────────────────────────

    private fun detectSensorType(): BiometricType {
        val pm = context.packageManager
        val hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val hasFace = pm.hasSystemFeature("android.hardware.biometrics.face")
        val hasIris = pm.hasSystemFeature("android.hardware.biometrics.iris")
        return when {
            hasFingerprint && (hasFace || hasIris) -> BiometricType.MULTIPLE
            hasFingerprint -> BiometricType.FINGERPRINT
            hasFace        -> BiometricType.FACE
            hasIris        -> BiometricType.IRIS
            else           -> BiometricType.FINGERPRINT // enrolled but type unknown → safest default
        }
    }
}
