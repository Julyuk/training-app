package com.trainingapp.data.biometric

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Test double for [BiometricAuthManager].
 *
 * Simulates hardware responses deterministically so the UI can be tested on
 * emulators without a configured biometric profile, and unit tests can verify
 * every authentication scenario without touching real hardware.
 *
 * @param sensorType     What [checkAvailability] reports; defaults to FINGERPRINT.
 * @param authResult     The terminal [AuthState] emitted by [authenticate]; defaults to Success.
 * @param biometricEnabled Whether [isEnabledByUser] returns `true`.
 */
class MockBiometricAuthManager(
    private val sensorType: BiometricType = BiometricType.FINGERPRINT,
    private val authResult: AuthState = AuthState.Success,
    private var biometricEnabled: Boolean = false
) : BiometricAuthManager {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override fun checkAvailability(): BiometricType = sensorType

    /**
     * Immediately transitions Idle → Authenticating → [authResult].
     * The [activity] parameter is accepted but never used, so null-safe for tests.
     */
    override fun authenticate(activity: FragmentActivity, reason: String) {
        _authState.value = AuthState.Authenticating
        _authState.value = authResult
    }

    override fun isEnabledByUser(): Boolean = biometricEnabled

    override fun resetState() {
        _authState.value = AuthState.Idle
    }

    /** Allows tests to flip the enabled flag after construction. */
    fun setEnabled(enabled: Boolean) {
        biometricEnabled = enabled
    }
}
