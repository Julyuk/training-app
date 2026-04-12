package com.trainingapp

import com.trainingapp.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Screen] sealed class route definitions.
 *
 * Verifies that every navigable destination has a unique route string and
 * that parameterised routes produce correct paths — without starting a device.
 */
class ScreenRoutesTest {

    // ── Route uniqueness ──────────────────────────────────────────────────────

    @Test
    fun `all top-level screen routes are distinct`() {
        val routes = listOf(
            Screen.WorkoutList.route,
            Screen.WorkoutDetail.route,
            Screen.ExerciseDetail.route,
            Screen.Profile.route,
            Screen.AddWorkout.route
        )
        assertEquals("Each screen must have a unique route", routes.size, routes.toSet().size)
    }

    @Test
    fun `WorkoutList route is a non-blank string`() {
        assertTrue(Screen.WorkoutList.route.isNotBlank())
    }

    @Test
    fun `Profile route is a non-blank string`() {
        assertTrue(Screen.Profile.route.isNotBlank())
    }

    @Test
    fun `AddWorkout route is a non-blank string`() {
        assertTrue(Screen.AddWorkout.route.isNotBlank())
    }

    // ── Parameterised route templates ─────────────────────────────────────────

    @Test
    fun `WorkoutDetail route template contains workoutId placeholder`() {
        assertTrue(Screen.WorkoutDetail.route.contains("workoutId"))
    }

    @Test
    fun `ExerciseDetail route template contains exerciseId placeholder`() {
        assertTrue(Screen.ExerciseDetail.route.contains("exerciseId"))
    }

    // ── createRoute helpers ───────────────────────────────────────────────────

    @Test
    fun `WorkoutDetail createRoute embeds the given id`() {
        val route = Screen.WorkoutDetail.createRoute(42)
        assertTrue("Route must contain the id", route.contains("42"))
        assertNotEquals(Screen.WorkoutDetail.route, route) // resolved, not template
    }

    @Test
    fun `ExerciseDetail createRoute embeds the given id`() {
        val route = Screen.ExerciseDetail.createRoute(7)
        assertTrue(route.contains("7"))
    }

    @Test
    fun `WorkoutDetail routes with different ids are different`() {
        val r1 = Screen.WorkoutDetail.createRoute(1)
        val r2 = Screen.WorkoutDetail.createRoute(2)
        assertNotEquals(r1, r2)
    }

    // ── Tab bar destinations ──────────────────────────────────────────────────

    @Test
    fun `WorkoutList and Profile are distinct top-level destinations`() {
        assertNotEquals(Screen.WorkoutList.route, Screen.Profile.route)
    }
}
