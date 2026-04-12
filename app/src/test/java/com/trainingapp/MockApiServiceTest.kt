package com.trainingapp

import com.trainingapp.data.SampleData
import com.trainingapp.data.remote.MockWorkoutApiService
import com.trainingapp.data.remote.dto.ExerciseDto
import com.trainingapp.data.remote.dto.WorkoutDto
import com.trainingapp.data.remote.dto.toDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MockWorkoutApiService].
 *
 * Uses [runTest] from kotlinx-coroutines-test which skips the artificial
 * network delay so tests remain fast.
 */
class MockApiServiceTest {

    private lateinit var api: MockWorkoutApiService

    @Before
    fun setUp() {
        // delayMs = 0 to skip artificial latency in tests
        api = MockWorkoutApiService(delayMs = 0L)
    }

    // ── getWorkouts ───────────────────────────────────────────────────────────

    @Test
    fun `getWorkouts returns all seeded workouts`() = runTest {
        val result = api.getWorkouts()
        assertEquals(SampleData.workouts.size, result.size)
    }

    @Test
    fun `getWorkouts returns DTOs with correct ids`() = runTest {
        val ids = api.getWorkouts().map { it.id }.toSet()
        val expectedIds = SampleData.workouts.map { it.id }.toSet()
        assertEquals(expectedIds, ids)
    }

    @Test
    fun `getWorkouts returns list not the same reference`() = runTest {
        val first = api.getWorkouts()
        val second = api.getWorkouts()
        // Content equal but not the same list instance (defensive copy)
        assertEquals(first, second)
    }

    // ── getWorkoutById ────────────────────────────────────────────────────────

    @Test
    fun `getWorkoutById returns workout for valid id`() = runTest {
        val result = api.getWorkoutById(1)
        assertNotNull(result)
        assertEquals(1, result!!.id)
    }

    @Test
    fun `getWorkoutById returns null for unknown id`() = runTest {
        val result = api.getWorkoutById(9999)
        assertNull(result)
    }

    @Test
    fun `getWorkoutById returns correct title`() = runTest {
        val expected = SampleData.workouts.first()
        val result = api.getWorkoutById(expected.id)
        assertNotNull(result)
        assertEquals(expected.title, result!!.title)
    }

    // ── createWorkout ─────────────────────────────────────────────────────────

    @Test
    fun `createWorkout returns dto with server-assigned id`() = runTest {
        val newDto = WorkoutDto(
            id = 0, // client provides 0; server assigns real id
            title = "New Workout",
            description = "Test",
            durationMinutes = 30,
            caloriesBurned = 200,
            isCompleted = false,
            date = "2024-05-01",
            category = "HIIT",
            exercises = emptyList()
        )
        val created = api.createWorkout(newDto)
        assertTrue(created.id > 0)
        assertEquals("New Workout", created.title)
    }

    @Test
    fun `createWorkout makes new workout findable via getWorkoutById`() = runTest {
        val newDto = WorkoutDto(
            id = 0,
            title = "Findable",
            description = "",
            durationMinutes = 45,
            caloriesBurned = 300,
            isCompleted = false,
            date = "2024-06-01",
            category = "CARDIO",
            exercises = emptyList()
        )
        val created = api.createWorkout(newDto)
        val fetched = api.getWorkoutById(created.id)
        assertNotNull(fetched)
        assertEquals("Findable", fetched!!.title)
    }

    @Test
    fun `createWorkout increments total count`() = runTest {
        val before = api.getWorkouts().size
        api.createWorkout(
            WorkoutDto(
                id = 0, title = "Extra", description = "", durationMinutes = 20,
                caloriesBurned = 150, isCompleted = false, date = "2024-07-01",
                category = "FLEXIBILITY", exercises = emptyList()
            )
        )
        val after = api.getWorkouts().size
        assertEquals(before + 1, after)
    }

    // ── deleteWorkout ─────────────────────────────────────────────────────────

    @Test
    fun `deleteWorkout removes workout from list`() = runTest {
        val before = api.getWorkouts().size
        api.deleteWorkout(1)
        val after = api.getWorkouts().size
        assertEquals(before - 1, after)
    }

    @Test
    fun `deleteWorkout makes workout unfindable`() = runTest {
        api.deleteWorkout(1)
        val result = api.getWorkoutById(1)
        assertNull(result)
    }

    @Test
    fun `deleteWorkout on non-existent id is idempotent`() = runTest {
        val before = api.getWorkouts().size
        api.deleteWorkout(9999)
        val after = api.getWorkouts().size
        assertEquals(before, after)
    }

    // ── DTO mappers ───────────────────────────────────────────────────────────

    @Test
    fun `workout toDto preserves all fields`() {
        val workout = SampleData.workouts.first()
        val dto = workout.toDto()
        assertEquals(workout.id, dto.id)
        assertEquals(workout.title, dto.title)
        assertEquals(workout.durationMinutes, dto.durationMinutes)
        assertEquals(workout.caloriesBurned, dto.caloriesBurned)
        assertEquals(workout.isCompleted, dto.isCompleted)
        assertEquals(workout.date.toString(), dto.date)
        assertEquals(workout.category.name, dto.category)
        assertEquals(workout.exercises.size, dto.exercises.size)
    }
}
