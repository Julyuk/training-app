package com.trainingapp.data.remote

import com.trainingapp.data.remote.dto.WorkoutDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/*
 * ═══════════════════════════════════════════════════════════════════
 * REST API CONTRACT  –  Training App  –  Base URL: /api/v1
 * ═══════════════════════════════════════════════════════════════════
 *
 * ┌────────┬──────────────────────┬──────────────────────┬─────────────────────────────────┐
 * │ Method │ Path                 │ Returns              │ Body / Params                   │
 * ├────────┼──────────────────────┼──────────────────────┼─────────────────────────────────┤
 * │ GET    │ /workouts            │ List<WorkoutDto>     │ –                               │
 * │ GET    │ /workouts/{id}       │ WorkoutDto           │ Path: id (Int)                  │
 * │ POST   │ /workouts            │ WorkoutDto (created) │ Body: WorkoutDto (id ignored)   │
 * │ DELETE │ /workouts/{id}       │ 204 No Content       │ Path: id (Int)                  │
 * │ GET    │ /profile             │ UserProfileDto       │ –                               │
 * └────────┴──────────────────────┴──────────────────────┴─────────────────────────────────┘
 *
 * Error responses follow RFC 7807 Problem Details:
 *   { "status": 404, "title": "Not Found", "detail": "Workout 99 not found" }
 * ═══════════════════════════════════════════════════════════════════
 */

/**
 * Retrofit interface for all workout-related network calls.
 * The active implementation is [MockWorkoutApiService] with artificial delay.
 * To switch to a real server, build a Retrofit instance with base URL "/api/v1/"
 * and call `retrofit.create(WorkoutApiService::class.java)`.
 */
interface WorkoutApiService {

    /** GET /workouts — returns all workouts for the authenticated user. */
    @GET("workouts")
    suspend fun getWorkouts(): List<WorkoutDto>

    /** GET /workouts/{id} — returns a single workout or null if not found. */
    @GET("workouts/{id}")
    suspend fun getWorkoutById(@Path("id") id: Int): WorkoutDto?

    /** POST /workouts — creates a new workout; server assigns the final id. */
    @POST("workouts")
    suspend fun createWorkout(@Body dto: WorkoutDto): WorkoutDto

    /** DELETE /workouts/{id} — removes a workout; throws on failure. */
    @DELETE("workouts/{id}")
    suspend fun deleteWorkout(@Path("id") id: Int)
}
