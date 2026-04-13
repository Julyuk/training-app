package com.trainingapp

import com.trainingapp.data.model.SampleChallenges
import com.trainingapp.data.model.Workout
import com.trainingapp.data.repository.WorkoutRepository
import com.trainingapp.data.websocket.ConnectionState
import com.trainingapp.data.websocket.MessageType
import com.trainingapp.data.websocket.SocketManager
import com.trainingapp.data.websocket.WebSocketMessage
import com.trainingapp.ui.viewmodel.ChallengesViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ChallengesViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var socketManager: SocketManager
    private lateinit var repository: WorkoutRepository
    private lateinit var messageFlow: MutableSharedFlow<WebSocketMessage>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        messageFlow = MutableSharedFlow(extraBufferCapacity = 64)
        socketManager = mockk(relaxed = true) {
            every { messages } returns messageFlow
            every { connectionState } returns MutableStateFlow(ConnectionState.Connected)
        }
        repository = mockk {
            every { getAllWorkouts() } returns flowOf(emptyList<Workout>())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = ChallengesViewModel(socketManager, repository)

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial challenges contain all sample challenge ids`() = runTest {
        val vm = buildViewModel()
        val ids = vm.challenges.value.map { it.id }
        assertEquals(SampleChallenges.all.map { it.id }, ids)
    }

    @Test
    fun `initial lastUpdate is null`() = runTest {
        val vm = buildViewModel()
        assertNull(vm.lastUpdate.value)
    }

    // ── lastUpdate reacts to WS events ────────────────────────────────────────

    @Test
    fun `challenge update message sets lastUpdate`() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val msg = WebSocketMessage(
            type = MessageType.CHALLENGE_UPDATE,
            title = "Виклик тижня",
            body = "Залишилось 2 дні!"
        )
        messageFlow.emit(msg)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(msg, vm.lastUpdate.value)
    }

    @Test
    fun `non-challenge messages do not affect lastUpdate`() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        messageFlow.emit(
            WebSocketMessage(
                type = MessageType.MOTIVATIONAL,
                title = "Мотивація",
                body = "Давай!"
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.lastUpdate.value)
    }

    @Test
    fun `only the latest challenge update is stored in lastUpdate`() = runTest {
        val vm = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        messageFlow.emit(
            WebSocketMessage(type = MessageType.CHALLENGE_UPDATE, title = "A", body = "First")
        )
        testDispatcher.scheduler.advanceUntilIdle()
        messageFlow.emit(
            WebSocketMessage(type = MessageType.CHALLENGE_UPDATE, title = "B", body = "Second")
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("B", vm.lastUpdate.value?.title)
    }

    // ── toggleJoin ────────────────────────────────────────────────────────────

    @Test
    fun `toggleJoin joins an unjoined challenge`() = runTest {
        val vm = buildViewModel()
        // challenges uses stateIn(WhileSubscribed) — upstream combine only runs
        // while there is an active subscriber. Subscribe via backgroundScope so
        // the subscription stays alive for the whole test, then advance the
        // dispatcher to let the initial combine emission land.
        backgroundScope.launch { vm.challenges.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val unjoined = SampleChallenges.all.first { !it.isJoined }
        assertFalse(vm.challenges.value.first { it.id == unjoined.id }.isJoined)

        vm.toggleJoin(unjoined.id)
        testDispatcher.scheduler.advanceUntilIdle() // let combine re-emit

        assertTrue(vm.challenges.value.first { it.id == unjoined.id }.isJoined)
    }

    @Test
    fun `toggleJoin leaves a joined challenge`() = runTest {
        val vm = buildViewModel()
        backgroundScope.launch { vm.challenges.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val joined = SampleChallenges.all.first { it.isJoined }
        assertTrue(vm.challenges.value.first { it.id == joined.id }.isJoined)

        vm.toggleJoin(joined.id)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.challenges.value.first { it.id == joined.id }.isJoined)
    }

    @Test
    fun `toggleJoin does not affect other challenges`() = runTest {
        val vm = buildViewModel()
        backgroundScope.launch { vm.challenges.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val target = SampleChallenges.all.first { !it.isJoined }
        val others = vm.challenges.value.filter { it.id != target.id }

        vm.toggleJoin(target.id)
        testDispatcher.scheduler.advanceUntilIdle()

        // All other challenges must remain unchanged
        val afterOthers = vm.challenges.value.filter { it.id != target.id }
        assertEquals(others, afterOthers)
    }
}
