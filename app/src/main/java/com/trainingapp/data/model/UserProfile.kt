package com.trainingapp.data.model

/**
 * Represents the user's personal profile and fitness preferences.
 * Displayed on the Profile screen; [weeklyWorkoutTarget] is used
 * to calculate weekly progress relative to completed workouts.
 */
data class UserProfile(
    val name: String,
    val age: Int,
    val weightKg: Float,
    val heightCm: Int,
    val fitnessGoal: String,
    val weeklyWorkoutTarget: Int,
    val isPremium: Boolean
)
