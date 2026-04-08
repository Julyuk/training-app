package com.trainingapp.data.remote.dto

import com.trainingapp.data.model.SyncStatus
import com.trainingapp.data.model.Workout
import com.trainingapp.data.model.WorkoutCategory
import java.time.LocalDate

/**
 * Data Transfer Object for a workout received from or sent to the REST API.
 *
 * Uses primitive [String] for date and category so the DTO can be serialised
 * with any JSON library without extra adapters.
 */
data class WorkoutDto(
    val id: Int,
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val isCompleted: Boolean,
    val date: String,               // ISO-8601: "yyyy-MM-dd"
    val category: String,           // WorkoutCategory.name
    val exercises: List<ExerciseDto>
)

fun WorkoutDto.toDomain(): Workout = Workout(
    id = id,
    title = title,
    description = description,
    durationMinutes = durationMinutes,
    caloriesBurned = caloriesBurned,
    isCompleted = isCompleted,
    date = LocalDate.parse(date),
    category = WorkoutCategory.valueOf(category),
    exercises = exercises.map { it.toDomain() },
    syncStatus = SyncStatus.SYNCED
)

fun Workout.toDto(): WorkoutDto = WorkoutDto(
    id = id,
    title = title,
    description = description,
    durationMinutes = durationMinutes,
    caloriesBurned = caloriesBurned,
    isCompleted = isCompleted,
    date = date.toString(),
    category = category.name,
    exercises = exercises.map { it.toDto() }
)
