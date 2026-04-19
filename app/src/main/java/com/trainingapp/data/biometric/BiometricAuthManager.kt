package com.trainingapp.data.biometric

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.StateFlow

/**
 * Central contract for biometric authentication.
 *
 * Implementations:
 *  - [BiometricAuthManagerImpl] — real device sensor via AndroidX Biometric
 *  - [MockBiometricAuthManager] — deterministic stub used in unit tests and on
 *    emulators without a configured biometric profile
 *
 * All state changes are emitted through [authState] so the UI stays reactive
 * without polling or scattered local flags.
 */
interface BiometricAuthManager {

    /** Current authentication state. Starts as [AuthState.Idle]. */
    val authState: StateFlow<AuthState>

    /**
     * Checks whether the device has an enrolled biometric credential and
     * the hardware is ready.  Does **not** mutate [authState].
     */
    fun checkAvailability(): BiometricType

    /**
     * Shows the system biometric prompt.  [reason] is the subtitle the user
     * sees (e.g. "Підтвердіть видалення тренування").
     *
     * State transitions emitted through [authState]:
     *  Idle → Authenticating → Success | Failed | Cancelled | Unavailable
     */
    fun authenticate(activity: FragmentActivity, reason: String)

    /** Returns `true` if the user has enabled biometric login in app settings. */
    fun isEnabledByUser(): Boolean

    /** Resets [authState] back to [AuthState.Idle] after consuming a terminal state. */
    fun resetState()
}
