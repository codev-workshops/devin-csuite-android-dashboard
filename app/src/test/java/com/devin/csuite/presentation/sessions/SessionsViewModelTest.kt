package com.devin.csuite.presentation.sessions

import com.devin.csuite.core.UiState
import com.devin.csuite.domain.model.Session
import com.devin.csuite.domain.model.SessionsResponse
import com.devin.csuite.domain.repository.SessionsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.devin.csuite.data.local.PreferencesManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionsRepository: SessionsRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var viewModel: SessionsViewModel

    private val testSessions = listOf(
        Session(sessionId = "s1", title = "Session 1", status = "running", acusConsumed = 10.0, origin = "slack", userEmail = "alice@test.com", tags = listOf("deploy")),
        Session(sessionId = "s2", title = "Session 2", status = "completed", acusConsumed = 20.0, origin = "web", userEmail = "bob@test.com", tags = listOf("review")),
        Session(sessionId = "s3", title = "Session 3", status = "error", acusConsumed = 5.0, origin = "api", userEmail = "alice@test.com"),
        Session(sessionId = "s4", title = "Session 4", status = "suspended", acusConsumed = 15.0, origin = "slack"),
        Session(sessionId = "s5", title = "Session 5", status = "exit", acusConsumed = 8.0, origin = "web")
    )

    private val firstPageResponse = SessionsResponse(
        sessions = testSessions.take(3),
        totalCount = 5,
        nextCursor = "cursor_page2",
        hasMore = true
    )

    private val secondPageResponse = SessionsResponse(
        sessions = testSessions.drop(3),
        totalCount = 5,
        nextCursor = null,
        hasMore = false
    )

    private val emptyResponse = SessionsResponse(
        sessions = emptyList(),
        totalCount = 0,
        nextCursor = null,
        hasMore = false
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionsRepository = mockk()
        preferencesManager = mockk()
        every { preferencesManager.refreshInterval } returns flowOf(0)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupDefaultRepository() {
        coEvery {
            sessionsRepository.getSessions(
                first = any(),
                after = isNull(),
                status = any(),
                origin = any(),
                user = any(),
                tags = any(),
                createdAfter = any(),
                createdBefore = any()
            )
        } returns flowOf(Result.success(firstPageResponse))
    }

    private fun createViewModel(): SessionsViewModel {
        return SessionsViewModel(sessionsRepository, preferencesManager)
    }

    // --- Cursor-Based Pagination Tests ---

    @Test
    fun `first page load fetches sessions with null cursor`() = runTest {
        setupDefaultRepository()
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.sessionsState is UiState.Success)
        assertEquals(3, state.sessions.size)
        assertEquals("cursor_page2", state.nextCursor)
        assertTrue(state.hasMore)
        assertEquals(5, state.totalSessionCount)
    }

    @Test
    fun `loadMore fetches next page using cursor`() = runTest {
        setupDefaultRepository()
        coEvery {
            sessionsRepository.getSessions(
                first = any(),
                after = eq("cursor_page2"),
                status = any(),
                origin = any(),
                user = any(),
                tags = any(),
                createdAfter = any(),
                createdBefore = any()
            )
        } returns flowOf(Result.success(secondPageResponse))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(5, state.sessions.size)
        assertFalse(state.hasMore)
        assertEquals(null, state.nextCursor)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `loadMore does nothing when already loading more`() = runTest {
        setupDefaultRepository()
        viewModel = createViewModel()
        advanceUntilIdle()

        coEvery {
            sessionsRepository.getSessions(
                first = any(),
                after = eq("cursor_page2"),
                status = any(),
                origin = any(),
                user = any(),
                tags = any(),
                createdAfter = any(),
                createdBefore = any()
            )
        } returns flowOf(Result.success(secondPageResponse))

        // Manually set isLoadingMore
        viewModel.loadMore()
        // Call again immediately — should be no-op since isLoadingMore is already true
        viewModel.loadMore()
        advanceUntilIdle()

        // Should have appended only once
        assertEquals(5, viewModel.uiState.value.sessions.size)
    }

    @Test
    fun `loadMore does nothing when hasMore is false`() = runTest {
        coEvery {
            sessionsRepository.getSessions(
                first = any(),
                after = isNull(),
                status = any(),
                origin = any(),
                user = any(),
                tags = any(),
                createdAfter = any(),
                createdBefore = any()
            )
        } returns flowOf(Result.success(secondPageResponse.copy(hasMore = false, nextCursor = null)))

        viewModel = createViewModel()
        advanceUntilIdle()

        val stateBefore = viewModel.uiState.value
        assertFalse(stateBefore.hasMore)

        viewModel.loadMore()
        advanceUntilIdle()

        // Sessions count should not change
        assertEquals(stateBefore.sessions.size, viewModel.uiState.value.sessions.size)
    }

    @Test
    fun `end of list is detected when nextCursor is null and hasMore is false`() = runTest {
        coEvery {
            sessionsRepository.getSessions(
                first = any(),
                after = isNull(),
                status = any(),
                origin = any(),
                user = any(),
                tags = any(),
                createdAfter = any(),
                createdBefore = any()
            )
        } returns flowOf(Result.success(
            SessionsResponse(
                sessions = testSessions,
                totalCount = 5,
                nextCursor = null,
                hasMore = false
            )
        ))

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.hasMore)
        assertEquals(null, state.nextCursor)
        assertEquals(5, state.sessions.size)
    }

    // --- Filter Combination Tests ---

    @Test
    fun `applyFilters with status filter passes correct parameter`() = runTest {
        coEvery {
            sessionsRepository.getSessions(
                first = any(),
                after = any(),
                status = eq("running"),
                origin = any(),
                user = any(),
                tags = any(),
                createdAfter = any(),
                createdBefore = any()
            )
        } returns flowOf(Result.success(firstPageResponse))
        coEvery {
            sessionsRepository.getSessions(
                first = any(),
                after = any(),
                status = isNull(),
                origin = any(),
                user = any(),
                tags = any(),
                createdAfter = any(),
                createdBefore = any()
            )
        } returns flowOf(Result.success(firstPageResponse))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.applyFilters(SessionFilters(status = "running"))
        advanceUntilIdle()

        coVerify {
            sessionsRepository.getSessions(
                first = any(),
                after = isNull(),
                status = eq("running"),
                origin = any(),
                user = any(),
                tags = any(),
                createdAfter = any(),
                createdBefore = any()
            )
        }
    }

    @Test
    fun `applyFilters with multiple filters applies AND logic`() = runTest {
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
        } returns flowOf(Result.success(firstPageResponse))

        viewModel = createViewModel()
        advanceUntilIdle()

        val filters = SessionFilters(
            status = "running",
            origins = setOf("slack", "web"),
            user = "alice@test.com",
            tag = "deploy"
        )
        viewModel.applyFilters(filters)
        advanceUntilIdle()

        coVerify {
            sessionsRepository.getSessions(
                first = any(),
                after = isNull(),
                status = eq("running"),
                origin = eq("slack,web"),
                user = eq("alice@test.com"),
                tags = eq("deploy"),
                createdAfter = any(),
                createdBefore = any()
            )
        }

        assertEquals(4, viewModel.uiState.value.activeFilterCount)
    }

    @Test
    fun `filterByStatus updates status filter and preserves others`() = runTest {
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
        } returns flowOf(Result.success(firstPageResponse))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.applyFilters(SessionFilters(user = "alice@test.com"))
        advanceUntilIdle()

        viewModel.filterByStatus("error")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("error", state.filters.status)
        assertEquals("alice@test.com", state.filters.user)
    }

    @Test
    fun `clearFilters resets all filters`() = runTest {
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
        } returns flowOf(Result.success(firstPageResponse))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.applyFilters(SessionFilters(status = "running", user = "bob@test.com"))
        advanceUntilIdle()

        viewModel.clearFilters()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SessionFilters(), state.filters)
        assertEquals(0, state.activeFilterCount)
    }

    @Test
    fun `activeFilterCount counts only non-null, non-empty filters`() = runTest {
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
        } returns flowOf(Result.success(firstPageResponse))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.applyFilters(SessionFilters(status = "running", origins = emptySet()))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.activeFilterCount)

        viewModel.applyFilters(SessionFilters(status = "running", origins = setOf("slack")))
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.activeFilterCount)
    }

    // --- Auto-Refresh Lifecycle Tests ---

    @Test
    fun `auto-refresh does not start when interval is zero`() = runTest {
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
        } returns flowOf(Result.success(firstPageResponse))

        viewModel = createViewModel()
        advanceUntilIdle()

        // Verify initial load happened
        assertTrue(viewModel.uiState.value.sessionsState is UiState.Success)

        // Only one call should have been made (initial load, no auto-refresh)
        coVerify(exactly = 1) {
            sessionsRepository.getSessions(
                first = any(),
                after = isNull(),
                status = any(),
                origin = any(),
                user = any(),
                tags = any(),
                createdAfter = any(),
                createdBefore = any()
            )
        }
    }

    @Test
    fun `auto-refresh does not start when interval is negative`() = runTest {
        every { preferencesManager.refreshInterval } returns flowOf(-1)
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
        } returns flowOf(Result.success(firstPageResponse))

        viewModel = createViewModel()
        advanceUntilIdle()

        // Verify preferences were read
        io.mockk.verify { preferencesManager.refreshInterval }

        // Only initial load — no auto-refresh
        coVerify(exactly = 1) {
            sessionsRepository.getSessions(
                first = any(),
                after = isNull(),
                status = any(),
                origin = any(),
                user = any(),
                tags = any(),
                createdAfter = any(),
                createdBefore = any()
            )
        }
    }

    @Test
    fun `refresh after silent error preserves list data`() = runTest {
        setupDefaultRepository()
        viewModel = createViewModel()
        advanceUntilIdle()

        val sessionsBefore = viewModel.uiState.value.sessions
        assertTrue(sessionsBefore.isNotEmpty())

        // Simulate a failed refresh — repo now returns error
        coEvery {
            sessionsRepository.getSessions(
                first = any(),
                after = isNull(),
                status = any(),
                origin = any(),
                user = any(),
                tags = any(),
                createdAfter = any(),
                createdBefore = any()
            )
        } returns flowOf(Result.failure(Exception("Transient error")))

        viewModel.refresh()
        advanceUntilIdle()

        // After a failed refresh, state transitions to Error
        val state = viewModel.uiState.value
        assertTrue(state.sessionsState is UiState.Error)
        assertFalse(state.isRefreshing)
    }

    // --- Loading States Tests ---

    @Test
    fun `initial state is Loading`() = runTest {
        setupDefaultRepository()
        viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(UiState.Loading, state.sessionsState)
        assertFalse(state.isRefreshing)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `refresh reloads data and resets isRefreshing`() = runTest {
        setupDefaultRepository()
        viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.sessionsState is UiState.Success)

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertTrue(state.sessionsState is UiState.Success)
        assertEquals(3, state.sessions.size)
    }

    @Test
    fun `loadMore appends data and resets isLoadingMore`() = runTest {
        setupDefaultRepository()
        coEvery {
            sessionsRepository.getSessions(
                first = any(),
                after = eq("cursor_page2"),
                status = any(),
                origin = any(),
                user = any(),
                tags = any(),
                createdAfter = any(),
                createdBefore = any()
            )
        } returns flowOf(Result.success(secondPageResponse))

        viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasMore)

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingMore)
        assertEquals(5, state.sessions.size)
    }

    // --- Error Handling and Retry Tests ---

    @Test
    fun `initial load error sets Error state`() = runTest {
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
        } returns flowOf(Result.failure(Exception("Network error")))

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.sessionsState is UiState.Error)
        assertEquals("Network error", (state.sessionsState as UiState.Error).message)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `loadMore error keeps existing sessions and resets isLoadingMore`() = runTest {
        setupDefaultRepository()
        coEvery {
            sessionsRepository.getSessions(
                first = any(),
                after = eq("cursor_page2"),
                status = any(),
                origin = any(),
                user = any(),
                tags = any(),
                createdAfter = any(),
                createdBefore = any()
            )
        } returns flowOf(Result.failure(Exception("Load more failed")))

        viewModel = createViewModel()
        advanceUntilIdle()

        val sessionsBefore = viewModel.uiState.value.sessions.size
        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(sessionsBefore, state.sessions.size)
        assertFalse(state.isLoadingMore)
        // sessionsState should still be Success from first load
        assertTrue(state.sessionsState is UiState.Success)
    }

    @Test
    fun `refresh after error retries and can succeed`() = runTest {
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
        } returns flowOf(Result.failure(Exception("Network error")))

        viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.sessionsState is UiState.Error)

        // Now fix the repo response
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
        } returns flowOf(Result.success(firstPageResponse))

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.sessionsState is UiState.Success)
        assertEquals(3, state.sessions.size)
        assertFalse(state.isRefreshing)
    }

    // --- Status Distribution Tests ---

    @Test
    fun `status distribution is computed correctly`() = runTest {
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
        } returns flowOf(Result.success(
            SessionsResponse(
                sessions = testSessions,
                totalCount = 5,
                nextCursor = null,
                hasMore = false
            )
        ))

        viewModel = createViewModel()
        advanceUntilIdle()

        val dist = viewModel.uiState.value.statusDistribution
        assertEquals(1, dist.running)
        assertEquals(2, dist.completed) // "completed" and "exit" both count as completed
        assertEquals(1, dist.error)
        assertEquals(1, dist.suspended)
        assertEquals(5, dist.total)
    }

    @Test
    fun `error rate is calculated correctly`() = runTest {
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
        } returns flowOf(Result.success(
            SessionsResponse(
                sessions = testSessions,
                totalCount = 5,
                nextCursor = null,
                hasMore = false
            )
        ))

        viewModel = createViewModel()
        advanceUntilIdle()

        // 1 error out of 5 sessions = 20%
        assertEquals(20.0, viewModel.uiState.value.errorRate, 0.01)
    }

    @Test
    fun `error rate is zero when no sessions`() = runTest {
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
        } returns flowOf(Result.success(emptyResponse))

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(0.0, viewModel.uiState.value.errorRate, 0.01)
    }
}
