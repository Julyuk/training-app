package com.trainingapp.data.local

/**
 * Persistence contract for security settings.
 *
 * Extracted as an interface so [SecuritySettingsViewModel] and
 * [AppLockManager] can be unit-tested with a mock instead of requiring
 * an Android [Context] / [SharedPreferences].
 */
interface SecurityStore {
    fun setBiometricEnabled(enabled: Boolean)
    fun isBiometricEnabled(): Boolean

    /** Seconds of background time before the app locks itself. */
    fun setAutoLockTimeoutSeconds(seconds: Int)
    fun getAutoLockTimeoutSeconds(): Int
}
