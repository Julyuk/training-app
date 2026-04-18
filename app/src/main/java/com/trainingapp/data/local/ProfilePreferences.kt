package com.trainingapp.data.local

import android.content.Context
import com.trainingapp.data.SampleData
import com.trainingapp.data.model.ActivityLevel
import com.trainingapp.data.model.Sex
import com.trainingapp.data.model.UserIdentity
import com.trainingapp.data.model.UserPhysical
import com.trainingapp.data.model.UserPreferences

class ProfilePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Identity ──────────────────────────────────────────────────────────────

    fun saveIdentity(identity: UserIdentity) {
        prefs.edit()
            .putString(KEY_NAME, identity.name)
            .putBoolean(KEY_PREMIUM, identity.isPremium)
            .apply()
    }

    fun loadIdentity(): UserIdentity {
        val defaults = SampleData.userIdentity
        return UserIdentity(
            name = prefs.getString(KEY_NAME, defaults.name) ?: defaults.name,
            isPremium = prefs.getBoolean(KEY_PREMIUM, defaults.isPremium)
        )
    }

    // ── Physical ──────────────────────────────────────────────────────────────

    fun savePhysical(physical: UserPhysical) {
        prefs.edit()
            .putInt(KEY_AGE, physical.age)
            .putFloat(KEY_WEIGHT, physical.weightKg)
            .putInt(KEY_HEIGHT, physical.heightCm)
            .putString(KEY_SEX, physical.sex.name)
            .apply()
    }

    fun loadPhysical(): UserPhysical {
        val defaults = SampleData.userPhysical
        return UserPhysical(
            age = prefs.getInt(KEY_AGE, defaults.age),
            weightKg = prefs.getFloat(KEY_WEIGHT, defaults.weightKg),
            heightCm = prefs.getInt(KEY_HEIGHT, defaults.heightCm),
            sex = prefs.getString(KEY_SEX, defaults.sex.name)
                ?.let { runCatching { Sex.valueOf(it) }.getOrNull() }
                ?: defaults.sex
        )
    }

    // ── Preferences ───────────────────────────────────────────────────────────

    fun savePreferences(preferences: UserPreferences) {
        prefs.edit()
            .putString(KEY_GOAL, preferences.fitnessGoal)
            .putInt(KEY_WEEKLY_TARGET, preferences.weeklyWorkoutTarget)
            .putString(KEY_ACTIVITY_LEVEL, preferences.activityLevel.name)
            .apply()
    }

    fun loadPreferences(): UserPreferences {
        val defaults = SampleData.userPreferences
        return UserPreferences(
            fitnessGoal = prefs.getString(KEY_GOAL, defaults.fitnessGoal) ?: defaults.fitnessGoal,
            weeklyWorkoutTarget = prefs.getInt(KEY_WEEKLY_TARGET, defaults.weeklyWorkoutTarget),
            activityLevel = prefs.getString(KEY_ACTIVITY_LEVEL, defaults.activityLevel.name)
                ?.let { runCatching { ActivityLevel.valueOf(it) }.getOrNull() }
                ?: defaults.activityLevel
        )
    }

    private companion object {
        const val PREFS_NAME         = "profile_prefs"
        const val KEY_NAME           = "name"
        const val KEY_PREMIUM        = "is_premium"
        const val KEY_AGE            = "age"
        const val KEY_WEIGHT         = "weight_kg"
        const val KEY_HEIGHT         = "height_cm"
        const val KEY_SEX            = "sex"
        const val KEY_GOAL           = "fitness_goal"
        const val KEY_WEEKLY_TARGET  = "weekly_target"
        const val KEY_ACTIVITY_LEVEL = "activity_level"
    }
}
