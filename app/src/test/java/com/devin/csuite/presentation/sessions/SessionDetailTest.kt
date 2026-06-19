package com.devin.csuite.presentation.sessions

import com.devin.csuite.core.UiState
import com.devin.csuite.data.local.PreferencesManager
import com.devin.csuite.domain.model.InsightsResponse
import com.devin.csuite.domain.model.Session
import com.devin.csuite.domain.model.SessionPullRequest
import com.devin.csuite.domain.model.SessionsResponse
import com.devin.csuite.domain.repository.SessionsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionsRepository: SessionsRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var viewModel: SessionsViewModel

    private val testSession = Session(
        sessionId = "devin-abc123",
        title = "Fix login bug",
        status = "completed",
        createdAt = 1718784000000,
        updatedAt = 1718787600000,
        acusConsumed = 42.5,
        userEmail = "alice@test.com",
        origin = "slack",
        tags = listOf("bugfix", "auth"),
        pullRequests = listOf(
            SessionPullRequest(
                url = "https://github.com/org/repo/pull/123",
                title = "Fix login flow",
                state = "merged",
                number = 123,
                repo = "org/repo"
            )
        ),
        url = "https://app.devin.ai/sessions/devin-abc123",
        orgId = "org-123"
    )

    private val testInsights = InsightsResponse(
        insights = "This session resolved a critical authentication bug affecting 15% of users.",
        status = "completed",
        sessionId = "devin-abc123"
    )

    private val defaultSessionsResponse = SessionsResponse(
        sessions = listOf(testSession),
        totalCount = 1,
        nextCursor = null,
        hasMore = false
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionsRepository = mockk()
        preferencesManager = mockk()
        every { preferencesManager.refreshInterval } returns flowOf(0)
        coEvery {
            sessionsRepository.getSessions(
                first = any(),
                after = any(),
                status = any(),
                origin = any(),
                user = any(),
                tags = any(),
                createdAfter = any(),
                createdBefore = any()
            )
        } returns flowOf(Result.success(defaultSessionsResponse))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SessionsViewModel {
        return SessionsViewModel(sessionsRepository, preferencesManager)
    }

    // --- Session Detail Data Loading Tests ---

    @Test
    fun `loadSessionDetail sets Loading state initially`() = runTest {
        coEvery {
            sessionsRepository.getSessionDetail("devin-abc123")
        } returns flowOf(Result.success(testSession))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadSessionDetail("devin-abc123")
        // Before advancing, selectedSession should be Loading
        assertEquals(UiState.Loading, viewModel.uiState.value.selectedSession)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.selectedSession is UiState.Success)
    }

    @Test
    fun `loadSessionDetail success populates session data`() = runTest {
        coEvery {
            sessionsRepository.getSessionDetail("devin-abc123")
        } returns flowOf(Result.success(testSession))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadSessionDetail("devin-abc123")
        advanceUntilIdle()

        val state = viewModel.uiState.value.selectedSession
        assertTrue(state is UiState.Success)
        val session = (state as UiState.Success).data
        assertEquals("devin-abc123", session.sessionId)
        assertEquals("Fix login bug", session.title)
        assertEquals("completed", session.status)
        assertEquals(42.5, session.acusConsumed, 0.01)
        assertEquals("alice@test.com", session.userEmail)
        assertEquals("slack", session.origin)
        assertEquals(listOf("bugfix", "auth"), session.tags)
        assertEquals(1, session.pullRequests.size)
        assertEquals("Fix login flow", session.pullRequests[0].title)
    }

    @Test
    fun `loadSessionDetail error sets Error state`() = runTest {
        coEvery {
            sessionsRepository.getSessionDetail("devin-abc123")
        } returns flowOf(Result.failure(Exception("Session not found")))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadSessionDetail("devin-abc123")
        advanceUntilIdle()

        val state = viewModel.uiState.value.selectedSession
        assertTrue(state is UiState.Error)
        assertEquals("Session not found", (state as UiState.Error).message)
    }

    @Test
    fun `loadSessionDetail with network error shows appropriate message`() = runTest {
        coEvery {
            sessionsRepository.getSessionDetail("devin-xyz")
        } returns flowOf(Result.failure(java.io.IOException("No internet connection")))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadSessionDetail("devin-xyz")
        advanceUntilIdle()

        val state = viewModel.uiState.value.selectedSession
        assertTrue(state is UiState.Error)
        assertEquals("No internet connection", (state as UiState.Error).message)
    }

    // --- Insights Generation Tests ---

    @Test
    fun `generateInsights sets Loading state then Success`() = runTest {
        coEvery {
            sessionsRepository.generateInsights("devin-abc123")
        } returns flowOf(Result.success(testInsights))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.generateInsights("devin-abc123")
        // Before advancing, insights should be Loading
        assertEquals(UiState.Loading, viewModel.uiState.value.insightsState)

        advanceUntilIdle()

        val insightsState = viewModel.uiState.value.insightsState
        assertTrue(insightsState is UiState.Success)
        val insights = (insightsState as UiState.Success).data
        assertEquals("This session resolved a critical authentication bug affecting 15% of users.", insights.insights)
        assertEquals("completed", insights.status)
        assertEquals("devin-abc123", insights.sessionId)
    }

    @Test
    fun `generateInsights error sets Error state`() = runTest {
        coEvery {
            sessionsRepository.generateInsights("devin-abc123")
        } returns flowOf(Result.failure(Exception("Insights generation failed")))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.generateInsights("devin-abc123")
        advanceUntilIdle()

        val insightsState = viewModel.uiState.value.insightsState
        assertTrue(insightsState is UiState.Error)
        assertEquals("Insights generation failed", (insightsState as UiState.Error).message)
    }

    @Test
    fun `generateInsights with rate limit error shows message`() = runTest {
        coEvery {
            sessionsRepository.generateInsights("devin-abc123")
        } returns flowOf(Result.failure(Exception("Rate limited. Please try again later")))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.generateInsights("devin-abc123")
        advanceUntilIdle()

        val insightsState = viewModel.uiState.value.insightsState
        assertTrue(insightsState is UiState.Error)
        assertEquals("Rate limited. Please try again later", (insightsState as UiState.Error).message)
    }

    @Test
    fun `clearInsights resets insights state to null`() = runTest {
        coEvery {
            sessionsRepository.generateInsights("devin-abc123")
        } returns flowOf(Result.success(testInsights))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.generateInsights("devin-abc123")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.insightsState is UiState.Success)

        viewModel.clearInsights()

        assertNull(viewModel.uiState.value.insightsState)
    }

    @Test
    fun `generateInsights can be retried after error`() = runTest {
        coEvery {
            sessionsRepository.generateInsights("devin-abc123")
        } returns flowOf(Result.failure(Exception("Timeout")))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.generateInsights("devin-abc123")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.insightsState is UiState.Error)

        // Retry with success
        coEvery {
            sessionsRepository.generateInsights("devin-abc123")
        } returns flowOf(Result.success(testInsights))

        viewModel.generateInsights("devin-abc123")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.insightsState is UiState.Success)
    }

    @Test
    fun `insightsState is initially null before any generation requested`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.insightsState)
    }
}
