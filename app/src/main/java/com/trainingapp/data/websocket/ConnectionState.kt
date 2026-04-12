package com.trainingapp.data.websocket

/**
 * Represents the lifecycle states of a WebSocket connection.
 *
 * Transitions:
 *   Disconnected → Connecting → Connected → Reconnecting → Connecting → Connected
 *                                         ↘ Disconnected (explicit disconnect)
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object Reconnecting : ConnectionState()

    override fun toString(): String = this::class.simpleName ?: super.toString()
}
