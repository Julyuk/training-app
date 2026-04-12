package com.trainingapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.trainingapp.data.websocket.ConnectionState
import com.trainingapp.data.websocket.SocketManager
import com.trainingapp.data.websocket.WebSocketMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Live Feed screen.
 *
 * Collects all inbound [WebSocketMessage]s and stores them newest-first in
 * [messages]. The WebSocket connection is opened when this ViewModel is
 * created and closed when it is cleared (screen leaves back-stack).
 */
class LiveFeedViewModel(
    private val socketManager: SocketManager,
    private val wsUrl: String = "ws://training-app.local/ws"
) : ViewModel() {

    /** Mirrors the current connection state from [socketManager]. */
    val connectionState: StateFlow<ConnectionState> = socketManager.connectionState

    private val _messages = MutableStateFlow<List<WebSocketMessage>>(emptyList())

    /** Received messages, newest first. */
    val messages: StateFlow<List<WebSocketMessage>> = _messages.asStateFlow()

    init {
        viewModelScope.launch {
            socketManager.messages.collect { msg ->
                // BUG FIX 1: use update{} for an atomic read-modify-write so
                //   rapid back-to-back messages can never clobber each other.
                // BUG FIX 2: cap at 100 entries so the list doesn't grow without
                //   bound over a long-running session (memory pressure).
                _messages.update { current -> (listOf(msg) + current).take(100) }
            }
        }
        socketManager.connect(wsUrl)
    }

    /** Remove all cached messages from the feed. */
    fun clearMessages() {
        _messages.value = emptyList()
    }

    /**
     * Attempt to re-establish the connection.
     * No-op if already connected or connecting.
     */
    fun reconnect() {
        socketManager.connect(wsUrl)
    }

    override fun onCleared() {
        super.onCleared()
        socketManager.disconnect()
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        fun factory(socketManager: SocketManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LiveFeedViewModel(socketManager) as T
            }
    }
}
