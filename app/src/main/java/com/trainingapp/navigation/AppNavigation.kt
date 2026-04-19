package com.trainingapp.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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
import com.trainingapp.data.biometric.AuthState
import com.trainingapp.ui.screens.AddWorkoutScreen
import com.trainingapp.ui.screens.ChallengesScreen
import com.trainingapp.ui.screens.EditProfileScreen
import com.trainingapp.ui.screens.EditWorkoutScreen
import com.trainingapp.ui.screens.ExerciseDetailScreen
import com.trainingapp.ui.screens.LiveFeedScreen
import com.trainingapp.ui.screens.LockScreen
import com.trainingapp.ui.screens.ProfileScreen
import com.trainingapp.ui.screens.SecuritySettingsScreen
import com.trainingapp.ui.screens.WorkoutDetailScreen
import com.trainingapp.ui.screens.WorkoutListScreen
import com.trainingapp.ui.viewmodel.ChallengesViewModel
import com.trainingapp.ui.viewmodel.LiveFeedViewModel
import com.trainingapp.ui.viewmodel.LockViewModel
import com.trainingapp.ui.viewmodel.ProfileViewModel
import com.trainingapp.ui.viewmodel.SecuritySettingsViewModel
import com.trainingapp.ui.viewmodel.WorkoutDetailViewModel
import com.trainingapp.ui.viewmodel.WorkoutListViewModel

