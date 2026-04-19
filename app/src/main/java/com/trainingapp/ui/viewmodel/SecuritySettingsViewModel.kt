package com.trainingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.trainingapp.data.biometric.BiometricAuthManager
import com.trainingapp.data.biometric.BiometricType
import com.trainingapp.data.local.SecurityStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Drives the security settings screen.
 *
 * Reads the current saved values from [SecurityStore] on construction and
 * exposes them as [StateFlow]s so the UI is always in sync.  Every mutating
 * call writes through to [SecurityStore] immediately.
 */
class SecuritySettingsViewModel(
    private val securityStore: SecurityStore,
    biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    /** What sensor (if any) the device physically has. */
    val biometricType: BiometricType = biometricAuthManager.checkAvailability()

    private val _isBiometricEnabled =
        MutableStateFlow(securityStore.isBiometricEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _autoLockTimeout =
        MutableStateFlow(securityStore.getAutoLockTimeoutSeconds())
    val autoLockTimeout: StateFlow<Int> = _autoLockTimeout.asStateFlow()

    fun setBiometricEnabled(enabled: Boolean) {
        securityStore.setBiometricEnabled(enabled)
        _isBiometricEnabled.value = enabled
    }

    fun setAutoLockTimeout(seconds: Int) {
        securityStore.setAutoLockTimeoutSeconds(seconds)
        _autoLockTimeout.value = seconds
    }

    companion object {
        fun factory(
            securityStore: SecurityStore,
            biometricAuthManager: BiometricAuthManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SecuritySettingsViewModel(securityStore, biometricAuthManager) as T
        }
    }
}
