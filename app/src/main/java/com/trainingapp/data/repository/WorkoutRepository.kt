package com.trainingapp.data.repository

import com.trainingapp.data.model.Exercise
import com.trainingapp.data.model.Workout
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for workout data.
 *
 * Consumers (ViewModels) interact only with this interface; they are
 * unaware of whether data comes from Room, the network, or a mock.
 *
 * All read operations return [Flow] so that the UI reacts automatically
 * whenever the local database changes.
 */
interface WorkoutRepository {

    /** Reactive stream of all workouts, ordered by date descending. */
    fun getAllWorkouts(): Flow<List<Workout>>

    /** Reactive stream for a single workout; emits null when not found. */
    fun getWorkoutById(id: Int): Flow<Workout?>

    /** Reactive stream for a single exercise across all workouts. */
    fun getExerciseById(exerciseId: Int): Flow<Exercise?>

    /**
     * Persist [workout] and all its exercises to local storage.
     * New workouts are stored with [com.trainingapp.data.model.SyncStatus.PENDING]
     * until [syncWithApi] succeeds.
     */
    suspend fun saveWorkout(workout: Workout)

    /** Flip the isCompleted flag of a workout in local storage. */
    suspend fun toggleCompleted(id: Int)

    /** Remove a workout (and its exercises) from local storage. */
    suspend fun deleteWorkout(id: Int)

    /**
     * Upload all locally-created or modified workouts ([SyncStatus.PENDING]) to the server.
     * After each successful upload the record is marked [com.trainingapp.data.model.SyncStatus.SYNCED].
     * On network failure the function returns silently; PENDING records will be
     * retried the next time connectivity is available.
     */
    suspend fun uploadPendingWorkouts()

    /**
     * Pull the latest data from the remote API and merge it into local storage.
     * On success, updated records are marked [com.trainingapp.data.model.SyncStatus.SYNCED].
     * On network failure the function returns silently; local data is preserved.
     */
    suspend fun syncWithApi()
}
