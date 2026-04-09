package com.trainingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.trainingapp.data.local.ProfilePreferences
import com.trainingapp.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the profile and edit-profile screens.
 * Reads and writes [UserProfile] via [ProfilePreferences] (SharedPreferences).
 */
class ProfileViewModel(private val prefs: ProfilePreferences) : ViewModel() {

    private val _profile = MutableStateFlow(prefs.load())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    fun save(profile: UserProfile) {
        prefs.save(profile)
        _profile.value = profile
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
