package com.trainingapp.data.local

import android.content.Context

/**
 * [SharedPreferences]-backed implementation of [SecurityStore].
 *
 * Persists two settings:
 *  - whether the user has opted into biometric authentication
 *  - the inactivity timeout (seconds) after which the app auto-locks
 */
class SecurityPreferences(context: Context) : SecurityStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun setBiometricEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()

    override fun isBiometricEnabled(): Boolean =
        prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    override fun setAutoLockTimeoutSeconds(seconds: Int) =
        prefs.edit().putInt(KEY_AUTO_LOCK_TIMEOUT, seconds).apply()

    override fun getAutoLockTimeoutSeconds(): Int =
        prefs.getInt(KEY_AUTO_LOCK_TIMEOUT, DEFAULT_TIMEOUT_SECONDS)

    companion object {
        private const val PREFS_NAME = "security_prefs"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        const val DEFAULT_TIMEOUT_SECONDS = 30
    }
}
