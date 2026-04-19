package com.trainingapp.ui.viewmodel

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.trainingapp.data.biometric.AuthState
import com.trainingapp.data.biometric.BiometricAuthManager
import kotlinx.coroutines.flow.StateFlow

/**
 * Drives the lock screen.
 *
 * Delegates all state to [BiometricAuthManager] so the lock screen and any
 * other consumer observe the same [StateFlow] — one source of truth.
 */
class LockViewModel(
    private val biometricAuthManager: BiometricAuthManager
) : ViewModel() {

    val authState: StateFlow<AuthState> = biometricAuthManager.authState

    fun authenticate(activity: FragmentActivity) {
        biometricAuthManager.authenticate(activity, "Відкрийте застосунок")
    }

    fun resetState() = biometricAuthManager.resetState()

    companion object {
        fun factory(biometricAuthManager: BiometricAuthManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LockViewModel(biometricAuthManager) as T
            }
    }
}
