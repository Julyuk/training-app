package com.trainingapp.data.local

import kotlinx.coroutines.flow.Flow

/**
 * Abstracts network-connectivity detection so that ViewModels and Repositories
 * can react to online/offline transitions without importing Android framework classes.
 *
 * Production implementation: [ConnectivityNetworkMonitor].
 * Test implementation: a simple wrapper around [kotlinx.coroutines.flow.MutableStateFlow].
 *
 * ────────────────────────────────────────────────────────────────────────────
 * OFFLINE-FIRST STRATEGY (summary)
 * ────────────────────────────────────────────────────────────────────────────
 * • All reads come from the local Room database (instant, cached, reactive).
 * • All writes are saved locally first with SyncStatus.PENDING.
 * • When [isOnline] emits `true` after a period of `false`, the app:
 *     1. Uploads all PENDING workouts to the server via WorkoutRepository.uploadPendingWorkouts().
 *     2. Pulls the latest server state via WorkoutRepository.syncWithApi().
 * • Without internet the app is fully functional: add, edit, delete, browse.
 * ────────────────────────────────────────────────────────────────────────────
 */
interface NetworkMonitor {

    /**
     * Cold flow that emits the current connectivity state on subscription,
     * then emits again whenever it changes:
     *  - `true`  — device has a validated internet connection
     *  - `false` — device is offline or connection is unvalidated
     */
    val isOnline: Flow<Boolean>
}
