package com.trainingapp

import app.cash.turbine.test
import com.trainingapp.data.local.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the [NetworkMonitor] contract.
 *
 * The real [ConnectivityNetworkMonitor] depends on Android's ConnectivityManager
 * and cannot run on the JVM.  [FakeNetworkMonitor] implements the same interface
 * with a [MutableStateFlow] so we can verify that any code that *depends on*
 * [NetworkMonitor] reacts correctly to connectivity transitions — without needing
 * a device or Robolectric.
 */
class NetworkMonitorTest {

    // ── Fake implementation ───────────────────────────────────────────────────

    /**
     * Test double for [NetworkMonitor] backed by a [MutableStateFlow].
     * Starts offline (false) by default; call [setOnline] to simulate transitions.
     */
    private class FakeNetworkMonitor(initialOnline: Boolean = false) : NetworkMonitor {
        private val _isOnline = MutableStateFlow(initialOnline)
        override val isOnline: Flow<Boolean> = _isOnline.asStateFlow()

        fun setOnline(value: Boolean) { _isOnline.value = value }
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `isOnline emits false when starting offline`() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)

        monitor.isOnline.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isOnline emits true when starting online`() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)

        monitor.isOnline.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Connectivity transitions ──────────────────────────────────────────────

    @Test
    fun `isOnline emits true when transitioning from offline to online`() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)

        monitor.isOnline.test {
            assertFalse(awaitItem())   // initial: offline
            monitor.setOnline(true)
            assertTrue(awaitItem())   // transitioned: online
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isOnline emits false when transitioning from online to offline`() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)

        monitor.isOnline.test {
            assertTrue(awaitItem())    // initial: online
            monitor.setOnline(false)
            assertFalse(awaitItem())  // transitioned: offline
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isOnline emits multiple transitions in order`() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)

        monitor.isOnline.test {
            assertFalse(awaitItem())  // offline

            monitor.setOnline(true)
            assertTrue(awaitItem())   // online

            monitor.setOnline(false)
            assertFalse(awaitItem()) // offline again

            monitor.setOnline(true)
            assertTrue(awaitItem())   // online again

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Duplicate suppression ─────────────────────────────────────────────────

    @Test
    fun `isOnline does not emit when value stays the same`() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = true)

        monitor.isOnline.test {
            assertTrue(awaitItem())   // initial: online
            // Setting same value should produce no additional emission
            monitor.setOnline(true)
            // No second item — StateFlow deduplicates equal values
            // Verify only one item was available
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Value after transition ────────────────────────────────────────────────

    @Test
    fun `value reflects most recent transition`() = runTest {
        val monitor = FakeNetworkMonitor(initialOnline = false)

        monitor.setOnline(true)
        monitor.setOnline(false)
        monitor.setOnline(true)

        // Collect current value
        monitor.isOnline.test {
            val current = awaitItem()
            assertEquals(true, current)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
