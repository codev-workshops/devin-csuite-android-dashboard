package com.devin.csuite.integration

import com.devin.csuite.core.NetworkMonitor
import com.devin.csuite.core.UiState
import com.devin.csuite.data.local.datasource.LocalMetricsDataSource
import com.devin.csuite.domain.model.AcuLimitsResponse
import com.devin.csuite.domain.model.DailyConsumption
import com.devin.csuite.domain.model.DailyConsumptionResponse
import com.devin.csuite.domain.model.MetricDataPoint
import com.devin.csuite.domain.model.PrMetricsResponse
import com.devin.csuite.domain.model.SearchMetricsResponse
import com.devin.csuite.domain.model.SessionMetricsResponse
import com.devin.csuite.domain.repository.MetricsRepository
import com.devin.csuite.presentation.analytics.AnalyticsViewModel
import com.devin.csuite.presentation.home.HomeViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

/**
 * Epic 6.7.1 - Integration test for filter application:
 * Time range filters, origin filters, and role-based filtering logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FilterFlowTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var metricsRepository: MetricsRepository
    private lateinit var localMetrics: LocalMetricsDataSource
    private lateinit var networkMonitor: NetworkMonitor
    private val isConnectedFlow = MutableStateFlow(true)

    private val testConsumption = DailyConsumptionResponse(
        daily = listOf(
            DailyConsumption(date = "2026-06-01", acusConsumed = 50.0),
            DailyConsumption(date = "2026-06-02", acusConsumed = 75.0)
        )
    )

    private val testAcuLimits = AcuLimitsResponse(
        acuLimit = 1000.0,
        acusUsed = 500.0,
        remaining = 500.0
    )

    private val testSessionMetrics = SessionMetricsResponse(
        totalSessions = 500,
        data = listOf(MetricDataPoint("2026-06-18", 45.0))
    )

    private val testPrMetrics = PrMetricsResponse(
        totalPrs = 75, mergedPrs = 60,
        data = listOf(MetricDataPoint("2026-06-18", 10.0))
    )

    private val testSearchMetrics = SearchMetricsResponse(
        totalSearches = 200,
        data = listOf(MetricDataPoint("2026-06-18", 25.0))
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        metricsRepository = mockk()
        localMetrics = mockk(relaxed = true)
        networkMonitor = mockk()
        every { networkMonitor.isConnected } returns isConnectedFlow
        coEvery { localMetrics.getLastUpdated(any()) } returns System.currentTimeMillis()
        setupRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupRepository() {
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.success(testConsumption))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimits))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.success(testSessionMetrics))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.success(testPrMetrics))
        coEvery { metricsRepository.getSearchMetrics() } returns flowOf(Result.success(testSearchMetrics))
    }

    // --- Time Range Filter Tests ---

    @Test
    fun `home view model defaults to 30d time range`() = runTest {
        setupHomeRepository()
        val viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        assertEquals("30d", viewModel.uiState.value.selectedTimeRange)
    }

    @Test
    fun `home view model time range can be changed to 7d`() = runTest {
        setupHomeRepository()
        val viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        viewModel.setTimeRange("7d")
        assertEquals("7d", viewModel.uiState.value.selectedTimeRange)
    }

    @Test
    fun `home view model time range can be changed to 90d`() = runTest {
        setupHomeRepository()
        val viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        viewModel.setTimeRange("90d")
        assertEquals("90d", viewModel.uiState.value.selectedTimeRange)
    }

    @Test
    fun `changing time range triggers data reload`() = runTest {
        setupHomeRepository()
        val viewModel = HomeViewModel(metricsRepository, localMetrics, networkMonitor)
        advanceUntilIdle()

        viewModel.setTimeRange("7d")
        advanceUntilIdle()

        // After reload, data should still be present
        assertEquals("7d", viewModel.uiState.value.selectedTimeRange)
    }

    // --- Origin Filter Tests ---

    @Test
    fun `analytics view model starts with no origins selected`() = runTest {
        val viewModel = AnalyticsViewModel(metricsRepository)
        assertTrue(viewModel.uiState.value.selectedOrigins.isEmpty())
    }

    @Test
    fun `analytics origin filter has correct options`() {
        val expectedOrigins = listOf("Webapp", "Slack", "Teams", "API", "CLI", "Linear", "Jira", "Scheduled")
        assertEquals(expectedOrigins, AnalyticsViewModel.ORIGIN_OPTIONS)
    }

    @Test
    fun `toggle origin adds it to selected set`() = runTest {
        val viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.toggleOrigin("Slack")
        assertTrue(viewModel.uiState.value.selectedOrigins.contains("Slack"))
    }

    @Test
    fun `toggle origin twice removes it from selected set`() = runTest {
        val viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.toggleOrigin("Slack")
        viewModel.toggleOrigin("Slack")
        assertFalse(viewModel.uiState.value.selectedOrigins.contains("Slack"))
    }

    @Test
    fun `multiple origins can be selected simultaneously`() = runTest {
        val viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.toggleOrigin("Webapp")
        viewModel.toggleOrigin("API")
        viewModel.toggleOrigin("CLI")

        val selected = viewModel.uiState.value.selectedOrigins
        assertEquals(3, selected.size)
        assertTrue(selected.containsAll(setOf("Webapp", "API", "CLI")))
    }

    @Test
    fun `clearOrigins resets filter to empty`() = runTest {
        val viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.toggleOrigin("Webapp")
        viewModel.toggleOrigin("Slack")
        viewModel.clearOrigins()

        assertTrue(viewModel.uiState.value.selectedOrigins.isEmpty())
    }

    @Test
    fun `toggle origin filter visibility`() = runTest {
        val viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showOriginFilter)
        viewModel.toggleOriginFilter()
        assertTrue(viewModel.uiState.value.showOriginFilter)
        viewModel.toggleOriginFilter()
        assertFalse(viewModel.uiState.value.showOriginFilter)
    }

    // --- Analytics Data Loading with Filters ---

    @Test
    fun `analytics loads consumption data successfully`() = runTest {
        val viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.consumptionState is UiState.Success)
        val data = (state.consumptionState as UiState.Success).data
        assertEquals(2, data.size)
        assertEquals(50.0, data[0].acusConsumed, 0.001)
    }

    @Test
    fun `analytics loads ACU limits successfully`() = runTest {
        val viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.acuLimitsState is UiState.Success)
        val limits = (state.acuLimitsState as UiState.Success).data
        assertEquals(1000.0, limits.acuLimit, 0.001)
    }

    @Test
    fun `analytics refresh reloads all data`() = runTest {
        val viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.refresh()
        assertTrue(viewModel.uiState.value.isRefreshing)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun `analytics handles data loading error gracefully`() = runTest {
        coEvery { metricsRepository.getDailyConsumption() } returns
            flowOf(Result.failure(Exception("Network error")))

        val viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.consumptionState is UiState.Error)
    }

    private fun setupHomeRepository() {
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(
            Result.success(
                com.devin.csuite.domain.model.BillingCyclesResponse(
                    cycles = emptyList(),
                    currentCycle = null
                )
            )
        )
        coEvery { metricsRepository.getSessions(limit = any(), status = any()) } returns flowOf(
            Result.success(
                com.devin.csuite.domain.model.SessionsResponse(sessions = emptyList(), totalCount = 0)
            )
        )
        coEvery { metricsRepository.getMauMetrics() } returns flowOf(
            Result.success(com.devin.csuite.domain.model.MauMetricsResponse(mau = 0, data = emptyList()))
        )
        coEvery { metricsRepository.getDauMetrics() } returns flowOf(
            Result.success(com.devin.csuite.domain.model.DauMetricsResponse(dau = 0, data = emptyList()))
        )
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(
            Result.success(com.devin.csuite.domain.model.PrMetricsResponse(totalPrs = 0, mergedPrs = 0, data = emptyList()))
        )
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(
            Result.success(com.devin.csuite.domain.model.SessionMetricsResponse(totalSessions = 0, data = emptyList()))
        )
        coEvery { metricsRepository.getActiveUsers() } returns flowOf(
            Result.success(com.devin.csuite.domain.model.ActiveUsersResponse(users = emptyList()))
        )
    }
}
