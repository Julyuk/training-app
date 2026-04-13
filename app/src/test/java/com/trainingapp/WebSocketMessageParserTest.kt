package com.trainingapp

import com.trainingapp.data.websocket.MessageType
import com.trainingapp.data.websocket.WebSocketMessage
import com.trainingapp.data.websocket.WebSocketMessageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * Unit tests for [WebSocketMessageParser].
 *
 * Covers serialisation, deserialisation, round-trips, edge cases (missing
 * fields, unknown message types, malformed JSON, blank input).
 */
class WebSocketMessageParserTest {

    // ── parse: happy paths ────────────────────────────────────────────────────

    @Test
    fun `parse returns WebSocketMessage for well-formed JSON`() {
        val json = """
            {
              "type": "MOTIVATIONAL",
              "title": "День добрий!",
              "body": "Зроби 10 хвилин розминки.",
              "timestamp": "2024-06-01T09:00:00",
              "payload": {}
            }
        """.trimIndent()

        val result = WebSocketMessageParser.parse(json)

        assertNotNull(result)
        assertEquals(MessageType.MOTIVATIONAL, result!!.type)
        assertEquals("День добрий!", result.title)
        assertEquals("Зроби 10 хвилин розминки.", result.body)
    }

    @Test
    fun `parse correctly maps all MessageType values`() {
        val types = MessageType.entries
        for (type in types) {
            val json = """{"type":"${type.name}","title":"t","body":"b"}"""
            val result = WebSocketMessageParser.parse(json)
            assertNotNull("parse returned null for type $type", result)
            assertEquals(type, result!!.type)
        }
    }

    @Test
    fun `parse deserialises payload map`() {
        val json = """
            {
              "type": "NEW_WORKOUT_SUGGESTION",
              "title": "HIIT",
              "body": "Спробуй!",
              "payload": {"category": "HIIT", "durationMinutes": "20"}
            }
        """.trimIndent()

        val result = WebSocketMessageParser.parse(json)

        assertNotNull(result)
        assertEquals("HIIT", result!!.payload["category"])
        assertEquals("20", result.payload["durationMinutes"])
    }

    @Test
    fun `parse deserialises timestamp from ISO-8601 string`() {
        val json = """
            {"type":"SYSTEM","title":"t","body":"b","timestamp":"2024-03-15T14:30:00"}
        """.trimIndent()

        val result = WebSocketMessageParser.parse(json)

        assertNotNull(result)
        assertEquals(2024, result!!.timestamp.year)
        assertEquals(3, result.timestamp.monthValue)
        assertEquals(15, result.timestamp.dayOfMonth)
        assertEquals(14, result.timestamp.hour)
        assertEquals(30, result.timestamp.minute)
    }

    // ── parse: missing / optional fields ─────────────────────────────────────

    @Test
    fun `parse uses empty map when payload is absent`() {
        val json = """{"type":"MOTIVATIONAL","title":"Hi","body":"Go!"}"""
        val result = WebSocketMessageParser.parse(json)
        assertNotNull(result)
        assertTrue(result!!.payload.isEmpty())
    }

    @Test
    fun `parse uses current time when timestamp is absent`() {
        val before = LocalDateTime.now()
        val json = """{"type":"SYSTEM","title":"t","body":"b"}"""
        val result = WebSocketMessageParser.parse(json)
        val after = LocalDateTime.now()

        assertNotNull(result)
        assertTrue(!result!!.timestamp.isBefore(before))
        assertTrue(!result.timestamp.isAfter(after))
    }

    @Test
    fun `parse uses empty strings when title and body are absent`() {
        val json = """{"type":"SYSTEM"}"""
        val result = WebSocketMessageParser.parse(json)
        assertNotNull(result)
        assertEquals("", result!!.title)
        assertEquals("", result.body)
    }

    // ── parse: unknown / invalid type ─────────────────────────────────────────

    @Test
    fun `parse falls back to SYSTEM for unknown message type`() {
        val json = """{"type":"TOTALLY_UNKNOWN","title":"?","body":"?"}"""
        val result = WebSocketMessageParser.parse(json)
        assertNotNull(result)
        assertEquals(MessageType.SYSTEM, result!!.type)
    }

    @Test
    fun `parse falls back to SYSTEM when type field is missing`() {
        val json = """{"title":"No type","body":"test"}"""
        val result = WebSocketMessageParser.parse(json)
        assertNotNull(result)
        assertEquals(MessageType.SYSTEM, result!!.type)
    }

    // ── parse: error handling ─────────────────────────────────────────────────

    @Test
    fun `parse returns null for blank input`() {
        assertNull(WebSocketMessageParser.parse(""))
        assertNull(WebSocketMessageParser.parse("   "))
    }

    @Test
    fun `parse returns null for malformed JSON`() {
        assertNull(WebSocketMessageParser.parse("{not valid json"))
    }

    @Test
    fun `parse returns null for a plain text string`() {
        assertNull(WebSocketMessageParser.parse("hello world"))
    }

    // ── toJson ────────────────────────────────────────────────────────────────

    @Test
    fun `toJson produces a non-blank string`() {
        val message = WebSocketMessage(
            type = MessageType.CHALLENGE_UPDATE,
            title = "Виклик тижня",
            body = "Залишилось 2 дні!"
        )
        val json = WebSocketMessageParser.toJson(message)
        assertTrue(json.isNotBlank())
    }

    @Test
    fun `toJson includes the message type as its enum name`() {
        val message = WebSocketMessage(
            type = MessageType.WORKOUT_COMPLETED,
            title = "Готово",
            body = "Тренування завершено"
        )
        val json = WebSocketMessageParser.toJson(message)
        assertTrue(json.contains("WORKOUT_COMPLETED"))
    }

    // ── round-trip ────────────────────────────────────────────────────────────

    @Test
    fun `round-trip preserves type, title, and body`() {
        val original = WebSocketMessage(
            type = MessageType.NEW_WORKOUT_SUGGESTION,
            title = "Силове тренування",
            body = "Присідання та жим"
        )
        val parsed = WebSocketMessageParser.parse(WebSocketMessageParser.toJson(original))

        assertNotNull(parsed)
        assertEquals(original.type, parsed!!.type)
        assertEquals(original.title, parsed.title)
        assertEquals(original.body, parsed.body)
    }

    @Test
    fun `round-trip preserves payload map`() {
        val original = WebSocketMessage(
            type = MessageType.NEW_WORKOUT_SUGGESTION,
            title = "HIIT",
            body = "20 хв",
            payload = mapOf("category" to "HIIT", "durationMinutes" to "20")
        )
        val parsed = WebSocketMessageParser.parse(WebSocketMessageParser.toJson(original))

        assertNotNull(parsed)
        assertEquals(original.payload, parsed!!.payload)
    }

    @Test
    fun `round-trip preserves timestamp to second precision`() {
        val ts = LocalDateTime.of(2024, 5, 20, 8, 45, 0)
        val original = WebSocketMessage(
            type = MessageType.MOTIVATIONAL,
            title = "t",
            body = "b",
            timestamp = ts
        )
        val parsed = WebSocketMessageParser.parse(WebSocketMessageParser.toJson(original))

        assertNotNull(parsed)
        // Compare truncated to seconds (ISO_LOCAL_DATE_TIME has no sub-second precision here)
        assertEquals(ts.withNano(0), parsed!!.timestamp.withNano(0))
    }
}
