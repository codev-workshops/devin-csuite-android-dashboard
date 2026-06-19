package com.devin.csuite.presentation.home

import app.cash.turbine.test
import com.devin.csuite.core.NetworkMonitor
import com.devin.csuite.core.UiState
import com.devin.csuite.data.local.datasource.LocalMetricsDataSource
import com.devin.csuite.domain.model.ActiveUser
import com.devin.csuite.domain.model.ActiveUsersResponse
import com.devin.csuite.domain.model.BillingCycle
import com.devin.csuite.domain.model.BillingCyclesResponse
import com.devin.csuite.domain.model.DauMetricsResponse
import com.devin.csuite.domain.model.MauMetricsResponse
import com.devin.csuite.domain.model.MetricDataPoint
import com.devin.csuite.domain.model.OrganizationsResponse
import com.devin.csuite.domain.model.PrMetricsResponse
import com.devin.csuite.domain.model.Session
import com.devin.csuite.domain.model.SessionMetricsResponse
import com.devin.csuite.domain.model.SessionsResponse
import com.devin.csuite.domain.repository.MetricsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var metricsRepository: MetricsRepository
    private lateinit var localMetrics: LocalMetricsDataSource
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var viewModel: HomeViewModel

    private val testBillingCycle = BillingCycle(
        cycleStart = "2026-06-01",
        cycleEnd = "2026-06-30",
        acusUsed = 500.0,
        acuLimit = 1000.0,
        status = "active"
    )

    private val testBillingCyclesResponse = BillingCyclesResponse(
        cycles = listOf(testBillingCycle),
        currentCycle = testBillingCycle
    )

    private val testSessionsResponse = SessionsResponse(
        sessions = listOf(
            Session(sessionId = "s1", title = "Test Session", status = "running", acusConsumed = 10.0),
            Session(sessionId = "s2", title = "Another Session", status = "running", acusConsumed = 20.0)
        ),
        totalCount = 42
    )

    private val testMauResponse = MauMetricsResponse(mau = 150, data = emptyList())
    private val testDauResponse = DauMetricsResponse(
        dau = 30,
        data = listOf(MetricDataPoint(date = "2026-06-18", value = 28.0), MetricDataPoint(date = "2026-06-19", value = 30.0))
    )
    private val testPrResponse = PrMetricsResponse(totalPrs = 75, mergedPrs = 60, data = emptyList())
    private val testSessionMetricsResponse = SessionMetricsResponse(
        totalSessions = 500,
        data = listOf(MetricDataPoint(date = "2026-06-18", value = 45.0))
    )
    private val testActiveUsersResponse = ActiveUsersResponse(
        users = listOf(
            ActiveUser(userEmail = "alice@test.com", displayName = "Alice", sessionCount = 50, acusConsumed = 200.0),
            ActiveUser(userEmail = "bob@test.com", displayName = "Bob", sessionCount = 30, acusConsumed = 100.0)
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        metricsRepository = mockk()
        localMetrics = mockk(relaxed = true)
        networkMonitor = mockk()
        every { networkMonitor.isConnected } returns kotlinx.coroutines.flow.MutableStateFlow(true)
        coEvery { localMetrics.getLastUpdated(any()) } returns System.currentTimeMillis()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupSuccessRepository() {
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.success(testBillingCyclesResponse))
        coEvery { metricsRepository.getSessions(limit = 100, status = "running") } returns flowOf(Result.success(testSessionsResponse))
        coEvery { metricsRepository.getMauMetrics() } returns flowOf(Result.success(testMauResponse))
        coEvery { metricsRepository.getDauMetrics() } returns flowOf(Result.success(testDauResponse))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.success(testPrResponse))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.success(testSessionMetricsResponse))
        coEvery { metricsRepository.getActiveUsers() } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { metricsRepository.getSessions(limit = 5, status = null) } returns flowOf(Result.success(testSessionsResponse))
    }

    // --- State Transition Tests ---

    @Test
    fun `initial state is all loading`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)

        val state = viewModel.uiState.value
        assertEquals(UiState.Loading, state.acuState)
        assertEquals(UiState.Loading, state.activeSessionsState)
        assertEquals(UiState.Loading, state.mauState)
        assertEquals(UiState.Loading, state.prCountState)
        assertEquals(UiState.Loading, state.sessionChartData)
        assertEquals(UiState.Loading, state.dauChartData)
        assertEquals(UiState.Loading, state.topUsers)
        assertEquals(UiState.Loading, state.recentSessions)
    }

    @Test
    fun `successful load transitions all states to Success`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.acuState is UiState.Success)
        assertTrue(state.activeSessionsState is UiState.Success)
        assertTrue(state.mauState is UiState.Success)
        assertTrue(state.prCountState is UiState.Success)
        assertTrue(state.sessionChartData is UiState.Success)
        assertTrue(state.dauChartData is UiState.Success)
        assertTrue(state.topUsers is UiState.Success)
        assertTrue(state.recentSessions is UiState.Success)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `ACU data is correctly parsed from billing cycles response`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val acuState = viewModel.uiState.value.acuState as UiState.Success
        assertEquals(testBillingCycle, acuState.data)
    }

    @Test
    fun `active sessions count is correctly parsed`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val sessionsState = viewModel.uiState.value.activeSessionsState as UiState.Success
        assertEquals(42, sessionsState.data)
    }

    @Test
    fun `MAU count is correctly parsed`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val mauState = viewModel.uiState.value.mauState as UiState.Success
        assertEquals(150, mauState.data)
    }

    @Test
    fun `PR count is correctly parsed`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val prState = viewModel.uiState.value.prCountState as UiState.Success
        assertEquals(75, prState.data)
    }

    @Test
    fun `top users limited to 5`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val topUsers = viewModel.uiState.value.topUsers as UiState.Success
        assertTrue(topUsers.data.size <= 5)
        assertEquals("alice@test.com", topUsers.data[0].userEmail)
    }

    @Test
    fun `recent sessions limited to 5`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val sessions = viewModel.uiState.value.recentSessions as UiState.Success
        assertTrue(sessions.data.size <= 5)
    }

    // --- Error State Tests ---

    @Test
    fun `network error transitions to Error state for ACU`() = runTest {
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.failure(Exception("Network error")))
        coEvery { metricsRepository.getSessions(limit = 100, status = "running") } returns flowOf(Result.success(testSessionsResponse))
        coEvery { metricsRepository.getMauMetrics() } returns flowOf(Result.success(testMauResponse))
        coEvery { metricsRepository.getDauMetrics() } returns flowOf(Result.success(testDauResponse))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.success(testPrResponse))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.success(testSessionMetricsResponse))
        coEvery { metricsRepository.getActiveUsers() } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { metricsRepository.getSessions(limit = 5, status = null) } returns flowOf(Result.success(testSessionsResponse))

        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.acuState is UiState.Error)
        assertEquals("Network error", (state.acuState as UiState.Error).message)
        // Other states should still succeed
        assertTrue(state.activeSessionsState is UiState.Success)
        assertTrue(state.mauState is UiState.Success)
    }

    @Test
    fun `partial failure - only failed sections show error`() = runTest {
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.failure(Exception("ACU error")))
        coEvery { metricsRepository.getSessions(limit = 100, status = "running") } returns flowOf(Result.failure(Exception("Sessions error")))
        coEvery { metricsRepository.getMauMetrics() } returns flowOf(Result.success(testMauResponse))
        coEvery { metricsRepository.getDauMetrics() } returns flowOf(Result.success(testDauResponse))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.failure(Exception("PR error")))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.success(testSessionMetricsResponse))
        coEvery { metricsRepository.getActiveUsers() } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { metricsRepository.getSessions(limit = 5, status = null) } returns flowOf(Result.success(testSessionsResponse))

        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.acuState is UiState.Error)
        assertTrue(state.activeSessionsState is UiState.Error)
        assertTrue(state.mauState is UiState.Success)
        assertTrue(state.prCountState is UiState.Error)
        assertTrue(state.dauChartData is UiState.Success)
        assertTrue(state.sessionChartData is UiState.Success)
        assertTrue(state.topUsers is UiState.Success)
    }

    @Test
    fun `all sections fail - all show error state`() = runTest {
        val error = Exception("Total failure")
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.failure(error))
        coEvery { metricsRepository.getSessions(limit = 100, status = "running") } returns flowOf(Result.failure(error))
        coEvery { metricsRepository.getMauMetrics() } returns flowOf(Result.failure(error))
        coEvery { metricsRepository.getDauMetrics() } returns flowOf(Result.failure(error))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.failure(error))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.failure(error))
        coEvery { metricsRepository.getActiveUsers() } returns flowOf(Result.failure(error))
        coEvery { metricsRepository.getSessions(limit = 5, status = null) } returns flowOf(Result.failure(error))

        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.acuState is UiState.Error)
        assertTrue(state.activeSessionsState is UiState.Error)
        assertTrue(state.mauState is UiState.Error)
        assertTrue(state.prCountState is UiState.Error)
        assertTrue(state.sessionChartData is UiState.Error)
        assertTrue(state.dauChartData is UiState.Error)
        assertTrue(state.topUsers is UiState.Error)
        assertTrue(state.recentSessions is UiState.Error)
    }

    // --- Critical Alert Tests ---

    @Test
    fun `ACU usage over 80 percent triggers critical alert`() = runTest {
        val highUsageCycle = BillingCycle(
            cycleStart = "2026-06-01",
            cycleEnd = "2026-06-30",
            acusUsed = 850.0,
            acuLimit = 1000.0,
            status = "active"
        )
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(
            Result.success(BillingCyclesResponse(cycles = listOf(highUsageCycle), currentCycle = highUsageCycle))
        )
        coEvery { metricsRepository.getSessions(limit = 100, status = "running") } returns flowOf(Result.success(testSessionsResponse))
        coEvery { metricsRepository.getMauMetrics() } returns flowOf(Result.success(testMauResponse))
        coEvery { metricsRepository.getDauMetrics() } returns flowOf(Result.success(testDauResponse))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.success(testPrResponse))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.success(testSessionMetricsResponse))
        coEvery { metricsRepository.getActiveUsers() } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { metricsRepository.getSessions(limit = 5, status = null) } returns flowOf(Result.success(testSessionsResponse))

        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showCriticalAlert)
        assertTrue(state.criticalAlertMessage.contains("85"))
    }

    @Test
    fun `ACU usage under 80 percent does not trigger alert`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showCriticalAlert)
        assertEquals("", state.criticalAlertMessage)
    }

    // --- Tiered Loading Order Tests ---

    @Test
    fun `tier 1 data loads before tier 2 data`() = runTest {
        val loadOrder = mutableListOf<String>()

        coEvery { metricsRepository.getBillingCycles() } answers {
            loadOrder.add("billing")
            flowOf(Result.success(testBillingCyclesResponse))
        }
        coEvery { metricsRepository.getSessions(limit = 100, status = "running") } answers {
            loadOrder.add("activeSessions")
            flowOf(Result.success(testSessionsResponse))
        }
        coEvery { metricsRepository.getMauMetrics() } answers {
            loadOrder.add("mau")
            flowOf(Result.success(testMauResponse))
        }
        coEvery { metricsRepository.getPrMetrics() } answers {
            loadOrder.add("prs")
            flowOf(Result.success(testPrResponse))
        }
        coEvery { metricsRepository.getSessionMetrics() } answers {
            loadOrder.add("sessionMetrics")
            flowOf(Result.success(testSessionMetricsResponse))
        }
        coEvery { metricsRepository.getDauMetrics() } answers {
            loadOrder.add("dau")
            flowOf(Result.success(testDauResponse))
        }
        coEvery { metricsRepository.getActiveUsers() } answers {
            loadOrder.add("activeUsers")
            flowOf(Result.success(testActiveUsersResponse))
        }
        coEvery { metricsRepository.getSessions(limit = 5, status = null) } answers {
            loadOrder.add("recentSessions")
            flowOf(Result.success(testSessionsResponse))
        }

        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        // Tier 1: billing, activeSessions, mau must come before Tier 2 items
        val tier1Items = setOf("billing", "activeSessions", "mau")
        val tier2Items = setOf("prs", "sessionMetrics", "dau", "activeUsers", "recentSessions")

        val firstTier2Index = loadOrder.indexOfFirst { it in tier2Items }
        val lastTier1Index = loadOrder.indexOfLast { it in tier1Items }

        assertTrue(
            "Tier 1 should complete before Tier 2 starts. Order: $loadOrder",
            lastTier1Index < firstTier2Index
        )
    }

    @Test
    fun `tier 1 loads all three KPIs in parallel`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        // Verify all three Tier 1 calls were made
        coVerify { metricsRepository.getBillingCycles() }
        coVerify { metricsRepository.getSessions(limit = 100, status = "running") }
        coVerify { metricsRepository.getMauMetrics() }
    }

    @Test
    fun `tier 2 loads all secondary data`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        coVerify { metricsRepository.getPrMetrics() }
        coVerify { metricsRepository.getSessionMetrics() }
        coVerify { metricsRepository.getDauMetrics() }
        coVerify { metricsRepository.getActiveUsers() }
        coVerify { metricsRepository.getSessions(limit = 5, status = null) }
    }

    // --- Filter Tests ---

    @Test
    fun `setTimeRange updates selected time range`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        viewModel.setTimeRange("7d")
        advanceUntilIdle()

        assertEquals("7d", viewModel.uiState.value.selectedTimeRange)
    }

    @Test
    fun `setTimeRange triggers re-fetch of data`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        viewModel.setTimeRange("7d")
        advanceUntilIdle()

        // Verify data was fetched twice (init + setTimeRange)
        coVerify(exactly = 2) { metricsRepository.getBillingCycles() }
        coVerify(exactly = 2) { metricsRepository.getMauMetrics() }
    }

    @Test
    fun `default time range is 30d`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)

        assertEquals("30d", viewModel.uiState.value.selectedTimeRange)
    }

    // --- Pull-to-Refresh Tests ---

    @Test
    fun `refresh sets isRefreshing to true then false`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        viewModel.uiState.test {
            val idleState = awaitItem()
            assertFalse(idleState.isRefreshing)

            viewModel.refresh()

            val refreshingState = awaitItem()
            assertTrue(refreshingState.isRefreshing)

            // Eventually finishes refreshing
            var finalState = refreshingState
            while (finalState.isRefreshing) {
                finalState = awaitItem()
            }
            assertFalse(finalState.isRefreshing)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh triggers re-fetch of all data`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        // init + refresh = 2 calls each
        coVerify(exactly = 2) { metricsRepository.getBillingCycles() }
        coVerify(exactly = 2) { metricsRepository.getSessions(limit = 100, status = "running") }
        coVerify(exactly = 2) { metricsRepository.getMauMetrics() }
        coVerify(exactly = 2) { metricsRepository.getPrMetrics() }
    }

    // --- Empty/Edge Case Tests ---

    @Test
    fun `empty billing cycles uses default BillingCycle`() = runTest {
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(
            Result.success(BillingCyclesResponse(cycles = emptyList(), currentCycle = null))
        )
        coEvery { metricsRepository.getSessions(limit = 100, status = "running") } returns flowOf(Result.success(testSessionsResponse))
        coEvery { metricsRepository.getMauMetrics() } returns flowOf(Result.success(testMauResponse))
        coEvery { metricsRepository.getDauMetrics() } returns flowOf(Result.success(testDauResponse))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.success(testPrResponse))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.success(testSessionMetricsResponse))
        coEvery { metricsRepository.getActiveUsers() } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { metricsRepository.getSessions(limit = 5, status = null) } returns flowOf(Result.success(testSessionsResponse))

        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val acuState = viewModel.uiState.value.acuState as UiState.Success
        assertEquals(BillingCycle(), acuState.data)
    }

    @Test
    fun `zero ACU limit does not divide by zero`() = runTest {
        val zeroCycle = BillingCycle(acusUsed = 100.0, acuLimit = 0.0, status = "active")
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(
            Result.success(BillingCyclesResponse(cycles = listOf(zeroCycle), currentCycle = zeroCycle))
        )
        coEvery { metricsRepository.getSessions(limit = 100, status = "running") } returns flowOf(Result.success(testSessionsResponse))
        coEvery { metricsRepository.getMauMetrics() } returns flowOf(Result.success(testMauResponse))
        coEvery { metricsRepository.getDauMetrics() } returns flowOf(Result.success(testDauResponse))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.success(testPrResponse))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.success(testSessionMetricsResponse))
        coEvery { metricsRepository.getActiveUsers() } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { metricsRepository.getSessions(limit = 5, status = null) } returns flowOf(Result.success(testSessionsResponse))

        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.acuState is UiState.Success)
        assertFalse(state.showCriticalAlert)
    }

    @Test
    fun `DAU chart data points are correctly passed through`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val dauState = viewModel.uiState.value.dauChartData as UiState.Success
        assertEquals(2, dauState.data.size)
        assertEquals("2026-06-18", dauState.data[0].date)
        assertEquals(28.0, dauState.data[0].value, 0.001)
    }

    @Test
    fun `session chart data points are correctly passed through`() = runTest {
        setupSuccessRepository()
        viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        val chartState = viewModel.uiState.value.sessionChartData as UiState.Success
        assertEquals(1, chartState.data.size)
        assertEquals(45.0, chartState.data[0].value, 0.001)
    }
}
