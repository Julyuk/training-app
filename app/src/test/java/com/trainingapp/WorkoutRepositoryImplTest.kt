package com.trainingapp

import app.cash.turbine.test
import com.trainingapp.data.local.dao.WorkoutDao
import com.trainingapp.data.local.entity.WorkoutExerciseEntity
import com.trainingapp.data.local.entity.WorkoutWithExercises
import com.trainingapp.data.local.entity.toEntity
import com.trainingapp.data.model.Exercise
import com.trainingapp.data.model.SyncStatus
import com.trainingapp.data.model.Workout
import com.trainingapp.data.model.WorkoutCategory
import com.trainingapp.data.remote.WorkoutApiService
import com.trainingapp.data.remote.dto.WorkoutDto
import com.trainingapp.data.repository.WorkoutRepositoryImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

/**
 * Unit tests for [WorkoutRepositoryImpl].
 *
 * Both [WorkoutDao] and [WorkoutApiService] are mocked so these tests run
 * entirely on the JVM without a real database or network.
 */
class WorkoutRepositoryImplTest {

    private lateinit var dao: WorkoutDao
    private lateinit var apiService: WorkoutApiService
    private lateinit var repository: WorkoutRepositoryImpl

    private val testExercise = Exercise(
        id = 1, name = "Push-up", muscleGroup = "Chest",
        sets = 3, reps = 15, weightKg = 0f, isBodyweight = true, notes = ""
    )

    private val testWorkout = Workout(
        id = 1, title = "Morning", description = "", durationMinutes = 30,
        caloriesBurned = 200, isCompleted = false,
        date = LocalDate.of(2024, 3, 10),
        category = WorkoutCategory.STRENGTH,
        exercises = listOf(testExercise),
        syncStatus = SyncStatus.PENDING
    )

    private fun fakeWithExercises(workout: Workout): WorkoutWithExercises {
        val entity = workout.toEntity()
        val exerciseEntities = workout.exercises.map { ex ->
            WorkoutExerciseEntity(
                workoutId = workout.id,
                exerciseId = ex.id,
                name = ex.name,
                muscleGroup = ex.muscleGroup,
                sets = ex.sets,
                reps = ex.reps,
                weightKg = ex.weightKg,
                isBodyweight = ex.isBodyweight,
                notes = ex.notes
            )
        }
        return WorkoutWithExercises(entity, exerciseEntities)
    }

    @Before
    fun setUp() {
        dao = mockk()
        apiService = mockk()
        repository = WorkoutRepositoryImpl(dao, apiService)
    }

    // ── getAllWorkouts ─────────────────────────────────────────────────────────

