package com.trainingapp.data.websocket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for a WebSocket connection manager.
 *
 * Implementations handle the connection lifecycle and expose two reactive
 * streams so the UI layer never has to deal with threading directly.
 *
 * Typical usage:
 * ```
 * manager.connect(url)
 * manager.messages.collect { msg -> handleMessage(msg) }
 * // ...
 * manager.disconnect()
 * ```
 */
interface SocketManager {

    /** Current connection state; never null; starts as [ConnectionState.Disconnected]. */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Hot stream of inbound messages. Consumers that subscribe after a message
     * was emitted will not receive that message (no replay buffer).
     */
    val messages: Flow<WebSocketMessage>

    /**
     * Open (or reopen) a WebSocket to [url].
     * Must be idempotent: calling this while already [ConnectionState.Connected]
     * or [ConnectionState.Connecting] is a no-op.
     */
    fun connect(url: String)

    /**
     * Close the connection gracefully and transition to [ConnectionState.Disconnected].
     * Any in-flight simulation or reconnect attempt is cancelled.
     */
    fun disconnect()

    /**
     * Send [message] as a text frame over the open connection.
     * Silently ignored if the socket is not in [ConnectionState.Connected] state.
     */
    fun send(message: String)
}
