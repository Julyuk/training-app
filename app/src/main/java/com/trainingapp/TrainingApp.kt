package com.trainingapp

import android.app.Application
import com.trainingapp.data.SampleData
import com.trainingapp.data.local.AppDatabase
import com.trainingapp.data.local.ProfilePreferences
import com.trainingapp.data.remote.MockWorkoutApiService
import com.trainingapp.data.repository.WorkoutRepository
import com.trainingapp.data.repository.WorkoutRepositoryImpl
import com.trainingapp.data.websocket.MockSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Custom Application class that wires together the database, API service and
 * repository at startup.
 *
 * If the database is empty (first launch) the sample data from [SampleData]
 * is seeded so that the app has content immediately.
 */
class TrainingApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val profilePreferences: ProfilePreferences by lazy {
        ProfilePreferences(this)
    }

    val workoutRepository: WorkoutRepository by lazy {
        val db = AppDatabase.getInstance(this)
        WorkoutRepositoryImpl(
            workoutDao = db.workoutDao(),
            apiService = MockWorkoutApiService()
        )
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
        }
        // Establish the WebSocket connection as soon as the app starts.
        socketManager.connect("ws://training-app.local/ws")
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
