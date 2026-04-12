package com.trainingapp.data.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Stub implementation of [SocketManager] that simulates a real WebSocket server.
 *
 * **Job tracking fix:** Every coroutine launched by [connect], [disconnect], and
 * [simulateDisconnect] is stored in [activeJob]. Calling [disconnect] at any
 * point — even during an ongoing reconnect — cancels that coroutine, so the
 * state machine can never "drift" back to [ConnectionState.Connected] after an
 * explicit disconnect.
 *
 * @param scope                The coroutine scope used for all internal work.
 * @param messageIntervalRange Delay range (ms) between auto-messages. Override
 *                             in tests to use a short interval without real waits.
 */
class MockSocketManager(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val messageIntervalRange: LongRange = 3_000L..5_000L
) : SocketManager {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // SharedFlow: hot, no replay — matches real WebSocket behaviour.
    private val _messages = MutableSharedFlow<WebSocketMessage>(extraBufferCapacity = 64)
    override val messages: Flow<WebSocketMessage> = _messages.asSharedFlow()

    // BUG FIX: track BOTH the connect/reconnect handshake coroutine AND the
    // simulation loop in separate fields so disconnect() can reliably cancel both.
    private var activeJob: Job? = null      // connect / reconnect handshake
    private var simulationJob: Job? = null  // periodic message generator

    // ── Public API ────────────────────────────────────────────────────────────

    override fun connect(url: String) {
        val current = _connectionState.value
        if (current == ConnectionState.Connected || current == ConnectionState.Connecting) return

        _connectionState.value = ConnectionState.Connecting
        activeJob?.cancel()
        activeJob = scope.launch {
            delay(500) // simulate TCP + WS handshake
            _connectionState.value = ConnectionState.Connected
            startSimulation()
        }
    }

    override fun disconnect() {
        // Cancel both the handshake/reconnect coroutine AND the simulation loop.
        // This prevents a previously-launched reconnect from transitioning back
        // to Connected after an explicit disconnect() call.
        activeJob?.cancel()
        activeJob = null
        simulationJob?.cancel()
        simulationJob = null
        _connectionState.value = ConnectionState.Disconnected
    }

    override fun send(message: String) {
        if (_connectionState.value != ConnectionState.Connected) return
        scope.launch {
            // Simulate the full round-trip: serialise the outbound message to JSON
            // and immediately parse it back — exactly what a real WebSocket layer
            // would do when the server echoes or responds.
            val outbound = WebSocketMessage(
                type = MessageType.SYSTEM,
                title = "Повідомлення отримано",
                body = message
            )
            val rawJson = WebSocketMessageParser.toJson(outbound)
            val parsed = WebSocketMessageParser.parse(rawJson) ?: outbound
            _messages.emit(parsed)
        }
    }

    /**
     * Simulate an unexpected server-side disconnect followed by one automatic
     * retry, demonstrating the required Reconnecting → Connecting → Connected
     * state cycle.
     *
     * The reconnect coroutine is stored in [activeJob], so a subsequent
     * [disconnect] call reliably cancels it.
     */
    fun simulateDisconnect() {
        if (_connectionState.value != ConnectionState.Connected) return
        simulationJob?.cancel()
        simulationJob = null
        _connectionState.value = ConnectionState.Reconnecting

        activeJob?.cancel()
        activeJob = scope.launch {
            delay(2_000) // back-off before retry
            _connectionState.value = ConnectionState.Connecting
            delay(500)   // handshake
            _connectionState.value = ConnectionState.Connected
            startSimulation()
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun startSimulation() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            while (isActive && _connectionState.value == ConnectionState.Connected) {
                val intervalMs = Random.nextLong(
                    messageIntervalRange.first,
                    messageIntervalRange.last + 1
                )
                delay(intervalMs)
                if (_connectionState.value == ConnectionState.Connected) {
                    // Copy with a fresh timestamp so every emission is a distinct
                    // object — prevents duplicate-key crashes in the LazyColumn when
                    // the same pool item is picked more than once.
                    _messages.emit(
                        MESSAGE_POOL[Random.nextInt(MESSAGE_POOL.size)]
                            .copy(timestamp = LocalDateTime.now())
                    )
                }
            }
        }
    }

    companion object {
        // BUG FIX: was rebuilt on every randomMessage() call — move to companion
        // object so the list is allocated once for the lifetime of the class.
        private val MESSAGE_POOL = listOf(
            WebSocketMessage(
                type = MessageType.MOTIVATIONAL,
                title = "Мотивація дня",
                body = "Ти вже на крок ближче до своєї мети! Не зупиняйся!"
            ),
            WebSocketMessage(
                type = MessageType.WORKOUT_COMPLETED,
                title = "Друг завершив тренування",
                body = "Олексій завершив силове тренування — 45 хв, 320 ккал"
            ),
            WebSocketMessage(
                type = MessageType.CHALLENGE_UPDATE,
                title = "Виклик тижня",
                body = "До завершення виклику «7 днів кардіо» залишилось 2 дні!"
            ),
            WebSocketMessage(
                type = MessageType.NEW_WORKOUT_SUGGESTION,
                title = "Нове тренування для тебе",
                body = "Спробуй HIIT-тренування: 20 хв, ефективно для спалення жиру",
                payload = mapOf("category" to "HIIT", "durationMinutes" to "20")
            ),
            WebSocketMessage(
                type = MessageType.CHALLENGE_UPDATE,
                title = "Результати виклику",
                body = "Ти на 3-му місці серед учасників виклику цього тижня!"
            ),
            WebSocketMessage(
                type = MessageType.WORKOUT_COMPLETED,
                title = "Твоє досягнення",
                body = "Вітаємо! Ти виконав 5 тренувань цього тижня — новий рекорд!"
            )
        )
    }
}
