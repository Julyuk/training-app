package com.trainingapp.data.model

/**
 * Represents the user's personal profile and fitness preferences.
 *
 * [sex] and [activityLevel] drive the accurate Mifflin-St Jeor / Harris-Benedict
 * TDEE calculation shown on the profile screen.
 * Defaulting both to the neutral/moderate values means existing users who
 * haven't filled them in yet still get a reasonable estimate.
 */
data class UserProfile(
    val name: String,
    val age: Int,
    val weightKg: Float,
    val heightCm: Int,
    val fitnessGoal: String,
    val weeklyWorkoutTarget: Int,
    val isPremium: Boolean,
    val sex: Sex = Sex.UNSPECIFIED,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE
)
