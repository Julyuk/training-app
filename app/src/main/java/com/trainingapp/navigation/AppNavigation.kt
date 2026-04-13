package com.trainingapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trainingapp.TrainingApp
import com.trainingapp.ui.screens.AddWorkoutScreen
import com.trainingapp.ui.screens.ChallengesScreen
import com.trainingapp.ui.screens.EditProfileScreen
import com.trainingapp.ui.screens.EditWorkoutScreen
import com.trainingapp.ui.screens.ExerciseDetailScreen
import com.trainingapp.ui.screens.LiveFeedScreen
import com.trainingapp.ui.screens.ProfileScreen
import com.trainingapp.ui.screens.WorkoutDetailScreen
import com.trainingapp.ui.screens.WorkoutListScreen
import com.trainingapp.ui.viewmodel.ChallengesViewModel
import com.trainingapp.ui.viewmodel.LiveFeedViewModel
import com.trainingapp.ui.viewmodel.ProfileViewModel
import com.trainingapp.ui.viewmodel.WorkoutDetailViewModel
import com.trainingapp.ui.viewmodel.WorkoutListViewModel

/**
 * Root composable that owns the single [NavController] and draws the
 * bottom navigation bar.
 *
 * Two new top-level destinations are wired in:
 *  - [Screen.LiveFeed]   → real-time WebSocket event feed
 *  - [Screen.Challenges] → community challenges with live WS updates
 *
 * Both share the same [SocketManager] instance from [TrainingApp], so they
 * observe the same connection without opening duplicate sockets.
 */
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val app = context.applicationContext as TrainingApp

    val repository = remember { app.workoutRepository }
    val socketManager = remember { app.socketManager }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = listOf(
        Screen.WorkoutList.route,
        Screen.LiveFeed.route,
        Screen.Challenges.route,
        Screen.Profile.route
    )
    val showBottomBar = currentRoute in topLevelRoutes

    val workoutListViewModel: WorkoutListViewModel = viewModel(
        factory = WorkoutListViewModel.factory(repository)
    )
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.factory(app.profilePreferences)
    )
    val liveFeedViewModel: LiveFeedViewModel = viewModel(
        factory = LiveFeedViewModel.factory(socketManager)
    )
    val challengesViewModel: ChallengesViewModel = viewModel(
        factory = ChallengesViewModel.factory(socketManager, repository)
    )

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
                        selected = currentRoute == Screen.LiveFeed.route,
                        onClick = {
                            navController.navigate(Screen.LiveFeed.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Notifications, contentDescription = "Живий канал") },
                        label = { Text("Канал") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Challenges.route,
                        onClick = {
                            navController.navigate(Screen.Challenges.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = "Виклики") },
                        label = { Text("Виклики") }
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
            // ── Workout List ──────────────────────────────────────────────────
            composable(Screen.WorkoutList.route) {
                val workouts by workoutListViewModel.workouts.collectAsState()
                val joinedCategories by challengesViewModel.joinedCategories.collectAsState()
                WorkoutListScreen(
                    workouts = workouts,
                    onWorkoutClick = { workout ->
                        navController.navigate(Screen.WorkoutDetail.createRoute(workout.id))
                    },
                    onToggleCompleted = { workoutListViewModel.toggleCompleted(it) },
                    onDeleteClick = { workoutListViewModel.deleteWorkout(it) },
                    onAddClick = { navController.navigate(Screen.AddWorkout.route) },
                    joinedChallengeCategories = joinedCategories
                )
            }

            // ── Add Workout ───────────────────────────────────────────────────
            composable(Screen.AddWorkout.route) {
                AddWorkoutScreen(
                    onBack = { navController.popBackStack() },
                    onSave = { title, description, duration, calories, isCompleted, category ->
                        workoutListViewModel.addWorkout(
                            title, description, duration, calories, isCompleted, category
                        )
                    }
                )
            }

            // ── Workout Detail ────────────────────────────────────────────────
            composable(
                route = Screen.WorkoutDetail.route,
                arguments = listOf(navArgument("workoutId") { type = NavType.IntType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: return@composable
                val detailViewModel: WorkoutDetailViewModel = viewModel(
                    key = "workout_detail_$workoutId",
                    factory = WorkoutDetailViewModel.factory(repository, workoutId)
                )
                val workout by detailViewModel.workout.collectAsState()
                workout?.let { w ->
                    WorkoutDetailScreen(
                        workout = w,
                        onBack = { navController.popBackStack() },
                        onEditClick = {
                            navController.navigate(Screen.EditWorkout.createRoute(w.id))
                        },
                        onExerciseClick = { exercise ->
                            navController.navigate(Screen.ExerciseDetail.createRoute(exercise.id))
                        }
                    )
                }
            }

            // ── Exercise Detail ───────────────────────────────────────────────
            composable(
                route = Screen.ExerciseDetail.route,
                arguments = listOf(navArgument("exerciseId") { type = NavType.IntType })
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: return@composable
                val exerciseViewModel: WorkoutDetailViewModel = viewModel(
                    key = "exercise_detail_$exerciseId",
                    factory = WorkoutDetailViewModel.factory(repository, -1, exerciseId)
                )
                val exercise by exerciseViewModel.exercise.collectAsState()
                exercise?.let { e ->
                    ExerciseDetailScreen(
                        exercise = e,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // ── Profile ───────────────────────────────────────────────────────
            composable(Screen.Profile.route) {
                val workouts by workoutListViewModel.workouts.collectAsState()
                val profile by profileViewModel.profile.collectAsState()
                ProfileScreen(
                    profile = profile,
                    allWorkouts = workouts,
                    onEditClick = { navController.navigate(Screen.EditProfile.route) }
                )
            }

            // ── Edit Profile ──────────────────────────────────────────────────
            composable(Screen.EditProfile.route) {
                val profile by profileViewModel.profile.collectAsState()
                EditProfileScreen(
                    profile = profile,
                    onBack = { navController.popBackStack() },
                    onSave = { profileViewModel.save(it) }
                )
            }

            // ── Edit Workout ──────────────────────────────────────────────────
            composable(
                route = Screen.EditWorkout.route,
                arguments = listOf(navArgument("workoutId") { type = NavType.IntType })
            ) { backStackEntry ->
                val workoutId = backStackEntry.arguments?.getInt("workoutId") ?: return@composable
                val editViewModel: WorkoutDetailViewModel = viewModel(
                    key = "edit_workout_$workoutId",
                    factory = WorkoutDetailViewModel.factory(repository, workoutId)
                )
                val workout by editViewModel.workout.collectAsState()
                workout?.let { w ->
                    EditWorkoutScreen(
                        workout = w,
                        onBack = { navController.popBackStack() },
                        onSave = { updated -> workoutListViewModel.updateWorkout(updated) }
                    )
                }
            }

            // ── Live Feed (WebSocket) ─────────────────────────────────────────
            composable(Screen.LiveFeed.route) {
                val messages by liveFeedViewModel.messages.collectAsState()
                val connectionState by liveFeedViewModel.connectionState.collectAsState()
                LiveFeedScreen(
                    messages = messages,
                    connectionState = connectionState,
                    onClearMessages = { liveFeedViewModel.clearMessages() },
                    onReconnect = { liveFeedViewModel.reconnect() }
                )
            }

            // ── Challenges ────────────────────────────────────────────────────
            composable(Screen.Challenges.route) {
                val challenges by challengesViewModel.challenges.collectAsState()
                val lastUpdate by challengesViewModel.lastUpdate.collectAsState()
                ChallengesScreen(
                    challenges = challenges,
                    lastUpdate = lastUpdate,
                    onToggleJoin = { challengesViewModel.toggleJoin(it) }
                )
            }
        }
    }
}
