package com.trainingapp.data.remote.dto

import com.trainingapp.data.model.Exercise

/**
 * Data Transfer Object for an exercise received from the REST API.
 *
 * Maps 1-to-1 with the domain [Exercise] model; kept separate so that
 * API schema changes don't propagate directly into the domain layer.
 */
data class ExerciseDto(
    val id: Int,
    val name: String,
    val muscleGroup: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Float,
    val isBodyweight: Boolean,
    val notes: String
)

fun ExerciseDto.toDomain(): Exercise = Exercise(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    sets = sets,
    reps = reps,
    weightKg = weightKg,
    isBodyweight = isBodyweight,
    notes = notes
)

fun Exercise.toDto(): ExerciseDto = ExerciseDto(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    sets = sets,
    reps = reps,
    weightKg = weightKg,
    isBodyweight = isBodyweight,
    notes = notes
)
