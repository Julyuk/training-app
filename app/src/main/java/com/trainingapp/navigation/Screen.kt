package com.trainingapp.navigation

/**
 * Sealed class that defines every navigable destination in the app.
 * Each object holds a [route] string consumed by NavHost / NavController.
 * Parameterised destinations expose a helper to build the actual route string.
 */
sealed class Screen(val route: String) {

    object WorkoutList : Screen("workout_list")

    object WorkoutDetail : Screen("workout_detail/{workoutId}") {
        fun createRoute(workoutId: Int) = "workout_detail/$workoutId"
    }

    object ExerciseDetail : Screen("exercise_detail/{exerciseId}") {
        fun createRoute(exerciseId: Int) = "exercise_detail/$exerciseId"
    }

    object Profile : Screen("profile")
}
