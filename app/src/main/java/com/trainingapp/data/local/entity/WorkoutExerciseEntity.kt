package com.trainingapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.trainingapp.data.model.Exercise

/**
 * Room entity representing an exercise entry within a specific workout.
 *
 * The same logical exercise (same [exerciseId]) may appear in multiple workouts,
 * so [rowId] is the table's auto-generated primary key while [exerciseId] is
 * the domain-level identifier used to navigate to ExerciseDetailScreen.
 *
 * The FK to [WorkoutEntity] cascades deletions, so removing a workout
 * automatically removes all its associated exercise rows.
 */
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workout_id")]
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0L,
    @ColumnInfo(name = "workout_id") val workoutId: Int,
    @ColumnInfo(name = "exercise_id") val exerciseId: Int,
    val name: String,
    @ColumnInfo(name = "muscle_group") val muscleGroup: String,
    val sets: Int,
    val reps: Int,
    @ColumnInfo(name = "weight_kg") val weightKg: Float,
    @ColumnInfo(name = "is_bodyweight") val isBodyweight: Boolean,
    val notes: String
)

// ---------- Mappers ----------

fun WorkoutExerciseEntity.toDomain(): Exercise = Exercise(
    id = exerciseId,
    name = name,
    muscleGroup = muscleGroup,
    sets = sets,
    reps = reps,
    weightKg = weightKg,
    isBodyweight = isBodyweight,
    notes = notes
)

fun Exercise.toEntity(workoutId: Int): WorkoutExerciseEntity = WorkoutExerciseEntity(
    workoutId = workoutId,
    exerciseId = id,
    name = name,
    muscleGroup = muscleGroup,
    sets = sets,
    reps = reps,
    weightKg = weightKg,
    isBodyweight = isBodyweight,
    notes = notes
)
