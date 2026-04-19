package com.trainingapp.data.biometric

/**
 * All possible states of a single biometric authentication attempt.
 * Stored centrally in [BiometricAuthManager.authState] so every part of the
 * UI reacts to the same source of truth.
 *
 * Terminal states: Success, Failed, Cancelled, Unavailable.
 * Call [BiometricAuthManager.resetState] to return to Idle.
 */
sealed class AuthState {
    /** No authentication in progress. */
    object Idle : AuthState()

    /** System biometric dialog is open; waiting for user input. */
    object Authenticating : AuthState()

    /** User was verified successfully. */
    object Success : AuthState()

    /**
     * Authentication ended with a permanent error (too many attempts,
     * hardware lockout, etc.).  [message] is a localised description
     * suitable for display to the user.
     */
    data class Failed(val message: String) : AuthState()

    /** User explicitly dismissed the prompt (negative button or back). */
    object Cancelled : AuthState()

    /**
     * No biometric sensor present, no enrolled credentials, or the
     * hardware is temporarily unavailable.
     */
    object Unavailable : AuthState()
}
