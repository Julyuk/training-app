package com.trainingapp.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.trainingapp.data.model.Workout

/**
 * Room one-to-many relation: one [WorkoutEntity] has many [WorkoutExerciseEntity] rows.
 * Used by [WorkoutDao] queries annotated with @Transaction.
 */
data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workout_id"
    )
    val exercises: List<WorkoutExerciseEntity>
)

// ---------- Mapper ----------

fun WorkoutWithExercises.toDomain(): Workout = Workout(
    id = workout.id,
    title = workout.title,
    description = workout.description,
    durationMinutes = workout.durationMinutes,
    caloriesBurned = workout.caloriesBurned,
    isCompleted = workout.isCompleted,
    date = workout.date,
    category = workout.category,
    syncStatus = workout.syncStatus,
    exercises = exercises.map { it.toDomain() }
)

fun Workout.toEntity(): WorkoutEntity = WorkoutEntity(
    id = id,
    title = title,
    description = description,
    durationMinutes = durationMinutes,
    caloriesBurned = caloriesBurned,
    isCompleted = isCompleted,
    date = date,
    category = category,
    syncStatus = syncStatus
)
