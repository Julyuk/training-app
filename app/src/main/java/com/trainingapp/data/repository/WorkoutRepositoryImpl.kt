package com.trainingapp.data.repository

import com.trainingapp.data.local.dao.WorkoutDao
import com.trainingapp.data.local.entity.toDomain
import com.trainingapp.data.local.entity.toEntity
import com.trainingapp.data.model.Exercise
import com.trainingapp.data.model.SyncStatus
import com.trainingapp.data.model.Workout
import com.trainingapp.data.remote.WorkoutApiService
import com.trainingapp.data.remote.dto.toDto
import com.trainingapp.data.remote.dto.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.IOException

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
        workoutDao.getAllWorkoutsWithExercises()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)

    // ── Read one ─────────────────────────────────────────────────────

    override fun getWorkoutById(id: Int): Flow<Workout?> =
        workoutDao.getWorkoutsWithExercisesById(id)
            .map { list -> list.firstOrNull()?.toDomain() }
            .flowOn(Dispatchers.Default)

    override fun getExerciseById(exerciseId: Int): Flow<Exercise?> =
        workoutDao.getExerciseByIdFlow(exerciseId)
            .map { entity -> entity?.toDomain() }
            .flowOn(Dispatchers.Default)

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

    // ── Upload pending ────────────────────────────────────────────────

    /**
     * Uploads every PENDING workout to the server.
     *
     * Flow:
     *  1. Query Room for all records with sync_status = PENDING.
     *  2. For each, call [apiService.createWorkout] (mock: 500 ms delay).
     *  3. On success, mark the local record as SYNCED via [workoutDao.updateSyncStatus].
     *
     * If the network is unavailable (IOException), the function returns silently
     * so the app stays functional offline.  PENDING records remain PENDING and
     * will be retried the next time [ConnectivityNetworkMonitor] reports online.
     */
    override suspend fun uploadPendingWorkouts() {
        val pending = try {
            workoutDao.getPendingWorkoutsWithExercises()
        } catch (_: Exception) {
            return
        }
        if (pending.isEmpty()) return

        // Upload all pending workouts in parallel — total time ≈ one network round-trip
        // instead of N × round-trip when uploading sequentially.
        val results: List<Pair<Int, SyncStatus?>> = coroutineScope {
            pending.map { withExercises ->
                async {
                    val workout = withExercises.toDomain()
                    val newStatus: SyncStatus? = try {
                        apiService.createWorkout(workout.toDto())
                        SyncStatus.SYNCED
                    } catch (_: IOException) {
                        null // leave PENDING — retried on next connectivity restore
                    } catch (_: Exception) {
                        SyncStatus.ERROR
                    }
                    workout.id to newStatus
                }
            }.map { it.await() }
        }

        // One transaction for all status updates → Room fires one invalidation
        // instead of N, so the list screen recomposes only once.
        val updates = results.mapNotNull { (id, status) -> status?.let { id to it.name } }
        if (updates.isNotEmpty()) workoutDao.batchUpdateSyncStatuses(updates)
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
            // Fetch remote and local state in parallel — both are independent reads.
            val remoteWorkouts: List<com.trainingapp.data.remote.dto.WorkoutDto>
            val localById: Map<Int, Workout>
            coroutineScope {
                val remoteDeferred = async { apiService.getWorkouts() }
                val localDeferred = async {
                    workoutDao.getAllWorkoutsSnapshot().associate { it.workout.id to it.toDomain() }
                }
                remoteWorkouts = remoteDeferred.await()
                localById = localDeferred.await()
            }

            // Skip workouts where local copy is PENDING (unsaved user changes).
            val toSave = remoteWorkouts.mapNotNull { dto ->
                val incoming = dto.toDomain().copy(syncStatus = SyncStatus.SYNCED)
                val local = localById[incoming.id]
                if (local == null || local.syncStatus != SyncStatus.PENDING) incoming else null
            }

            // One transaction → one Room invalidation → one list recomposition.
            if (toSave.isNotEmpty()) {
                workoutDao.batchInsertWorkoutsWithExercises(
                    toSave.map { w -> w.toEntity() to w.exercises.map { it.toEntity(w.id) } }
                )
            }
        } catch (_: IOException) {
            // Network unavailable — local data remains the source of truth.
        }
    }
}
