package com.devin.csuite.presentation.analytics

import app.cash.turbine.test
import com.devin.csuite.core.UiState
import com.devin.csuite.domain.model.AcuLimitsResponse
import com.devin.csuite.domain.model.DailyConsumption
import com.devin.csuite.domain.model.DailyConsumptionResponse
import com.devin.csuite.domain.model.MetricDataPoint
import com.devin.csuite.domain.model.PrMetricsResponse
import com.devin.csuite.domain.model.SearchMetricsResponse
import com.devin.csuite.domain.model.SessionMetricsResponse
import com.devin.csuite.domain.repository.MetricsRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var metricsRepository: MetricsRepository
    private lateinit var viewModel: AnalyticsViewModel

    private val testDailyConsumption = listOf(
        DailyConsumption(date = "2026-06-17", acusConsumed = 40.0),
        DailyConsumption(date = "2026-06-18", acusConsumed = 55.0),
        DailyConsumption(date = "2026-06-19", acusConsumed = 60.0)
    )

    private val testDailyConsumptionResponse = DailyConsumptionResponse(
        daily = testDailyConsumption,
        totalAcus = 155.0
    )

    private val testAcuLimitsResponse = AcuLimitsResponse(
        acuLimit = 1000.0,
        acusUsed = 500.0,
        remaining = 500.0
    )

    private val testSessionMetricsResponse = SessionMetricsResponse(
        totalSessions = 250,
        data = listOf(
            MetricDataPoint(date = "2026-06-18", value = 30.0),
            MetricDataPoint(date = "2026-06-19", value = 35.0)
        )
    )

    private val testPrMetricsResponse = PrMetricsResponse(
        totalPrs = 80,
        mergedPrs = 65,
        data = listOf(MetricDataPoint(date = "2026-06-19", value = 5.0))
    )

    private val testSearchMetricsResponse = SearchMetricsResponse(
        totalSearches = 120,
        data = listOf(MetricDataPoint(date = "2026-06-19", value = 15.0))
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        metricsRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupSuccessRepository() {
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.success(testDailyConsumptionResponse))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimitsResponse))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.success(testSessionMetricsResponse))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.success(testPrMetricsResponse))
        coEvery { metricsRepository.getSearchMetrics() } returns flowOf(Result.success(testSearchMetricsResponse))
    }

    // --- Section-Independent Loading Tests ---

    @Test
    fun `initial state has all sections loading`() = runTest {
        setupSuccessRepository()
        viewModel = AnalyticsViewModel(metricsRepository)

        val state = viewModel.uiState.value
        assertEquals(UiState.Loading, state.consumptionState)
        assertEquals(UiState.Loading, state.acuLimitsState)
        assertEquals(UiState.Loading, state.sessionMetricsState)
        assertEquals(UiState.Loading, state.prChartData)
        assertEquals(UiState.Loading, state.searchChartData)
    }

    @Test
    fun `successful load transitions all sections to Success`() = runTest {
        setupSuccessRepository()
        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.consumptionState is UiState.Success)
        assertTrue(state.acuLimitsState is UiState.Success)
        assertTrue(state.sessionMetricsState is UiState.Success)
        assertTrue(state.prChartData is UiState.Success)
        assertTrue(state.searchChartData is UiState.Success)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `consumption section loads independently from other sections`() = runTest {
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.success(testDailyConsumptionResponse))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimitsResponse))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.failure(Exception("Session error")))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.failure(Exception("PR error")))
        coEvery { metricsRepository.getSearchMetrics() } returns flowOf(Result.failure(Exception("Search error")))

        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.consumptionState is UiState.Success)
        assertTrue(state.acuLimitsState is UiState.Success)
        assertTrue(state.sessionMetricsState is UiState.Error)
        assertTrue(state.prChartData is UiState.Error)
        assertTrue(state.searchChartData is UiState.Error)
    }

    @Test
    fun `sessions section loads independently from consumption`() = runTest {
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.failure(Exception("Consumption error")))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.failure(Exception("Limits error")))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.success(testSessionMetricsResponse))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.success(testPrMetricsResponse))
        coEvery { metricsRepository.getSearchMetrics() } returns flowOf(Result.success(testSearchMetricsResponse))

        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.consumptionState is UiState.Error)
        assertTrue(state.acuLimitsState is UiState.Error)
        assertTrue(state.sessionMetricsState is UiState.Success)
        assertTrue(state.prChartData is UiState.Success)
        assertTrue(state.searchChartData is UiState.Success)
    }

    @Test
    fun `productivity section loads independently`() = runTest {
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.failure(Exception("error")))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.failure(Exception("error")))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.failure(Exception("error")))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.success(testPrMetricsResponse))
        coEvery { metricsRepository.getSearchMetrics() } returns flowOf(Result.success(testSearchMetricsResponse))

        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.consumptionState is UiState.Error)
        assertTrue(state.sessionMetricsState is UiState.Error)
        assertTrue(state.prChartData is UiState.Success)
        assertTrue(state.searchChartData is UiState.Success)
    }

    // --- Error Isolation Tests ---

    @Test
    fun `consumption failure does not affect session metrics`() = runTest {
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.failure(Exception("Consumption failed")))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimitsResponse))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.success(testSessionMetricsResponse))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.success(testPrMetricsResponse))
        coEvery { metricsRepository.getSearchMetrics() } returns flowOf(Result.success(testSearchMetricsResponse))

        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.consumptionState is UiState.Error)
        assertEquals("Consumption failed", (state.consumptionState as UiState.Error).message)
        assertTrue(state.sessionMetricsState is UiState.Success)
        assertEquals(250, (state.sessionMetricsState as UiState.Success).data.totalSessions)
    }

    @Test
    fun `session metrics failure does not affect consumption or productivity`() = runTest {
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.success(testDailyConsumptionResponse))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimitsResponse))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.failure(Exception("Session metrics down")))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.success(testPrMetricsResponse))
        coEvery { metricsRepository.getSearchMetrics() } returns flowOf(Result.success(testSearchMetricsResponse))

        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.consumptionState is UiState.Success)
        assertTrue(state.sessionMetricsState is UiState.Error)
        assertEquals("Session metrics down", (state.sessionMetricsState as UiState.Error).message)
        assertTrue(state.prChartData is UiState.Success)
        assertTrue(state.searchChartData is UiState.Success)
    }

    @Test
    fun `all sections fail independently with correct error messages`() = runTest {
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.failure(Exception("Consumption error")))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.failure(Exception("Limits error")))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.failure(Exception("Sessions error")))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.failure(Exception("PR error")))
        coEvery { metricsRepository.getSearchMetrics() } returns flowOf(Result.failure(Exception("Search error")))

        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Consumption error", (state.consumptionState as UiState.Error).message)
        assertEquals("Limits error", (state.acuLimitsState as UiState.Error).message)
        assertEquals("Sessions error", (state.sessionMetricsState as UiState.Error).message)
        assertEquals("PR error", (state.prChartData as UiState.Error).message)
        assertEquals("Search error", (state.searchChartData as UiState.Error).message)
    }

    @Test
    fun `null error message uses default fallback`() = runTest {
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.failure(Exception(null as String?)))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimitsResponse))
        coEvery { metricsRepository.getSessionMetrics() } returns flowOf(Result.success(testSessionMetricsResponse))
        coEvery { metricsRepository.getPrMetrics() } returns flowOf(Result.success(testPrMetricsResponse))
        coEvery { metricsRepository.getSearchMetrics() } returns flowOf(Result.success(testSearchMetricsResponse))

        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Failed to load consumption data", (state.consumptionState as UiState.Error).message)
    }

    // --- Filter Application Tests ---

    @Test
    fun `origin filter toggles correctly`() = runTest {
        setupSuccessRepository()
        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.selectedOrigins.isEmpty())

        viewModel.toggleOrigin("Slack")
        assertTrue(viewModel.uiState.value.selectedOrigins.contains("Slack"))

        viewModel.toggleOrigin("API")
        assertEquals(setOf("Slack", "API"), viewModel.uiState.value.selectedOrigins)

        viewModel.toggleOrigin("Slack")
        assertEquals(setOf("API"), viewModel.uiState.value.selectedOrigins)
    }

    @Test
    fun `clear origins resets selection`() = runTest {
        setupSuccessRepository()
        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.toggleOrigin("Webapp")
        viewModel.toggleOrigin("Slack")
        assertEquals(2, viewModel.uiState.value.selectedOrigins.size)

        viewModel.clearOrigins()
        assertTrue(viewModel.uiState.value.selectedOrigins.isEmpty())
    }

    @Test
    fun `toggle origin filter visibility`() = runTest {
        setupSuccessRepository()
        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showOriginFilter)

        viewModel.toggleOriginFilter()
        assertTrue(viewModel.uiState.value.showOriginFilter)

        viewModel.toggleOriginFilter()
        assertFalse(viewModel.uiState.value.showOriginFilter)
    }

    @Test
    fun `origin options contains expected platforms`() {
        val expected = listOf("Webapp", "Slack", "Teams", "API", "CLI", "Linear", "Jira", "Scheduled")
        assertEquals(expected, AnalyticsViewModel.ORIGIN_OPTIONS)
    }

    // --- Loading State Tests ---

    @Test
    fun `loading states per section during init`() = runTest {
        setupSuccessRepository()
        viewModel = AnalyticsViewModel(metricsRepository)

        val initialState = viewModel.uiState.value
        assertEquals(UiState.Loading, initialState.consumptionState)
        assertEquals(UiState.Loading, initialState.acuLimitsState)
        assertEquals(UiState.Loading, initialState.sessionMetricsState)
        assertEquals(UiState.Loading, initialState.prChartData)
        assertEquals(UiState.Loading, initialState.searchChartData)
        assertFalse(initialState.isRefreshing)
    }

    @Test
    fun `refresh sets isRefreshing to true then false`() = runTest {
        setupSuccessRepository()
        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val idleState = awaitItem()
            assertFalse(idleState.isRefreshing)

            viewModel.refresh()

            val refreshingState = awaitItem()
            assertTrue(refreshingState.isRefreshing)

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
        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        coVerify(exactly = 2) { metricsRepository.getDailyConsumption() }
        coVerify(exactly = 2) { metricsRepository.getAcuLimits() }
        coVerify(exactly = 2) { metricsRepository.getSessionMetrics() }
        coVerify(exactly = 2) { metricsRepository.getPrMetrics() }
        coVerify(exactly = 2) { metricsRepository.getSearchMetrics() }
    }

    // --- Tier Loading Order Tests ---

    @Test
    fun `tier 1 loads consumption and limits before tier 2`() = runTest {
        val loadOrder = mutableListOf<String>()

        coEvery { metricsRepository.getDailyConsumption() } answers {
            loadOrder.add("consumption")
            flowOf(Result.success(testDailyConsumptionResponse))
        }
        coEvery { metricsRepository.getAcuLimits() } answers {
            loadOrder.add("acuLimits")
            flowOf(Result.success(testAcuLimitsResponse))
        }
        coEvery { metricsRepository.getSessionMetrics() } answers {
            loadOrder.add("sessions")
            flowOf(Result.success(testSessionMetricsResponse))
        }
        coEvery { metricsRepository.getPrMetrics() } answers {
            loadOrder.add("prs")
            flowOf(Result.success(testPrMetricsResponse))
        }
        coEvery { metricsRepository.getSearchMetrics() } answers {
            loadOrder.add("searches")
            flowOf(Result.success(testSearchMetricsResponse))
        }

        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val tier1Items = setOf("consumption", "acuLimits")
        val tier2Items = setOf("sessions", "prs", "searches")

        val firstTier2Index = loadOrder.indexOfFirst { it in tier2Items }
        val lastTier1Index = loadOrder.indexOfLast { it in tier1Items }

        assertTrue(
            "Tier 1 should complete before Tier 2 starts. Order: $loadOrder",
            lastTier1Index < firstTier2Index
        )
    }

    // --- Data Parsing Tests ---

    @Test
    fun `consumption data correctly parsed`() = runTest {
        setupSuccessRepository()
        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val data = (viewModel.uiState.value.consumptionState as UiState.Success).data
        assertEquals(3, data.size)
        assertEquals("2026-06-17", data[0].date)
        assertEquals(40.0, data[0].acusConsumed, 0.001)
    }

    @Test
    fun `ACU limits correctly parsed`() = runTest {
        setupSuccessRepository()
        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val limits = (viewModel.uiState.value.acuLimitsState as UiState.Success).data
        assertEquals(1000.0, limits.acuLimit, 0.001)
        assertEquals(500.0, limits.acusUsed, 0.001)
        assertEquals(500.0, limits.remaining, 0.001)
    }

    @Test
    fun `session metrics correctly parsed`() = runTest {
        setupSuccessRepository()
        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val sessions = (viewModel.uiState.value.sessionMetricsState as UiState.Success).data
        assertEquals(250, sessions.totalSessions)
        assertEquals(2, sessions.data.size)
    }

    @Test
    fun `PR chart data correctly composed`() = runTest {
        setupSuccessRepository()
        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val prData = (viewModel.uiState.value.prChartData as UiState.Success).data
        assertEquals(80, prData.totalPrs)
        assertEquals(65, prData.mergedPrs)
        assertEquals(1, prData.data.size)
    }

    @Test
    fun `search chart data correctly parsed`() = runTest {
        setupSuccessRepository()
        viewModel = AnalyticsViewModel(metricsRepository)
        advanceUntilIdle()

        val searchData = (viewModel.uiState.value.searchChartData as UiState.Success).data
        assertEquals(1, searchData.size)
        assertEquals(15.0, searchData[0].value, 0.001)
    }

    // --- Default State Tests ---

    @Test
    fun `default selected origins is empty`() = runTest {
        setupSuccessRepository()
        viewModel = AnalyticsViewModel(metricsRepository)
        assertTrue(viewModel.uiState.value.selectedOrigins.isEmpty())
    }

    @Test
    fun `default origin filter is hidden`() = runTest {
        setupSuccessRepository()
        viewModel = AnalyticsViewModel(metricsRepository)
        assertFalse(viewModel.uiState.value.showOriginFilter)
    }
}
