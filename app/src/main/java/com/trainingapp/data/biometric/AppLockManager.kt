package com.trainingapp.data.biometric

import com.trainingapp.data.local.SecurityStore

/**
 * Decides whether the app should show the lock screen.
 *
 * Two cases require a lock:
 *  1. **Cold start** — biometric is enabled but the user has not yet authenticated
 *     in this process lifetime ([hasUnlockedThisSession] == false).
 *  2. **Background resume** — the app was backgrounded for longer than the
 *     configured auto-lock timeout.
 *
 * Lifecycle protocol:
 *  - Call [onAppBackgrounded] from ON_STOP.
 *  - Call [shouldLock]        from ON_START; show the lock screen if true.
 *  - Call [onUnlocked]        after the user successfully authenticates.
 */
class AppLockManager(private val securityStore: SecurityStore) {

    private var backgroundedAt: Long = 0L

    /** Becomes true only after the first successful biometric unlock this process. */
    var hasUnlockedThisSession: Boolean = false
        private set

    fun onAppBackgrounded() {
        if (securityStore.isBiometricEnabled()) {
            backgroundedAt = System.currentTimeMillis()
        }
    }

    fun shouldLock(): Boolean {
        if (!securityStore.isBiometricEnabled()) return false
        // Cold start: never authenticated in this process lifetime.
        if (!hasUnlockedThisSession) return true
        // Background resume: check elapsed time against configured timeout.
        if (backgroundedAt == 0L) return false
        val elapsed = System.currentTimeMillis() - backgroundedAt
        return elapsed >= securityStore.getAutoLockTimeoutSeconds() * 1_000L
    }

    /**
     * Call after a successful biometric authentication.
     * Marks this session as unlocked and clears the background timer.
     */
    fun onUnlocked() {
        hasUnlockedThisSession = true
        backgroundedAt = 0L
    }
}
