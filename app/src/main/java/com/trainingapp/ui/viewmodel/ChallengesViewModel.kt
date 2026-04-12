package com.trainingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trainingapp.data.model.Challenge
import com.trainingapp.data.model.SampleChallenges
import com.trainingapp.data.model.Workout
import com.trainingapp.data.model.WorkoutCategory
import com.trainingapp.data.repository.WorkoutRepository
import com.trainingapp.data.websocket.MessageType
import com.trainingapp.data.websocket.SocketManager
import com.trainingapp.data.websocket.WebSocketMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Challenges screen.
 *
 * **Challenge ↔ Workout connection:**
 * Each [Challenge] carries a [Challenge.relatedCategory]. When the user joins a
 * challenge, [computeProgress] counts completed workouts of that category from
 * the live Room [Flow], so ticking off a workout immediately updates progress.
 *
 * **Bugs fixed in this version:**
 * - BUG: `_joinState.value = _joinState.value.toMutableMap()…` was a non-atomic
 *   read-modify-write. Fixed with `_joinState.update { }`.
 * - BUG: `joinedCategories` used `combine(_joinState, challenges)` but ignored
 *   `_joinState` entirely (used `_` parameter). This created a diamond dependency
 *   that could emit intermediate "glitch" values. Fixed by mapping directly from
 *   `challenges`, which already captures the latest join state.
 * - BUG: `computeProgress` ran the O(n) filter before the O(1) guard on
 *   `targetCount`. Guard moved to the top.
 */
class ChallengesViewModel(
    private val socketManager: SocketManager,
    private val repository: WorkoutRepository
) : ViewModel() {

    // ── Base join-state (user-controlled) ────────────────────────────────────

    private val _joinState = MutableStateFlow(
        SampleChallenges.all.associate { it.id to it.isJoined }
    )

    // ── Challenges with live progress computed from real workouts ─────────────

    /**
     * Combines the user's join-state with the live workout list from Room.
     * Emits a fresh list whenever a workout is completed, added, or deleted,
     * or whenever the user joins/leaves a challenge — no manual refresh needed.
     */
    val challenges: StateFlow<List<Challenge>> = combine(
        _joinState,
        repository.getAllWorkouts()
    ) { joinMap, workouts ->
        SampleChallenges.all.map { base ->
            val isJoined = joinMap[base.id] ?: base.isJoined
            val progress = if (isJoined) computeProgress(base, workouts) else 0
            base.copy(isJoined = isJoined, progressPct = progress)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SampleChallenges.all
    )

    // ── Categories with active joined challenges ──────────────────────────────

    /**
     * The set of [WorkoutCategory] values that have at least one joined challenge.
     *
     * BUG FIX: previously `combine(_joinState, challenges) { _, ch -> … }` —
     * `_joinState` was listed as an upstream but its value was discarded (`_`),
     * creating a diamond dependency with potential glitch emissions. Now we simply
     * map from [challenges], which already reflects the latest join state.
     */
    val joinedCategories: StateFlow<Set<WorkoutCategory>> = challenges.map { list ->
        list.filter { it.isJoined }
            .mapNotNull { it.relatedCategory }
            .toSet()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SampleChallenges.all
            .filter { it.isJoined }
            .mapNotNull { it.relatedCategory }
            .toSet()
    )

    // ── WS update banner ──────────────────────────────────────────────────────

    private val _lastUpdate = MutableStateFlow<WebSocketMessage?>(null)

    /** The most recent CHALLENGE_UPDATE message pushed by the server. */
    val lastUpdate: StateFlow<WebSocketMessage?> = _lastUpdate.asStateFlow()

    init {
        viewModelScope.launch {
            socketManager.messages
                .filter { it.type == MessageType.CHALLENGE_UPDATE }
                .collect { msg -> _lastUpdate.value = msg }
        }
    }

    // ── User actions ──────────────────────────────────────────────────────────

    /**
     * Join (or leave) the challenge with [id].
     *
     * BUG FIX: previously `_joinState.value = _joinState.value.toMutableMap()…`
     * — a non-atomic read-modify-write. If two rapid taps could race (e.g. from
     * a double-tap), the second write could overwrite the first. Fixed with
     * `update { }` which performs an atomic compare-and-set loop.
     */
    fun toggleJoin(id: Int) {
        _joinState.update { current ->
            current.toMutableMap().also { it[id] = !(it[id] ?: false) }
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Returns a progress percentage [0..100].
     *
     * BUG FIX: the O(1) guard on [Challenge.targetCount] was previously placed
     * AFTER the O(n) filter, wasting work on every emission when targetCount = 0.
     * Guard is now the first thing evaluated.
     */
    /**
     * Returns a progress percentage [0..100].
     *
     * Only workouts that:
     *  - are marked completed,
     *  - fall within [Challenge.startDate]..[Challenge.endDate] (inclusive), and
     *  - match [Challenge.relatedCategory] (or any category when it is null)
     * count toward the challenge target.
     */
    private fun computeProgress(challenge: Challenge, allWorkouts: List<Workout>): Int {
        if (challenge.targetCount <= 0) return 0
        val relevant = allWorkouts.filter { w ->
            w.isCompleted &&
                !w.date.isBefore(challenge.startDate) &&
                !w.date.isAfter(challenge.endDate) &&
                (challenge.relatedCategory == null || w.category == challenge.relatedCategory)
        }
        return (relevant.size * 100 / challenge.targetCount).coerceAtMost(100)
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        fun factory(
            socketManager: SocketManager,
            repository: WorkoutRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChallengesViewModel(socketManager, repository) as T
            }
    }
}
