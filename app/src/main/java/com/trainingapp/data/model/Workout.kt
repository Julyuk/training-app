package com.trainingapp.data.model

import java.time.LocalDate

/**
 * Represents a single workout session logged by the user.
 * Acts as a container for one or more [Exercise] entries and
 * stores summary statistics such as duration and calories burned.
 *
 * [syncStatus] indicates whether this record has been uploaded to the server.
 * New records start as [SyncStatus.PENDING] and move to [SyncStatus.SYNCED]
 * after a successful API call.
 */
data class Workout(
    val id: Int,
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val isCompleted: Boolean,
    val date: LocalDate,
    val category: WorkoutCategory,
    val exercises: List<Exercise>,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
