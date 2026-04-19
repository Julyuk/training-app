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

    object AddWorkout : Screen("add_workout")

    object EditProfile : Screen("edit_profile")

    object EditWorkout : Screen("edit_workout/{workoutId}") {
        fun createRoute(workoutId: Int) = "edit_workout/$workoutId"
    }

    object LiveFeed : Screen("live_feed")

    object Challenges : Screen("challenges")

    object SecuritySettings : Screen("security_settings")
}