    @Test
    fun `getAllWorkouts emits domain models from DAO`() = runTest {
        val fakeData = listOf(fakeWithExercises(testWorkout))
        every { dao.getAllWorkoutsWithExercises() } returns flowOf(fakeData)

        repository.getAllWorkouts().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(testWorkout.id, list.first().id)
            assertEquals(testWorkout.title, list.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllWorkouts emits empty list when DAO is empty`() = runTest {
        every { dao.getAllWorkoutsWithExercises() } returns flowOf(emptyList())

        repository.getAllWorkouts().test {
            val list = awaitItem()
            assertEquals(0, list.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllWorkouts maps syncStatus correctly`() = runTest {
        val pendingWorkout = testWorkout.copy(syncStatus = SyncStatus.PENDING)
        every { dao.getAllWorkoutsWithExercises() } returns flowOf(listOf(fakeWithExercises(pendingWorkout)))

        repository.getAllWorkouts().test {
            val list = awaitItem()
            assertEquals(SyncStatus.PENDING, list.first().syncStatus)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── getWorkoutById ────────────────────────────────────────────────────────

    @Test
    fun `getWorkoutById returns matching workout`() = runTest {
        every { dao.getWorkoutsWithExercisesById(1) } returns flowOf(listOf(fakeWithExercises(testWorkout)))

        repository.getWorkoutById(1).test {
            val result = awaitItem()
            assertEquals(testWorkout.id, result?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getWorkoutById emits null when not found`() = runTest {
        every { dao.getWorkoutsWithExercisesById(999) } returns flowOf(emptyList())

        repository.getWorkoutById(999).test {
            val result = awaitItem()
            assertNull(result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── saveWorkout ───────────────────────────────────────────────────────────

    @Test
    fun `saveWorkout calls DAO insertWorkoutWithExercises`() = runTest {
        coEvery { dao.insertWorkoutWithExercises(any(), any()) } just Runs

        repository.saveWorkout(testWorkout)

        coVerify { dao.insertWorkoutWithExercises(testWorkout.toEntity(), any()) }
    }

    @Test
    fun `saveWorkout inserts correct number of exercises`() = runTest {
        val exercises = listOf(testExercise, testExercise.copy(id = 2, name = "Squat"))
        val workoutWithTwo = testWorkout.copy(exercises = exercises)

        val capturedExercises = mutableListOf<List<WorkoutExerciseEntity>>()
        coEvery { dao.insertWorkoutWithExercises(any(), capture(capturedExercises)) } just Runs

        repository.saveWorkout(workoutWithTwo)

        assertEquals(2, capturedExercises.first().size)
    }

    // ── deleteWorkout ─────────────────────────────────────────────────────────

    @Test
    fun `deleteWorkout calls DAO deleteWorkoutById`() = runTest {
        coEvery { dao.deleteWorkoutById(1) } just Runs

        repository.deleteWorkout(1)

        coVerify { dao.deleteWorkoutById(1) }
    }

    // ── syncWithApi ───────────────────────────────────────────────────────────

    @Test
    fun `syncWithApi saves all remote workouts to DAO`() = runTest {
        val remoteDto = WorkoutDto(
            id = 10, title = "Remote", description = "", durationMinutes = 45,
            caloriesBurned = 350, isCompleted = true, date = "2024-04-01",
            category = "CARDIO", exercises = emptyList()
        )
        coEvery { apiService.getWorkouts() } returns listOf(remoteDto)
        every { dao.getWorkoutsWithExercisesById(10) } returns flowOf(emptyList())
        coEvery { dao.insertWorkoutWithExercises(any(), any()) } just Runs

        repository.syncWithApi()

        coVerify { dao.insertWorkoutWithExercises(any(), any()) }
    }

    @Test
    fun `syncWithApi marks saved workouts as SYNCED`() = runTest {
        val remoteDto = WorkoutDto(
            id = 20, title = "Server Workout", description = "", durationMinutes = 60,
            caloriesBurned = 500, isCompleted = false, date = "2024-05-01",
            category = "STRENGTH", exercises = emptyList()
        )
        coEvery { apiService.getWorkouts() } returns listOf(remoteDto)
        every { dao.getWorkoutsWithExercisesById(20) } returns flowOf(emptyList())

        val insertedEntities = mutableListOf<com.trainingapp.data.local.entity.WorkoutEntity>()
        coEvery { dao.insertWorkoutWithExercises(capture(insertedEntities), any()) } just Runs

        repository.syncWithApi()

        assertEquals(SyncStatus.SYNCED, insertedEntities.first().syncStatus)
    }

    @Test
    fun `syncWithApi silently ignores network exceptions`() = runTest {
        // BUG FIX: was RuntimeException — but syncWithApi now only catches IOException.
        // RuntimeException would propagate and crash the caller, masking real bugs.
        // Use IOException to correctly simulate a network/connectivity failure.
        coEvery { apiService.getWorkouts() } throws IOException("No network")

        // Must not throw
        repository.syncWithApi()
    }

    @Test
    fun `syncWithApi does not write to DAO on network failure`() = runTest {
        coEvery { apiService.getWorkouts() } throws IOException("Timeout")

        repository.syncWithApi()

        coVerify(exactly = 0) { dao.insertWorkoutWithExercises(any(), any()) }
    }

    @Test
    fun `syncWithApi does not overwrite local PENDING workout`() = runTest {
        val remoteDto = WorkoutDto(
            id = 1, title = "Server Title", description = "", durationMinutes = 45,
            caloriesBurned = 350, isCompleted = true, date = "2024-04-01",
            category = "CARDIO", exercises = emptyList()
        )
        val localPending = fakeWithExercises(testWorkout.copy(syncStatus = SyncStatus.PENDING))
        coEvery { apiService.getWorkouts() } returns listOf(remoteDto)
        every { dao.getWorkoutsWithExercisesById(1) } returns flowOf(listOf(localPending))

        repository.syncWithApi()

        coVerify(exactly = 0) { dao.insertWorkoutWithExercises(any(), any()) }
    }

    @Test
    fun `syncWithApi overwrites local SYNCED workout with server data`() = runTest {
        val remoteDto = WorkoutDto(
            id = 1, title = "Updated on Server", description = "", durationMinutes = 45,
            caloriesBurned = 350, isCompleted = true, date = "2024-04-01",
            category = "CARDIO", exercises = emptyList()
        )
        val localSynced = fakeWithExercises(testWorkout.copy(syncStatus = SyncStatus.SYNCED))
        coEvery { apiService.getWorkouts() } returns listOf(remoteDto)
        every { dao.getWorkoutsWithExercisesById(1) } returns flowOf(listOf(localSynced))
        coEvery { dao.insertWorkoutWithExercises(any(), any()) } just Runs

        repository.syncWithApi()

        coVerify(exactly = 1) { dao.insertWorkoutWithExercises(any(), any()) }
    }

    @Test
    fun `toggleCompleted delegates to DAO`() = runTest {
        coEvery { dao.toggleCompleted(1) } just Runs

        repository.toggleCompleted(1)

        coVerify { dao.toggleCompleted(1) }
    }

    // ── uploadPendingWorkouts ─────────────────────────────────────────────────

    @Test
    fun `uploadPendingWorkouts calls createWorkout for each pending record`() = runTest {
        val pending = listOf(
            fakeWithExercises(testWorkout.copy(id = 1, syncStatus = SyncStatus.PENDING)),
            fakeWithExercises(testWorkout.copy(id = 2, title = "Evening Run", syncStatus = SyncStatus.PENDING))
        )
        coEvery { dao.getPendingWorkoutsWithExercises() } returns pending
        coEvery { apiService.createWorkout(any()) } answers {
            firstArg<com.trainingapp.data.remote.dto.WorkoutDto>().copy(id = 99)
        }
        coEvery { dao.updateSyncStatus(any(), any()) } just Runs

        repository.uploadPendingWorkouts()

        coVerify(exactly = 2) { apiService.createWorkout(any()) }
    }

    @Test
    fun `uploadPendingWorkouts marks each uploaded workout as SYNCED`() = runTest {
        val pending = listOf(fakeWithExercises(testWorkout.copy(syncStatus = SyncStatus.PENDING)))
        coEvery { dao.getPendingWorkoutsWithExercises() } returns pending
        coEvery { apiService.createWorkout(any()) } answers {
            firstArg<com.trainingapp.data.remote.dto.WorkoutDto>().copy(id = 99)
        }
        val capturedStatuses = mutableListOf<String>()
        coEvery { dao.updateSyncStatus(any(), capture(capturedStatuses)) } just Runs

        repository.uploadPendingWorkouts()

        assertEquals(SyncStatus.SYNCED.name, capturedStatuses.first())
    }

    @Test
    fun `uploadPendingWorkouts silently ignores network exceptions`() = runTest {
        coEvery { dao.getPendingWorkoutsWithExercises() } returns
            listOf(fakeWithExercises(testWorkout.copy(syncStatus = SyncStatus.PENDING)))
        coEvery { apiService.createWorkout(any()) } throws IOException("No network")

        // Must not propagate
        repository.uploadPendingWorkouts()
    }

    @Test
    fun `uploadPendingWorkouts does nothing when no PENDING records exist`() = runTest {
        coEvery { dao.getPendingWorkoutsWithExercises() } returns emptyList()

        repository.uploadPendingWorkouts()

        coVerify(exactly = 0) { apiService.createWorkout(any()) }
        coVerify(exactly = 0) { dao.updateSyncStatus(any(), any()) }
    }

    @Test
    fun `uploadPendingWorkouts does not call updateSyncStatus on network failure`() = runTest {
        coEvery { dao.getPendingWorkoutsWithExercises() } returns
            listOf(fakeWithExercises(testWorkout.copy(syncStatus = SyncStatus.PENDING)))
        coEvery { apiService.createWorkout(any()) } throws IOException("Timeout")

        repository.uploadPendingWorkouts()

        coVerify(exactly = 0) { dao.updateSyncStatus(any(), any()) }
    }
}
