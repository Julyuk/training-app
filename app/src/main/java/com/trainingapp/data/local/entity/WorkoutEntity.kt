package com.trainingapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trainingapp.data.model.SyncStatus
import com.trainingapp.data.model.WorkoutCategory
import java.time.LocalDate

/**
 * Room entity that persists one workout session.
 * Exercises are stored in a separate [WorkoutExerciseEntity] table
 * linked via [WorkoutWithExercises].
 */
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val title: String,
    val description: String,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,
    @ColumnInfo(name = "calories_burned") val caloriesBurned: Int,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    val date: LocalDate,
    val category: WorkoutCategory,
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus
)
