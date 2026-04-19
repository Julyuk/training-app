package com.trainingapp

import app.cash.turbine.test
import com.trainingapp.data.biometric.AppLockManager
import com.trainingapp.data.biometric.AuthState
import com.trainingapp.data.biometric.BiometricType
import com.trainingapp.data.biometric.MockBiometricAuthManager
import com.trainingapp.data.local.SecurityStore
import com.trainingapp.ui.viewmodel.LockViewModel
import com.trainingapp.ui.viewmodel.SecuritySettingsViewModel
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the biometric authentication layer.
 *
 * All tests run on the JVM without a real device or sensor.
 * [MockBiometricAuthManager] simulates every scenario deterministically.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BiometricAuthTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── MockBiometricAuthManager ──────────────────────────────────────────────

    @Test
    fun `checkAvailability returns NONE when configured with NONE`() {
        val mock = MockBiometricAuthManager(sensorType = BiometricType.NONE)
        assertEquals(BiometricType.NONE, mock.checkAvailability())
    }

    @Test
    fun `checkAvailability returns FINGERPRINT when configured with FINGERPRINT`() {
        val mock = MockBiometricAuthManager(sensorType = BiometricType.FINGERPRINT)
        assertEquals(BiometricType.FINGERPRINT, mock.checkAvailability())
    }

    @Test
    fun `authenticate emits Authenticating then Success`() = runTest {
        val mock = MockBiometricAuthManager(
            sensorType = BiometricType.FINGERPRINT,
            authResult = AuthState.Success
        )

        mock.authState.test {
            assertEquals(AuthState.Idle, awaitItem()) // initial
            mock.authenticate(mockk(relaxed = true), "reason")
            assertEquals(AuthState.Authenticating, awaitItem())
            assertEquals(AuthState.Success, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authenticate emits Authenticating then Failed`() = runTest {
        val mock = MockBiometricAuthManager(
            sensorType = BiometricType.FINGERPRINT,
            authResult = AuthState.Failed("Помилка")
        )

        mock.authState.test {
            awaitItem() // Idle
            mock.authenticate(mockk(relaxed = true), "reason")
            assertEquals(AuthState.Authenticating, awaitItem())
            val result = awaitItem()
            assertTrue(result is AuthState.Failed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authenticate emits Cancelled on user dismissal`() = runTest {
        val mock = MockBiometricAuthManager(
            sensorType = BiometricType.FINGERPRINT,
            authResult = AuthState.Cancelled
        )

        mock.authState.test {
            awaitItem() // Idle
            mock.authenticate(mockk(relaxed = true), "reason")
            awaitItem() // Authenticating
            assertEquals(AuthState.Cancelled, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authenticate emits Unavailable when sensor absent`() = runTest {
        val mock = MockBiometricAuthManager(
            sensorType = BiometricType.NONE,
            authResult = AuthState.Unavailable
        )

        mock.authState.test {
            awaitItem() // Idle
            mock.authenticate(mockk(relaxed = true), "reason")
            awaitItem() // Authenticating
            assertEquals(AuthState.Unavailable, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isEnabledByUser returns the configured value`() {
        val mockDisabled = MockBiometricAuthManager(biometricEnabled = false)
        assertFalse(mockDisabled.isEnabledByUser())

        val mockEnabled = MockBiometricAuthManager(biometricEnabled = true)
        assertTrue(mockEnabled.isEnabledByUser())
    }

    @Test
    fun `resetState returns authState to Idle`() = runTest {
        val mock = MockBiometricAuthManager(authResult = AuthState.Success)
        mock.authenticate(mockk(relaxed = true), "reason")
        assertEquals(AuthState.Success, mock.authState.value)

        mock.resetState()
        assertEquals(AuthState.Idle, mock.authState.value)
    }

    // ── LockViewModel ─────────────────────────────────────────────────────────

    @Test
    fun `LockViewModel authState starts as Idle`() {
        val vm = LockViewModel(MockBiometricAuthManager())
        assertEquals(AuthState.Idle, vm.authState.value)
    }

    @Test
    fun `LockViewModel authState transitions to Authenticating then Success`() = runTest {
        val mock = MockBiometricAuthManager(authResult = AuthState.Success)
        val vm = LockViewModel(mock)

        vm.authState.test {
            assertEquals(AuthState.Idle, awaitItem())
            vm.authenticate(mockk(relaxed = true))
            assertEquals(AuthState.Authenticating, awaitItem())
            assertEquals(AuthState.Success, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `LockViewModel resetState returns to Idle`() = runTest {
        val mock = MockBiometricAuthManager(authResult = AuthState.Success)
        val vm = LockViewModel(mock)
        vm.authenticate(mockk(relaxed = true))
        assertEquals(AuthState.Success, vm.authState.value)

        vm.resetState()
        assertEquals(AuthState.Idle, vm.authState.value)
    }

    // ── SecuritySettingsViewModel ─────────────────────────────────────────────

    @Test
    fun `SecuritySettingsViewModel isBiometricEnabled reads initial value from store`() {
        val store = mockk<SecurityStore>()
        every { store.isBiometricEnabled() } returns true
        every { store.getAutoLockTimeoutSeconds() } returns 30

        val vm = SecuritySettingsViewModel(store, MockBiometricAuthManager())
        assertTrue(vm.isBiometricEnabled.value)
    }

    @Test
    fun `SecuritySettingsViewModel setBiometricEnabled persists and updates flow`() {
        val store = mockk<SecurityStore>()
        every { store.isBiometricEnabled() } returns false
        every { store.getAutoLockTimeoutSeconds() } returns 30
        every { store.setBiometricEnabled(any()) } just Runs

        val vm = SecuritySettingsViewModel(store, MockBiometricAuthManager())
        vm.setBiometricEnabled(true)

        verify { store.setBiometricEnabled(true) }
        assertTrue(vm.isBiometricEnabled.value)
    }

    @Test
    fun `SecuritySettingsViewModel setAutoLockTimeout persists and updates flow`() {
        val store = mockk<SecurityStore>()
        every { store.isBiometricEnabled() } returns false
        every { store.getAutoLockTimeoutSeconds() } returns 30
        every { store.setAutoLockTimeoutSeconds(any()) } just Runs

        val vm = SecuritySettingsViewModel(store, MockBiometricAuthManager())
        vm.setAutoLockTimeout(120)

        verify { store.setAutoLockTimeoutSeconds(120) }
        assertEquals(120, vm.autoLockTimeout.value)
    }

    // ── AppLockManager ────────────────────────────────────────────────────────

    @Test
    fun `AppLockManager shouldLock returns false when biometric disabled`() {
        val store = mockk<SecurityStore>()
        every { store.isBiometricEnabled() } returns false
        every { store.getAutoLockTimeoutSeconds() } returns 0

        val manager = AppLockManager(store)
        manager.onAppBackgrounded()
        assertFalse(manager.shouldLock())
    }

    @Test
    fun `AppLockManager shouldLock returns true after timeout elapsed`() {
        val store = mockk<SecurityStore>()
        every { store.isBiometricEnabled() } returns true
        every { store.getAutoLockTimeoutSeconds() } returns 0 // 0 s → lock immediately

        val manager = AppLockManager(store)
        manager.onAppBackgrounded()
        assertTrue(manager.shouldLock())
    }

    @Test
    fun `AppLockManager shouldLock returns false after onUnlocked`() {
        val store = mockk<SecurityStore>()
        every { store.isBiometricEnabled() } returns true
        every { store.getAutoLockTimeoutSeconds() } returns 0

        val manager = AppLockManager(store)
        manager.onAppBackgrounded()
        manager.onUnlocked()   // simulates successful biometric auth
        assertFalse(manager.shouldLock())
    }

    @Test
    fun `AppLockManager shouldLock returns true on cold start when biometric enabled`() {
        val store = mockk<SecurityStore>()
        every { store.isBiometricEnabled() } returns true
        every { store.getAutoLockTimeoutSeconds() } returns 30

        val manager = AppLockManager(store)
        // No onAppBackgrounded() called — simulates fresh process launch
        assertTrue(manager.shouldLock())
    }
}
