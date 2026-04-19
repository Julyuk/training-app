package com.trainingapp

import android.app.Application
import com.trainingapp.data.SampleData
import com.trainingapp.data.biometric.AppLockManager
import com.trainingapp.data.biometric.BiometricAuthManager
import com.trainingapp.data.biometric.BiometricAuthManagerImpl
import com.trainingapp.data.local.AppDatabase
import com.trainingapp.data.local.ConnectivityNetworkMonitor
import com.trainingapp.data.local.NetworkMonitor
import com.trainingapp.data.local.ProfilePreferences
import com.trainingapp.data.local.SecurityPreferences
import com.trainingapp.data.remote.MockWorkoutApiService
import com.trainingapp.data.repository.WorkoutRepository
import com.trainingapp.data.repository.WorkoutRepositoryImpl
import com.trainingapp.data.websocket.MockSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * Custom Application class that wires together the database, API service,
 * repository and network monitor at startup.
 *
 * ── Offline-first behaviour ──────────────────────────────────────────────────
 * 1. On first launch the database is seeded with sample data.
 * 2. On every launch a background sync pulls the latest server state into
 *    Room (existing PENDING records are never overwritten by server data).
 * 3. [networkMonitor] watches for connectivity changes. When the device goes
 *    from offline → online, the app:
 *      a) Uploads all PENDING workouts to the server.
 *      b) Pulls the latest server state and merges it locally.
 * 4. Without internet the app is fully functional: the user can browse, add,
 *    edit and delete workouts.  Changes are queued as PENDING and synced later.
 * ────────────────────────────────────────────────────────────────────────────
 */
class TrainingApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val profilePreferences: ProfilePreferences by lazy {
        ProfilePreferences(this)
    }

    val securityPreferences: SecurityPreferences by lazy {
        SecurityPreferences(this)
    }

    val biometricAuthManager: BiometricAuthManager by lazy {
        BiometricAuthManagerImpl(this, securityPreferences)
    }

    val appLockManager: AppLockManager by lazy {
        AppLockManager(securityPreferences)
    }

    val workoutRepository: WorkoutRepository by lazy {
        val db = AppDatabase.getInstance(this)
        WorkoutRepositoryImpl(
            workoutDao = db.workoutDao(),
            apiService = MockWorkoutApiService()
        )
    }

    /**
     * Detects whether the device has an internet connection.
     * Exposes [NetworkMonitor.isOnline] — a Flow<Boolean> that emits the current
     * state on subscription and on every transition.
     */
    val networkMonitor: NetworkMonitor by lazy {
        ConnectivityNetworkMonitor(this)
    }

    /**
     * Shared WebSocket manager — single connection for the entire app.
     * Both the Live Feed and Challenges screens observe this instance so only
     * one physical (mock) socket is open at a time.
     */
    val socketManager: MockSocketManager by lazy {
        MockSocketManager()
    }

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            seedDatabaseIfEmpty()
            // Upload any PENDING changes first, then pull server state.
            // This order matters: upload before sync so the server has the
            // latest local data before we merge its response back.
            workoutRepository.uploadPendingWorkouts()
            workoutRepository.syncWithApi()
        }

        // Establish the WebSocket connection as soon as the app starts.
        socketManager.connect("ws://training-app.local/ws")

        // ── Auto-sync on connectivity restore ─────────────────────────────────
        // ConnectivityNetworkMonitor emits the current state immediately (initial
        // emission).  We drop(1) because the startup sync above already handles
        // the first launch.  Every subsequent `true` emission means the device
        // just came back online, so we upload queued changes then refresh.
        applicationScope.launch {
            networkMonitor.isOnline
                .drop(1)        // skip initial emission (startup sync handled above)
                .filter { it }  // only react when transitioning offline → online
                .collect {
                    workoutRepository.uploadPendingWorkouts()
                    workoutRepository.syncWithApi()
                }
        }
    }

    /**
     * Inserts sample workouts only on the very first launch (empty database).
     * After that, user data is preserved untouched across restarts.
     */
    private suspend fun seedDatabaseIfEmpty() {
        val db = AppDatabase.getInstance(this)
        if (db.workoutDao().countWorkouts() == 0) {
            SampleData.workouts.forEach { workout ->
                workoutRepository.saveWorkout(workout)
            }
        }
    }
}
