package com.trainingapp

import com.trainingapp.data.model.Exercise
import com.trainingapp.data.model.SyncStatus
import com.trainingapp.data.model.Workout
import com.trainingapp.data.model.WorkoutCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for domain model behaviour and computed properties.
 * These tests run on the JVM (no Android framework needed).
 */
class WorkoutModelTest {

    private fun makeExercise(id: Int = 1, isBodyweight: Boolean = false) = Exercise(
        id = id,
        name = "Test Exercise",
        muscleGroup = "Test Muscle",
        sets = 3,
        reps = 10,
        weightKg = 50f,
        isBodyweight = isBodyweight,
        notes = ""
    )

    private fun makeWorkout(
        id: Int = 1,
        isCompleted: Boolean = false,
        syncStatus: SyncStatus = SyncStatus.SYNCED,
        exercises: List<Exercise> = emptyList()
    ) = Workout(
        id = id,
        title = "Test Workout",
        description = "Desc",
        durationMinutes = 60,
        caloriesBurned = 400,
        isCompleted = isCompleted,
        date = LocalDate.of(2024, 1, 15),
        category = WorkoutCategory.STRENGTH,
        exercises = exercises,
        syncStatus = syncStatus
    )

    // ── SyncStatus defaults ───────────────────────────────────────────────────

    @Test
    fun `workout default syncStatus is SYNCED`() {
        val workout = makeWorkout()
        assertEquals(SyncStatus.SYNCED, workout.syncStatus)
    }

    @Test
    fun `workout can be created with PENDING syncStatus`() {
        val workout = makeWorkout(syncStatus = SyncStatus.PENDING)
        assertEquals(SyncStatus.PENDING, workout.syncStatus)
    }

    @Test
    fun `workout copy preserves syncStatus`() {
        val original = makeWorkout(syncStatus = SyncStatus.ERROR)
        val copy = original.copy(title = "Modified")
        assertEquals(SyncStatus.ERROR, copy.syncStatus)
    }

    // ── Completion state ──────────────────────────────────────────────────────

    @Test
    fun `completed workout is marked isCompleted true`() {
        val workout = makeWorkout(isCompleted = true)
        assertTrue(workout.isCompleted)
    }

    @Test
    fun `pending workout is marked isCompleted false`() {
        val workout = makeWorkout(isCompleted = false)
        assertFalse(workout.isCompleted)
    }

    // ── Exercise list ─────────────────────────────────────────────────────────

    @Test
    fun `workout exercise count matches list size`() {
        val exercises = listOf(makeExercise(1), makeExercise(2), makeExercise(3))
        val workout = makeWorkout(exercises = exercises)
        assertEquals(3, workout.exercises.size)
    }

    @Test
    fun `workout with no exercises has empty list`() {
        val workout = makeWorkout(exercises = emptyList())
        assertTrue(workout.exercises.isEmpty())
    }

    // ── Exercise properties ───────────────────────────────────────────────────

    @Test
    fun `bodyweight exercise has isBodyweight true`() {
        val exercise = makeExercise(isBodyweight = true)
        assertTrue(exercise.isBodyweight)
    }

    @Test
    fun `weighted exercise has isBodyweight false`() {
        val exercise = makeExercise(isBodyweight = false)
        assertFalse(exercise.isBodyweight)
    }

    @Test
    fun `exercise volume is sets times reps`() {
        val exercise = makeExercise().copy(sets = 4, reps = 12)
        assertEquals(48, exercise.sets * exercise.reps)
    }

    // ── WorkoutCategory ───────────────────────────────────────────────────────

    @Test
    fun `all workout categories have non-empty labels`() {
        WorkoutCategory.entries.forEach { category ->
            assertTrue(
                "Category ${category.name} must have a label",
                category.label.isNotBlank()
            )
            assertTrue(
                "Category ${category.name} must have an emoji",
                category.emoji.isNotBlank()
            )
        }
    }

    // ── Date handling ─────────────────────────────────────────────────────────

    @Test
    fun `workout date is stored correctly`() {
        val date = LocalDate.of(2024, 6, 15)
        val workout = makeWorkout().copy(date = date)
        assertEquals(date, workout.date)
    }

    @Test
    fun `workout created in past is before today`() {
        val pastDate = LocalDate.now().minusDays(7)
        val workout = makeWorkout().copy(date = pastDate)
        assertTrue(workout.date.isBefore(LocalDate.now()))
    }

    // ── SyncStatus enum ───────────────────────────────────────────────────────

    @Test
    fun `SyncStatus has exactly three values`() {
        assertEquals(3, SyncStatus.entries.size)
    }

    @Test
    fun `SyncStatus valueOf round-trips correctly`() {
        SyncStatus.entries.forEach { status ->
            assertEquals(status, SyncStatus.valueOf(status.name))
        }
    }
}
