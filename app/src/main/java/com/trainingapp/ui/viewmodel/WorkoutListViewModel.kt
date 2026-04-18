package com.trainingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trainingapp.data.model.SyncStatus
import com.trainingapp.data.model.Workout
import com.trainingapp.data.model.WorkoutCategory
import com.trainingapp.data.repository.WorkoutRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the workout list screen.
 *
 * Exposes [workouts] as a [StateFlow] that the UI collects; any database
 * change is automatically pushed to the screen without a manual refresh.
 *
 * [refresh] triggers a background API sync so that new server data
 * eventually appears in the local database (and thus in the flow).
 */
class WorkoutListViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val workouts: StateFlow<List<Workout>> = repository
        .getAllWorkouts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun refresh() {
        viewModelScope.launch {
            repository.syncWithApi()
        }
    }

    fun toggleCompleted(id: Int) {
        viewModelScope.launch {
            repository.toggleCompleted(id)
            repository.uploadPendingWorkouts()
        }
    }

    fun updateWorkout(workout: Workout) {
        viewModelScope.launch {
            repository.saveWorkout(workout)
            repository.uploadPendingWorkouts()
        }
    }

    fun deleteWorkout(id: Int) {
        viewModelScope.launch {
            repository.deleteWorkout(id)
        }
    }

    fun addWorkout(
        title: String,
        description: String,
        durationMinutes: Int,
        caloriesBurned: Int,
        isCompleted: Boolean,
        category: WorkoutCategory,
        date: LocalDate = LocalDate.now()
    ) {
        viewModelScope.launch {
            repository.saveWorkout(
                Workout(
                    id = 0,
                    title = title,
                    description = description,
                    durationMinutes = durationMinutes,
                    caloriesBurned = caloriesBurned,
                    isCompleted = isCompleted,
                    date = date,
                    category = category,
                    exercises = emptyList(),
                    syncStatus = SyncStatus.PENDING
                )
            )
            repository.uploadPendingWorkouts()
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        fun factory(repository: WorkoutRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    WorkoutListViewModel(repository) as T
            }
    }
}
