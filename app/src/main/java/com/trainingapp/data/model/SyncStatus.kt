package com.trainingapp.data.model

/**
 * Tracks whether a local record has been successfully synchronised with the remote server.
 *
 * PENDING – created or modified offline, not yet uploaded.
 * SYNCED  – matches the server's current state.
 * ERROR   – last sync attempt failed; will retry on next connectivity event.
 */
enum class SyncStatus {
    PENDING,
    SYNCED,
    ERROR
}
