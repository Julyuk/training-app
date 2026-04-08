package com.trainingapp.data.remote

import com.trainingapp.data.SampleData
import com.trainingapp.data.remote.dto.WorkoutDto
import com.trainingapp.data.remote.dto.toDto
import kotlinx.coroutines.delay

/**
 * Mock implementation of [WorkoutApiService] that returns hard-coded data
 * with a simulated network delay.
 *
 * Replace this class with a Retrofit-based implementation once a real server
 * is available. The [WorkoutRepository] depends only on the [WorkoutApiService]
 * interface, so swapping implementations requires no changes in the rest of the app.
 */
class MockWorkoutApiService(
    private val delayMs: Long = 500L
) : WorkoutApiService {

    // In-memory mutable store so create/delete calls are observable within a session.
    private val store: MutableList<WorkoutDto> =
        SampleData.workouts.map { it.toDto() }.toMutableList()

    override suspend fun getWorkouts(): List<WorkoutDto> {
        delay(delayMs)
        return store.toList()
    }

    override suspend fun getWorkoutById(id: Int): WorkoutDto? {
        delay(delayMs)
        return store.find { it.id == id }
    }

    override suspend fun createWorkout(dto: WorkoutDto): WorkoutDto {
        delay(delayMs)
        val newId = (store.maxOfOrNull { it.id } ?: 0) + 1
        val created = dto.copy(id = newId)
        store.add(created)
        return created
    }

    override suspend fun deleteWorkout(id: Int) {
        delay(delayMs)
        store.removeAll { it.id == id }
    }
}
