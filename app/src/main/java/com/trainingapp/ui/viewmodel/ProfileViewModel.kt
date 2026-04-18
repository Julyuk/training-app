package com.trainingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.trainingapp.data.local.ProfilePreferences
import com.trainingapp.data.model.UserIdentity
import com.trainingapp.data.model.UserPhysical
import com.trainingapp.data.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel(private val prefs: ProfilePreferences) : ViewModel() {

    private val _identity = MutableStateFlow(prefs.loadIdentity())
    val identity: StateFlow<UserIdentity> = _identity.asStateFlow()

    private val _physical = MutableStateFlow(prefs.loadPhysical())
    val physical: StateFlow<UserPhysical> = _physical.asStateFlow()

    private val _preferences = MutableStateFlow(prefs.loadPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    fun saveIdentity(identity: UserIdentity) {
        prefs.saveIdentity(identity)
        _identity.value = identity
    }

    fun savePhysical(physical: UserPhysical) {
        prefs.savePhysical(physical)
        _physical.value = physical
    }

    fun savePreferences(preferences: UserPreferences) {
        prefs.savePreferences(preferences)
        _preferences.value = preferences
    }

    companion object {
        fun factory(prefs: ProfilePreferences): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProfileViewModel(prefs) as T
            }
    }
}
