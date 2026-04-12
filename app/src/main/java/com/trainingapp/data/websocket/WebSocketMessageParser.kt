package com.trainingapp.data.websocket

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Converts [WebSocketMessage] to and from JSON strings.
 *
 * Handles two things that Gson cannot do out of the box:
 * - [LocalDateTime] — serialised as ISO-8601 text (e.g. "2024-06-01T12:00:00").
 * - Unknown [MessageType] values — any unrecognised string falls back to
 *   [MessageType.SYSTEM] instead of throwing.
 *
 * Usage:
 * ```
 * val json  = WebSocketMessageParser.toJson(message)
 * val msg   = WebSocketMessageParser.parse(json)   // null on malformed input
 * ```
 */
object WebSocketMessageParser {

    private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter)
        .registerTypeAdapter(MessageType::class.java, MessageTypeAdapter)
        .create()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Deserialise [rawJson] into a [WebSocketMessage].
     * Returns **null** for blank input, malformed JSON, or any unexpected error.
     */
    fun parse(rawJson: String): WebSocketMessage? {
        if (rawJson.isBlank()) return null
        return try {
            val dto = gson.fromJson(rawJson, WebSocketMessageDto::class.java) ?: return null
            dto.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Serialise [message] into a JSON string suitable for sending over the wire.
     * The [WebSocketMessage.timestamp] is formatted as ISO-8601.
     */
    fun toJson(message: WebSocketMessage): String =
        gson.toJson(WebSocketMessageDto.fromDomain(message))

    // ── Custom Gson adapters ──────────────────────────────────────────────────

    private object LocalDateTimeAdapter :
        JsonSerializer<LocalDateTime>,
        JsonDeserializer<LocalDateTime> {

        override fun serialize(
            src: LocalDateTime,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement = context.serialize(src.format(FORMATTER))

        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): LocalDateTime = try {
            LocalDateTime.parse(json.asString, FORMATTER)
        } catch (e: DateTimeParseException) {
            LocalDateTime.now()
        }
    }

    private object MessageTypeAdapter : JsonDeserializer<MessageType> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): MessageType = try {
            MessageType.valueOf(json.asString)
        } catch (e: IllegalArgumentException) {
            MessageType.SYSTEM
        }
    }

    // ── Wire-format DTO ───────────────────────────────────────────────────────

    /**
     * Internal data-transfer object that matches the JSON wire format exactly.
     * All fields are nullable so Gson never throws on missing keys.
     */
    private data class WebSocketMessageDto(
        val type: String? = null,
        val title: String? = null,
        val body: String? = null,
        val timestamp: String? = null,
        val payload: Map<String, String>? = null
    ) {
        /** Convert the DTO into the domain model used by the rest of the app. */
        fun toDomain(): WebSocketMessage = WebSocketMessage(
            type = type
                ?.let { runCatching { MessageType.valueOf(it) }.getOrDefault(MessageType.SYSTEM) }
                ?: MessageType.SYSTEM,
            title = title.orEmpty(),
            body = body.orEmpty(),
            timestamp = timestamp
                ?.let { runCatching { LocalDateTime.parse(it, FORMATTER) }.getOrNull() }
                ?: LocalDateTime.now(),
            payload = payload ?: emptyMap()
        )

        companion object {
            /** Build a DTO from the domain model, ready for serialisation. */
            fun fromDomain(msg: WebSocketMessage) = WebSocketMessageDto(
                type = msg.type.name,
                title = msg.title,
                body = msg.body,
                timestamp = msg.timestamp.format(FORMATTER),
                payload = msg.payload.ifEmpty { null }
            )
        }
    }
}
