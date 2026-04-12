package com.trainingapp.data.websocket

/**
 * Categories of messages that can arrive over the WebSocket channel.
 */
enum class MessageType {
    /** A friend or community member completed a workout. */
    WORKOUT_COMPLETED,

    /** A weekly or daily challenge has a status update. */
    CHALLENGE_UPDATE,

    /** Server-pushed motivational text. */
    MOTIVATIONAL,

    /** The server recommends a new workout plan. */
    NEW_WORKOUT_SUGGESTION,

    /** Low-level system / echo messages. */
    SYSTEM
}