/**
 * Root composable that owns the single [NavController] and draws the
 * bottom navigation bar.
 *
 * ── Biometric features added ──────────────────────────────────────────────────
 * • Auto-lock: a [LifecycleEventObserver] records when the app goes to the
 *   background.  On resume, [AppLockManager.shouldLock] decides whether to
 *   show [LockScreen] as a full-screen overlay (not a navigation destination,
 *   so the back button cannot dismiss it).
 * • Delete confirmation: if biometric is enabled, tapping the delete icon on a
 *   workout card launches [BiometricPrompt] instead of deleting immediately.
 *   The actual delete only happens on [AuthState.Success].
 * • Security settings: navigable from the Profile screen.
 */
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val app = context.applicationContext as TrainingApp

    val repository      = remember { app.workoutRepository }
    val socketManager   = remember { app.socketManager }
    val biometricMgr    = remember { app.biometricAuthManager }
    val appLockManager  = remember { app.appLockManager }

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

    // ── ViewModels ────────────────────────────────────────────────────────────
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
    val lockViewModel: LockViewModel = viewModel(
        factory = LockViewModel.factory(biometricMgr)
    )

    // ── Auto-lock ─────────────────────────────────────────────────────────────
    var isLocked by remember { mutableStateOf(false) }

    DisposableEffect(activity) {
        val lifecycleOwner = activity as LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP  -> appLockManager.onAppBackgrounded()
                Lifecycle.Event.ON_START -> {
                    if (appLockManager.shouldLock()) {
                        isLocked = true
                        // Timer is cleared only after the user authenticates (onUnlocked),
                        // not here — otherwise a second ON_START before unlock would skip it.
                    }
                }
                else -> {}
            }
        }
        (activity as LifecycleOwner).lifecycle.addObserver(observer)
        onDispose { (activity as LifecycleOwner).lifecycle.removeObserver(observer) }
    }

    val lockAuthState by lockViewModel.authState.collectAsState()
    LaunchedEffect(lockAuthState) {
        if (lockAuthState is AuthState.Success && isLocked) {
            isLocked = false
            lockViewModel.resetState()
            appLockManager.onUnlocked()   // marks session unlocked + clears timer
        }
    }

    // ── Biometric delete confirmation ─────────────────────────────────────────
    // Holds the id of the workout pending deletion until biometric auth resolves.
    var pendingDeleteId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(lockAuthState) {
        val deleteId = pendingDeleteId
        // Only process delete auth when the lock screen is NOT showing; if isLocked is
        // true the auth event belongs to the lock flow, not the delete confirmation.
        if (deleteId != null && !isLocked) {
            when (lockAuthState) {
                is AuthState.Success -> {
                    workoutListViewModel.deleteWorkout(deleteId)
                    pendingDeleteId = null
                    lockViewModel.resetState()
                }
                is AuthState.Failed,
                is AuthState.Cancelled,
                is AuthState.Unavailable -> {
                    pendingDeleteId = null
                    lockViewModel.resetState()
                }
                else -> {}
            }
        }
    }

    // Consume back presses while locked so the user cannot dismiss the lock screen.
    BackHandler(enabled = isLocked) { /* intentionally empty */ }

    // ── Scaffold + NavHost ────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
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
                // ── Workout List ──────────────────────────────────────────────
                composable(Screen.WorkoutList.route) {
                    val workouts by workoutListViewModel.workouts.collectAsState()
                    val joinedCategories by challengesViewModel.joinedCategories.collectAsState()
                    WorkoutListScreen(
                        workouts = workouts,
                        onWorkoutClick = { workout ->
                            navController.navigate(Screen.WorkoutDetail.createRoute(workout.id))
                        },
                        onToggleCompleted = { workoutListViewModel.toggleCompleted(it) },
                        onDeleteClick = { id ->
                            if (app.securityPreferences.isBiometricEnabled() &&
                                biometricMgr.checkAvailability() != com.trainingapp.data.biometric.BiometricType.NONE
                            ) {
                                // Biometric confirmation for destructive action
                                pendingDeleteId = id
                                biometricMgr.authenticate(activity, "Підтвердіть видалення тренування")
                            } else {
                                workoutListViewModel.deleteWorkout(id)
                            }
                        },
                        onAddClick = { navController.navigate(Screen.AddWorkout.route) },
                        joinedChallengeCategories = joinedCategories
                    )
                }

                // ── Add Workout ───────────────────────────────────────────────
                composable(Screen.AddWorkout.route) {
                    AddWorkoutScreen(
                        onBack = { navController.popBackStack() },
                        onSave = { title, description, duration, calories, isCompleted, category, date ->
                            workoutListViewModel.addWorkout(
                                title, description, duration, calories, isCompleted, category, date
                            )
                        }
                    )
                }

                // ── Workout Detail ────────────────────────────────────────────
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

                // ── Exercise Detail ───────────────────────────────────────────
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

                // ── Profile ───────────────────────────────────────────────────
                composable(Screen.Profile.route) {
                    val workouts by workoutListViewModel.workouts.collectAsState()
                    val identity by profileViewModel.identity.collectAsState()
                    val physical by profileViewModel.physical.collectAsState()
                    val preferences by profileViewModel.preferences.collectAsState()
                    ProfileScreen(
                        identity = identity,
                        physical = physical,
                        preferences = preferences,
                        allWorkouts = workouts,
                        onEditClick = { navController.navigate(Screen.EditProfile.route) },
                        onSecurityClick = { navController.navigate(Screen.SecuritySettings.route) }
                    )
                }

                // ── Edit Profile ──────────────────────────────────────────────
                composable(Screen.EditProfile.route) {
                    val identity by profileViewModel.identity.collectAsState()
                    val physical by profileViewModel.physical.collectAsState()
                    val preferences by profileViewModel.preferences.collectAsState()
                    EditProfileScreen(
                        identity = identity,
                        physical = physical,
                        preferences = preferences,
                        onBack = { navController.popBackStack() },
                        onSave = { id, ph, pr ->
                            profileViewModel.saveIdentity(id)
                            profileViewModel.savePhysical(ph)
                            profileViewModel.savePreferences(pr)
                        }
                    )
                }

                // ── Edit Workout ──────────────────────────────────────────────
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

                // ── Live Feed (WebSocket) ─────────────────────────────────────
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

                // ── Challenges ────────────────────────────────────────────────
                composable(Screen.Challenges.route) {
                    val challenges by challengesViewModel.challenges.collectAsState()
                    val lastUpdate by challengesViewModel.lastUpdate.collectAsState()
                    ChallengesScreen(
                        challenges = challenges,
                        lastUpdate = lastUpdate,
                        onToggleJoin = { challengesViewModel.toggleJoin(it) }
                    )
                }

                // ── Security Settings ─────────────────────────────────────────
                composable(Screen.SecuritySettings.route) {
                    val securityViewModel: SecuritySettingsViewModel = viewModel(
                        factory = SecuritySettingsViewModel.factory(
                            app.securityPreferences,
                            biometricMgr
                        )
                    )
                    SecuritySettingsScreen(
                        viewModel = securityViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // ── Lock screen overlay ───────────────────────────────────────────────
        // Rendered on top of everything; cannot be dismissed with the back button.
        if (isLocked) {
            LockScreen(
                biometricType = biometricMgr.checkAvailability(),
                authState = lockAuthState,
                onAuthenticate = { lockViewModel.authenticate(activity) },
                onUnlocked = {
                    isLocked = false
                    lockViewModel.resetState()
                }
            )
        }
    }
}
