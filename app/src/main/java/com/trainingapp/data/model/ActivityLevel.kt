package com.trainingapp.data.model

/**
 * Harris-Benedict activity multipliers applied to BMR to get TDEE.
 * The user selects the level that best describes their daily life
 * *outside* of logged workouts.
 */
enum class ActivityLevel(val label: String, val multiplier: Float) {
    SEDENTARY("Малорухливий (без вправ)", 1.2f),
    LIGHT("Легкий (1–2 дні/тиж)", 1.375f),
    MODERATE("Помірний (3–5 днів/тиж)", 1.55f),
    ACTIVE("Активний (6–7 днів/тиж)", 1.725f),
    VERY_ACTIVE("Дуже активний (2× на день)", 1.9f)
}
