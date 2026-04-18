package com.trainingapp.data.model

data class UserPreferences(
    val fitnessGoal: String,
    val weeklyWorkoutTarget: Int,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE
)
