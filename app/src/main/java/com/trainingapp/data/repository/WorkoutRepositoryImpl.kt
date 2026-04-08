package com.trainingapp.data.repository

import com.trainingapp.data.local.dao.WorkoutDao
import com.trainingapp.data.local.entity.toDomain
import com.trainingapp.data.local.entity.toEntity
import com.trainingapp.data.model.Exercise
import com.trainingapp.data.model.SyncStatus
import com.trainingapp.data.model.Workout
import com.trainingapp.data.remote.WorkoutApiService
import com.trainingapp.data.remote.dto.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/*
 * ════════════════════════════════════════════════════════════════════
 * SYNC STRATEGY: OFFLINE-FIRST
 * ════════════════════════════════════════════════════════════════════
 *
 * Reason for choice: a personal training log must be usable at the gym,
 * outdoors, or anywhere with poor connectivity.  The user should always
 * be able to browse history and log a new session without internet.
 *
 * How it works:
 *  1. Every READ goes to the local Room database (instant, reactive Flow).
 *  2. Every WRITE is saved locally first with SyncStatus.PENDING.
 *  3. syncWithApi() is called at startup and on connectivity restored:
 *     - fetches server state and merges into local DB (marks SYNCED).
 *     - PENDING records are uploaded (not yet implemented; placeholder).
 *  4. On network failure, syncWithApi() swallows the exception so the
 *     UI never sees an error from a background refresh.
 * ════════════════════════════════════════════════════════════════════
 */

/**
 * Concrete [WorkoutRepository] backed by a Room [WorkoutDao] and a
 * remote [WorkoutApiService] (currently mocked).
 */
class WorkoutRepositoryImpl(
    private val workoutDao: WorkoutDao,
    private val apiService: WorkoutApiService
) : WorkoutRepository {

    // ── Read list ────────────────────────────────────────────────────

    override fun getAllWorkouts(): Flow<List<Workout>> =
        workoutDao.getAllWorkoutsWithExercises().map { list ->
            list.map { it.toDomain() }
        }

    // ── Read one ─────────────────────────────────────────────────────

    override fun getWorkoutById(id: Int): Flow<Workout?> =
        workoutDao.getWorkoutsWithExercisesById(id).map { list ->
            list.firstOrNull()?.toDomain()
        }

    override fun getExerciseById(exerciseId: Int): Flow<Exercise?> =
        workoutDao.getExerciseByIdFlow(exerciseId).map { entity ->
            entity?.toDomain()
        }

    // ── Save ─────────────────────────────────────────────────────────

    override suspend fun saveWorkout(workout: Workout) {
        workoutDao.insertWorkoutWithExercises(
            workout.toEntity(),
            workout.exercises.map { it.toEntity(workout.id) }
        )
    }

    // ── Delete ────────────────────────────────────────────────────────

    override suspend fun toggleCompleted(id: Int) {
        workoutDao.toggleCompleted(id)
    }

    override suspend fun deleteWorkout(id: Int) {
        workoutDao.deleteWorkoutById(id)
    }

    // ── Sync ──────────────────────────────────────────────────────────

    /**
     * Pulls server data and writes it to local DB.
     * Each incoming workout is marked SYNCED, but only if the local copy is not
     * PENDING — a PENDING record means the user made a local change that hasn't
     * been uploaded yet, so we must not overwrite it with the (older) server value.
     * Silently ignores any network error so the app stays functional offline.
     */
    override suspend fun syncWithApi() {
        try {
            val remoteWorkouts = apiService.getWorkouts()
            remoteWorkouts.forEach { dto ->
                val incoming = dto.toDomain().copy(syncStatus = SyncStatus.SYNCED)
                val local = workoutDao.getWorkoutsWithExercisesById(incoming.id)
                    .map { it.firstOrNull()?.toDomain() }
                    .firstOrNull()
                if (local == null || local.syncStatus != SyncStatus.PENDING) {
                    saveWorkout(incoming)
                }
            }
        } catch (_: Exception) {
            // Network unavailable — local data remains the source of truth.
        }
    }
}
