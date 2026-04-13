package com.trainingapp

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.trainingapp.data.websocket.ConnectionState
import com.trainingapp.data.websocket.MessageType
import com.trainingapp.data.websocket.SocketManager
import com.trainingapp.data.websocket.WebSocketMessage
import com.trainingapp.ui.viewmodel.LiveFeedViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [LiveFeedViewModel].
 *
 * [SocketManager] is mocked with MockK so no real coroutines or
 * timers are involved — all delays are virtual via [StandardTestDispatcher].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveFeedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var socketManager: SocketManager
    private lateinit var messageFlow: MutableSharedFlow<WebSocketMessage>
    private lateinit var stateFlow: MutableStateFlow<ConnectionState>
    private lateinit var viewModel: LiveFeedViewModel

    private fun makeMessage(
        type: MessageType = MessageType.MOTIVATIONAL,
        title: String = "Title",
        body: String = "Body"
    ) = WebSocketMessage(type = type, title = title, body = body)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        messageFlow = MutableSharedFlow(extraBufferCapacity = 64)
        stateFlow = MutableStateFlow(ConnectionState.Disconnected)

        socketManager = mockk(relaxed = true) {
            every { messages } returns messageFlow
            every { connectionState } returns stateFlow
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() =
        LiveFeedViewModel(socketManager, wsUrl = "ws://test")

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial messages list is empty`() = runTest {
        viewModel = buildViewModel()
        assertEquals(emptyList<WebSocketMessage>(), viewModel.messages.value)
    }

    @Test
    fun `initial connectionState reflects socket manager state`() = runTest {
        stateFlow.value = ConnectionState.Disconnected
        viewModel = buildViewModel()
        assertEquals(ConnectionState.Disconnected, viewModel.connectionState.value)
    }

    // ── Message collection ────────────────────────────────────────────────────

    @Test
    fun `incoming socket message appears in messages StateFlow`() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.messages.test {
            awaitItem() // initial empty list
            messageFlow.emit(makeMessage(title = "Push Day"))
            val updated = awaitItem()
            assertEquals(1, updated.size)
            assertEquals("Push Day", updated.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple messages are stored newest-first`() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        messageFlow.emit(makeMessage(title = "First"))
        testDispatcher.scheduler.advanceUntilIdle()
        messageFlow.emit(makeMessage(title = "Second"))
        testDispatcher.scheduler.advanceUntilIdle()

        val messages = viewModel.messages.value
        assertEquals(2, messages.size)
        assertEquals("Second", messages[0].title) // newest first
        assertEquals("First", messages[1].title)
    }

    @Test
    fun `messages accumulate across multiple emissions`() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        repeat(5) { i ->
            messageFlow.emit(makeMessage(title = "Msg $i"))
            testDispatcher.scheduler.advanceUntilIdle()
        }

        assertEquals(5, viewModel.messages.value.size)
    }

    // ── clearMessages ─────────────────────────────────────────────────────────

    @Test
    fun `clearMessages empties the messages list`() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        messageFlow.emit(makeMessage())
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.messages.value.isNotEmpty())

        viewModel.clearMessages()
        assertEquals(emptyList<WebSocketMessage>(), viewModel.messages.value)
    }

    // ── connectionState ───────────────────────────────────────────────────────

    @Test
    fun `connectionState updates when socket manager state changes`() = runTest {
        viewModel = buildViewModel()

        viewModel.connectionState.test {
            awaitItem() // Disconnected
            stateFlow.value = ConnectionState.Connecting
            assertEquals(ConnectionState.Connecting, awaitItem())
            stateFlow.value = ConnectionState.Connected
            assertEquals(ConnectionState.Connected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── reconnect ─────────────────────────────────────────────────────────────

    @Test
    fun `reconnect delegates to socket manager connect`() = runTest {
        viewModel = buildViewModel()
        viewModel.reconnect()
        // connect() is called once in init + once in reconnect()
        verify(atLeast = 2) { socketManager.connect(any()) }
    }

    // ── init / onCleared ──────────────────────────────────────────────────────

    @Test
    fun `init calls socket manager connect`() = runTest {
        viewModel = buildViewModel()
        verify(exactly = 1) { socketManager.connect("ws://test") }
    }

    @Test
    fun `disconnect is called when ViewModel is cleared`() = runTest {
        // ViewModelStore.clear() is the proper way to trigger onCleared()
        // because onCleared() is protected and cannot be called from outside.
        val store = ViewModelStore()
        ViewModelProvider(store, LiveFeedViewModel.factory(socketManager))[LiveFeedViewModel::class.java]
        store.clear()
        verify(exactly = 1) { socketManager.disconnect() }
    }
}
