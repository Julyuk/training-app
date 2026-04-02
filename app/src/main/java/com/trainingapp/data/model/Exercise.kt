package com.trainingapp.data.model

/**
 * Represents a single exercise performed during a workout session.
 * Tracks volume (sets × reps), load (weight), and which muscle group is targeted.
 * When [isBodyweight] is true, [weightKg] is treated as 0 and not displayed.
 */
data class Exercise(
    val id: Int,
    val name: String,
    val muscleGroup: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Float,
    val isBodyweight: Boolean,
    val notes: String
)
