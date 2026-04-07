package com.trainingapp.data.local

import android.content.Context
import com.trainingapp.data.SampleData
import com.trainingapp.data.model.ActivityLevel
import com.trainingapp.data.model.Sex
import com.trainingapp.data.model.UserProfile

/**
 * Persists [UserProfile] fields to SharedPreferences.
 *
 * SharedPreferences is the right choice here: the profile is a small set of
 * independent key-value pairs (no relations, no sorting) that must survive
 * app restarts without the overhead of a database table.
 *
 * Default values fall back to [SampleData.userProfile] so the app is
 * immediately usable on first launch without requiring the user to fill in
 * any fields.
 */
class ProfilePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_NAME, profile.name)
            .putInt(KEY_AGE, profile.age)
            .putFloat(KEY_WEIGHT, profile.weightKg)
            .putInt(KEY_HEIGHT, profile.heightCm)
            .putString(KEY_GOAL, profile.fitnessGoal)
            .putInt(KEY_WEEKLY_TARGET, profile.weeklyWorkoutTarget)
            .putBoolean(KEY_PREMIUM, profile.isPremium)
            .putString(KEY_SEX, profile.sex.name)
            .putString(KEY_ACTIVITY_LEVEL, profile.activityLevel.name)
            .apply()
    }

    fun load(): UserProfile {
        val defaults = SampleData.userProfile
        return UserProfile(
            name = prefs.getString(KEY_NAME, defaults.name) ?: defaults.name,
            age = prefs.getInt(KEY_AGE, defaults.age),
            weightKg = prefs.getFloat(KEY_WEIGHT, defaults.weightKg),
            heightCm = prefs.getInt(KEY_HEIGHT, defaults.heightCm),
            fitnessGoal = prefs.getString(KEY_GOAL, defaults.fitnessGoal) ?: defaults.fitnessGoal,
            weeklyWorkoutTarget = prefs.getInt(KEY_WEEKLY_TARGET, defaults.weeklyWorkoutTarget),
            isPremium = prefs.getBoolean(KEY_PREMIUM, defaults.isPremium),
            sex = prefs.getString(KEY_SEX, defaults.sex.name)
                ?.let { runCatching { Sex.valueOf(it) }.getOrNull() }
                ?: defaults.sex,
            activityLevel = prefs.getString(KEY_ACTIVITY_LEVEL, defaults.activityLevel.name)
                ?.let { runCatching { ActivityLevel.valueOf(it) }.getOrNull() }
                ?: defaults.activityLevel
        )
    }

    private companion object {
        const val PREFS_NAME          = "profile_prefs"
        const val KEY_NAME            = "name"
        const val KEY_AGE             = "age"
        const val KEY_WEIGHT          = "weight_kg"
        const val KEY_HEIGHT          = "height_cm"
        const val KEY_GOAL            = "fitness_goal"
        const val KEY_WEEKLY_TARGET   = "weekly_target"
        const val KEY_PREMIUM         = "is_premium"
        const val KEY_SEX             = "sex"
        const val KEY_ACTIVITY_LEVEL  = "activity_level"
    }
}
