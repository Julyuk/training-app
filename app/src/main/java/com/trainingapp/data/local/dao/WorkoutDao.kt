package com.trainingapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.trainingapp.data.local.entity.WorkoutEntity
import com.trainingapp.data.local.entity.WorkoutExerciseEntity
import com.trainingapp.data.local.entity.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for workout persistence.
 *
 * Implements four required operations per entity:
 *  - [insertWorkout] / [insertExercises]  → save
 *  - [getAllWorkoutsWithExercises]         → read list
 *  - [getWorkoutsWithExercisesById]        → read one
 *  - [deleteWorkoutById]                  → delete (cascades to exercises via FK)
 */
@Dao
interface WorkoutDao {

    // ── Read list ────────────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY date DESC")
    fun getAllWorkoutsWithExercises(): Flow<List<WorkoutWithExercises>>

    // ── Read one ─────────────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun getWorkoutsWithExercisesById(id: Int): Flow<List<WorkoutWithExercises>>

    // ── Exercise lookup (used by ExerciseDetailScreen) ────────────────────────

    @Query("SELECT * FROM workout_exercises WHERE exercise_id = :exerciseId LIMIT 1")
    fun getExerciseByIdFlow(exerciseId: Int): Flow<WorkoutExerciseEntity?>

    // ── Save ─────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<WorkoutExerciseEntity>)

    @Query("DELETE FROM workout_exercises WHERE workout_id = :workoutId")
    suspend fun deleteExercisesByWorkoutId(workoutId: Int)

    /**
     * Replaces a workout and its full exercise list atomically.
     * When inserting a new workout (id = 0), Room auto-generates the real ID
     * and returns it from [insertWorkout]; that generated ID is used for the
     * exercises so they are never orphaned under workout_id = 0.
     * Deleting exercises first ensures removed exercises don't linger in the DB.
     */
    @Transaction
    suspend fun insertWorkoutWithExercises(workout: WorkoutEntity, exercises: List<WorkoutExerciseEntity>) {
        val generatedRowId = insertWorkout(workout)
        val actualId = if (workout.id == 0) generatedRowId.toInt() else workout.id
        deleteExercisesByWorkoutId(actualId)
        if (exercises.isNotEmpty()) {
            insertExercises(exercises.map { it.copy(workoutId = actualId) })
        }
    }

    @Query("SELECT COUNT(*) FROM workouts")
    suspend fun countWorkouts(): Int

    // ── Toggle completion ─────────────────────────────────────────────────────

    @Query("UPDATE workouts SET is_completed = NOT is_completed, sync_status = 'PENDING' WHERE id = :id")
    suspend fun toggleCompleted(id: Int)

    // ── Delete ────────────────────────────────────────────────────────────────

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkoutById(id: Int)

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()
}
