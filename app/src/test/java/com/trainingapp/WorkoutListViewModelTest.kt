package com.trainingapp

import app.cash.turbine.test
import com.trainingapp.data.model.Exercise
import com.trainingapp.data.model.SyncStatus
import com.trainingapp.data.model.Workout
import com.trainingapp.data.model.WorkoutCategory
import com.trainingapp.data.repository.WorkoutRepository
import com.trainingapp.ui.viewmodel.WorkoutListViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [WorkoutListViewModel].
 *
 * Uses [StandardTestDispatcher] and Turbine to verify that the StateFlow
 * exposes the correct state and that ViewModel actions delegate to the repository.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: WorkoutRepository
    private lateinit var viewModel: WorkoutListViewModel

    private fun makeWorkout(id: Int, title: String = "Workout $id") = Workout(
        id = id, title = title, description = "", durationMinutes = 45,
        caloriesBurned = 300, isCompleted = false,
        date = LocalDate.of(2024, 1, id),
        category = WorkoutCategory.STRENGTH,
        exercises = emptyList(),
        syncStatus = SyncStatus.SYNCED
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `workouts starts as empty list before repository emits`() = runTest {
        every { repository.getAllWorkouts() } returns flowOf(emptyList())
        viewModel = WorkoutListViewModel(repository)

        assertEquals(emptyList<Workout>(), viewModel.workouts.value)
    }

    @Test
    fun `workouts emits repository data`() = runTest {
        val workouts = listOf(makeWorkout(1), makeWorkout(2))
        every { repository.getAllWorkouts() } returns flowOf(workouts)
        viewModel = WorkoutListViewModel(repository)

        viewModel.workouts.test {
            // StateFlow with WhileSubscribed emits the initialValue first, then repository data
            awaitItem() // initial empty list
            val filled = awaitItem()
            assertEquals(2, filled.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `workouts reflects reactive database updates`() = runTest {
        val flow = MutableStateFlow<List<Workout>>(emptyList())
        every { repository.getAllWorkouts() } returns flow
        viewModel = WorkoutListViewModel(repository)

        viewModel.workouts.test {
            awaitItem() // empty
            flow.value = listOf(makeWorkout(1))
            val updated = awaitItem()
            assertEquals(1, updated.size)
            assertEquals(1, updated.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    fun `refresh triggers syncWithApi on repository`() = runTest {
        every { repository.getAllWorkouts() } returns flowOf(emptyList())
        coEvery { repository.syncWithApi() } just Runs
        viewModel = WorkoutListViewModel(repository)

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.syncWithApi() }
    }

    // ── deleteWorkout ─────────────────────────────────────────────────────────

    @Test
    fun `deleteWorkout delegates to repository`() = runTest {
        every { repository.getAllWorkouts() } returns flowOf(emptyList())
        coEvery { repository.deleteWorkout(any()) } just Runs
        viewModel = WorkoutListViewModel(repository)

        viewModel.deleteWorkout(42)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteWorkout(42) }
    }

    // ── addWorkout ────────────────────────────────────────────────────────────

    @Test
    fun `addWorkout delegates to repository saveWorkout`() = runTest {
        every { repository.getAllWorkouts() } returns flowOf(emptyList())
        coEvery { repository.saveWorkout(any()) } just Runs
        coEvery { repository.uploadPendingWorkouts() } just Runs
        viewModel = WorkoutListViewModel(repository)

        viewModel.addWorkout("Push Day", "Desc", 60, 400, true, WorkoutCategory.STRENGTH)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.saveWorkout(any()) }
    }

    @Test
    fun `addWorkout saves workout with PENDING syncStatus`() = runTest {
        every { repository.getAllWorkouts() } returns flowOf(emptyList())
        val saved = mutableListOf<Workout>()
        coEvery { repository.saveWorkout(capture(saved)) } just Runs
        coEvery { repository.uploadPendingWorkouts() } just Runs
        viewModel = WorkoutListViewModel(repository)

        viewModel.addWorkout("Leg Day", "", 45, 300, false, WorkoutCategory.STRENGTH)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SyncStatus.PENDING, saved.first().syncStatus)
    }

    @Test
    fun `addWorkout saves workout with correct fields`() = runTest {
        every { repository.getAllWorkouts() } returns flowOf(emptyList())
        val saved = mutableListOf<Workout>()
        coEvery { repository.saveWorkout(capture(saved)) } just Runs
        coEvery { repository.uploadPendingWorkouts() } just Runs
        viewModel = WorkoutListViewModel(repository)

        viewModel.addWorkout("HIIT Session", "Intervals", 30, 350, true, WorkoutCategory.HIIT)
        testDispatcher.scheduler.advanceUntilIdle()

        val workout = saved.first()
        assertEquals("HIIT Session", workout.title)
        assertEquals("Intervals", workout.description)
        assertEquals(30, workout.durationMinutes)
        assertEquals(350, workout.caloriesBurned)
        assertEquals(true, workout.isCompleted)
        assertEquals(WorkoutCategory.HIIT, workout.category)
    }

    // ── toggleCompleted ───────────────────────────────────────────────────────

    @Test
    fun `toggleCompleted delegates to repository`() = runTest {
        every { repository.getAllWorkouts() } returns flowOf(emptyList())
        coEvery { repository.toggleCompleted(any()) } just Runs
        coEvery { repository.uploadPendingWorkouts() } just Runs
        viewModel = WorkoutListViewModel(repository)

        viewModel.toggleCompleted(5)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.toggleCompleted(5) }
    }

    // ── updateWorkout ─────────────────────────────────────────────────────────

    @Test
    fun `updateWorkout delegates to repository saveWorkout`() = runTest {
        every { repository.getAllWorkouts() } returns flowOf(emptyList())
        val saved = mutableListOf<Workout>()
        coEvery { repository.saveWorkout(capture(saved)) } just Runs
        coEvery { repository.uploadPendingWorkouts() } just Runs
        viewModel = WorkoutListViewModel(repository)

        val updated = makeWorkout(1, "Updated").copy(syncStatus = SyncStatus.PENDING)
        viewModel.updateWorkout(updated)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, saved.size)
        assertEquals("Updated", saved.first().title)
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    @Test
    fun `factory creates WorkoutListViewModel instance`() {
        every { repository.getAllWorkouts() } returns flowOf(emptyList())
        val factory = WorkoutListViewModel.factory(repository)
        val vm = factory.create(WorkoutListViewModel::class.java)
        // If create() returns without throwing, the factory works correctly
        assertEquals(WorkoutListViewModel::class.java, vm::class.java)
    }
}
