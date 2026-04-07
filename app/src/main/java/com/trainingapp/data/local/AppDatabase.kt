package com.trainingapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.trainingapp.data.local.dao.WorkoutDao
import com.trainingapp.data.local.entity.WorkoutEntity
import com.trainingapp.data.local.entity.WorkoutExerciseEntity

/**
 * Single Room database for the Training App.
 *
 * Version history:
 *  1 – initial schema with workouts + workout_exercises tables.
 *  2 – WorkoutEntity primary key changed to autoGenerate = true.
 */
@Database(
    entities = [WorkoutEntity::class, WorkoutExerciseEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    companion object {
        private const val DATABASE_NAME = "training_app.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
