package com.trainingapp.data.model

/**
 * Categorises the type of physical activity for a workout session.
 */
enum class WorkoutCategory(val label: String, val emoji: String) {
    STRENGTH("Силова", "💪"),
    CARDIO("Кардіо", "🏃"),
    FLEXIBILITY("Гнучкість", "🧘"),
    HIIT("HIIT", "⚡")
}
