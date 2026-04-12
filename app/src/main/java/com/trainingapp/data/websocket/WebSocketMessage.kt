package com.trainingapp.data.websocket

import java.time.LocalDateTime

/**
 * Domain model for a single inbound WebSocket event.
 *
 * @param type     Semantic category of the message (see [MessageType]).
 * @param title    Short human-readable heading (shown in the feed card).
 * @param body     Full message text.
 * @param timestamp When the message was received; defaults to now.
 * @param payload  Optional key-value extras (e.g. workoutId, challengeId).
 */
data class WebSocketMessage(
    val type: MessageType,
    val title: String,
    val body: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val payload: Map<String, String> = emptyMap()
)
