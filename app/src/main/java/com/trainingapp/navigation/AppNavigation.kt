package com.trainingapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.trainingapp.data.SampleData
import com.trainingapp.ui.screens.*

/**
 * Root composable that owns the single [NavController] and draws the
 * bottom navigation bar. The bar is visible only on top-level destinations;
 * detail screens slide in on top without it.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Destinations that show the bottom bar
    val topLevelRoutes = listOf(Screen.WorkoutList.route, Screen.Profile.route)
    val showBottomBar = currentRoute in topLevelRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.WorkoutList.route,
                        onClick = {
                            navController.navigate(Screen.WorkoutList.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = "Тренування") },
                        label = { Text("Тренування") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Profile.route,
                        onClick = {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Профіль") },
                        label = { Text("Профіль") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.WorkoutList.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.WorkoutList.route) {
                WorkoutListScreen(
                    workouts = SampleData.workouts,
                    onWorkoutClick = { workout ->
                        navController.navigate(Screen.WorkoutDetail.createRoute(workout.id))
                    }
                )
            }

            composable(
                route = Screen.WorkoutDetail.route,
                arguments = listOf(navArgument("workoutId") { type = NavType.IntType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: return@composable
                val workout = SampleData.findWorkoutById(workoutId) ?: return@composable
                WorkoutDetailScreen(
                    workout = workout,
                    onBack = { navController.popBackStack() },
                    onExerciseClick = { exercise ->
                        navController.navigate(Screen.ExerciseDetail.createRoute(exercise.id))
                    }
                )
            }

            composable(
                route = Screen.ExerciseDetail.route,
                arguments = listOf(navArgument("exerciseId") { type = NavType.IntType })
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: return@composable
                val exercise = SampleData.findExerciseById(exerciseId) ?: return@composable
                ExerciseDetailScreen(
                    exercise = exercise,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(profile = SampleData.userProfile)
            }
        }
    }
}
