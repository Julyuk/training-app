package com.trainingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trainingapp.data.model.Exercise
import com.trainingapp.data.model.Workout
import com.trainingapp.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for the workout detail and exercise detail screens.
 *
 * Both [workout] and [exercise] are reactive flows, so the UI updates
 * automatically when the underlying database row changes.
 */
class WorkoutDetailViewModel(
    repository: WorkoutRepository,
    workoutId: Int,
    exerciseId: Int = -1
) : ViewModel() {

    val workout: StateFlow<Workout?> = repository
        .getWorkoutById(workoutId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val exercise: StateFlow<Exercise?> = repository
        .getExerciseById(exerciseId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        fun factory(
            repository: WorkoutRepository,
            workoutId: Int,
            exerciseId: Int = -1
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    WorkoutDetailViewModel(repository, workoutId, exerciseId) as T
            }
    }
}
