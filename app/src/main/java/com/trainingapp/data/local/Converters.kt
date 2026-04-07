package com.trainingapp.data.local

import androidx.room.TypeConverter
import com.trainingapp.data.model.SyncStatus
import com.trainingapp.data.model.WorkoutCategory
import java.time.LocalDate

/**
 * Room TypeConverters for types that cannot be stored as primitives.
 * Registered globally on [AppDatabase] via @TypeConverters.
 */
class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    }

    @TypeConverter
    fun fromWorkoutCategory(category: WorkoutCategory): String = category.name

    @TypeConverter
    fun toWorkoutCategory(value: String): WorkoutCategory = WorkoutCategory.valueOf(value)

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
