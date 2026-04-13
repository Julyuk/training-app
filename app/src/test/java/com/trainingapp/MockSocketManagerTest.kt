package com.trainingapp

import app.cash.turbine.test
import com.trainingapp.data.websocket.ConnectionState
import com.trainingapp.data.websocket.MessageType
import com.trainingapp.data.websocket.MockSocketManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for [MockSocketManager].
 *
 * All timing is controlled by the [TestScope] injected via [runTest] so
 * the suite runs without any real-clock delays.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MockSocketManagerTest {

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial connection state is Disconnected`() = runTest {
        val manager = MockSocketManager(scope = backgroundScope)
        assertEquals(ConnectionState.Disconnected, manager.connectionState.value)
    }

    // ── connect ───────────────────────────────────────────────────────────────

    @Test
    fun `connect transitions immediately to Connecting`() = runTest {
        val manager = MockSocketManager(scope = backgroundScope)
        manager.connect("ws://test")
        assertEquals(ConnectionState.Connecting, manager.connectionState.value)
    }

    @Test
    fun `connect transitions to Connected after handshake delay`() = runTest {
        val manager = MockSocketManager(scope = backgroundScope)
        manager.connect("ws://test")
        advanceTimeBy(600) // past the 500 ms handshake
        assertEquals(ConnectionState.Connected, manager.connectionState.value)
    }

    @Test
    fun `connect while already Connecting is idempotent`() = runTest {
        val manager = MockSocketManager(scope = backgroundScope)
        manager.connect("ws://test")
        manager.connect("ws://test") // second call must not change state
        assertEquals(ConnectionState.Connecting, manager.connectionState.value)
    }

    @Test
    fun `connect while already Connected is idempotent`() = runTest {
        val manager = MockSocketManager(scope = backgroundScope)
        manager.connect("ws://test")
        advanceTimeBy(600)
        assertEquals(ConnectionState.Connected, manager.connectionState.value)

        manager.connect("ws://test") // should not re-trigger handshake
        assertEquals(ConnectionState.Connected, manager.connectionState.value)
    }

    // ── disconnect ────────────────────────────────────────────────────────────

    @Test
    fun `disconnect transitions to Disconnected`() = runTest {
        val manager = MockSocketManager(scope = backgroundScope)
        manager.connect("ws://test")
        advanceTimeBy(600)
        assertEquals(ConnectionState.Connected, manager.connectionState.value)

        manager.disconnect()
        assertEquals(ConnectionState.Disconnected, manager.connectionState.value)
    }

    @Test
    fun `disconnect cancels simulation so no more messages arrive`() = runTest {
        val manager = MockSocketManager(
            scope = backgroundScope,
            messageIntervalRange = 100L..200L
        )
        manager.connect("ws://test")
        advanceTimeBy(600) // connected

        manager.disconnect()
        // After disconnect no messages should arrive regardless of time
        manager.messages.test {
            advanceTimeBy(1_000)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── send ──────────────────────────────────────────────────────────────────

    @Test
    fun `send while Disconnected emits no message`() = runTest {
        val manager = MockSocketManager(scope = backgroundScope)
        // State is Disconnected — send must be silent
        manager.messages.test {
            manager.send("hello")
            advanceTimeBy(100)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `send while Connected emits a SYSTEM message`() = runTest {
        val manager = MockSocketManager(
            scope = backgroundScope,
            messageIntervalRange = 60_000L..60_000L // suppress auto-messages
        )
        manager.connect("ws://test")
        advanceTimeBy(600)

        manager.messages.test {
            manager.send("ping")
            val msg = awaitItem()
            assertEquals(MessageType.SYSTEM, msg.type)
            assertEquals("ping", msg.body)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── auto-messages ─────────────────────────────────────────────────────────

    @Test
    fun `messages are emitted automatically after connection`() = runTest {
        val manager = MockSocketManager(
            scope = backgroundScope,
            messageIntervalRange = 100L..200L // fast interval for tests
        )
        manager.connect("ws://test")
        advanceTimeBy(600) // connected

        manager.messages.test {
            advanceTimeBy(300) // enough for at least one auto-message
            val msg = awaitItem()
            assertNotNull(msg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── simulateDisconnect / reconnect ────────────────────────────────────────

    @Test
    fun `simulateDisconnect transitions immediately to Reconnecting`() = runTest {
        val manager = MockSocketManager(scope = backgroundScope)
        manager.connect("ws://test")
        advanceTimeBy(600)
        assertEquals(ConnectionState.Connected, manager.connectionState.value)

        // simulateDisconnect sets Reconnecting synchronously before launching the retry coroutine
        manager.simulateDisconnect()
        assertEquals(ConnectionState.Reconnecting, manager.connectionState.value)
    }

    @Test
    fun `simulateDisconnect eventually returns to Connected`() = runTest {
        val manager = MockSocketManager(scope = backgroundScope)
        manager.connect("ws://test")
        advanceTimeBy(600) // Connected

        manager.simulateDisconnect()
        // 2_000 ms back-off + 500 ms handshake = 2_500 ms
        advanceTimeBy(3_000)
        assertEquals(ConnectionState.Connected, manager.connectionState.value)
    }

    @Test
    fun `messages resume after automatic reconnect`() = runTest {
        val manager = MockSocketManager(
            scope = backgroundScope,
            messageIntervalRange = 100L..200L
        )
        manager.connect("ws://test")
        advanceTimeBy(600)

        manager.simulateDisconnect()
        advanceTimeBy(3_000) // reconnect completes

        manager.messages.test {
            advanceTimeBy(300) // enough for one auto-message
            val msg = awaitItem()
            assertNotNull(msg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `disconnect during reconnect prevents state from returning to Connected`() = runTest {
        // BUG this test was written to verify: before the activeJob fix, calling
        // disconnect() while simulateDisconnect()'s coroutine was sleeping would
        // NOT cancel that coroutine — after 2_500 ms the state would drift back
        // to Connected even though the user explicitly disconnected.
        val manager = MockSocketManager(scope = backgroundScope)
        manager.connect("ws://test")
        advanceTimeBy(600) // Connected

        manager.simulateDisconnect()          // launches reconnect coroutine
        advanceTimeBy(500)                    // mid-reconnect back-off
        manager.disconnect()                  // must cancel the reconnect coroutine

        advanceTimeBy(5_000)                  // wait longer than reconnect would take
        assertEquals(ConnectionState.Disconnected, manager.connectionState.value)
    }
}
