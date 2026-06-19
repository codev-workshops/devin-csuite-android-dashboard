package com.devin.csuite.presentation.billing

import app.cash.turbine.test
import com.devin.csuite.core.UiState
import com.devin.csuite.data.remote.ApiException
import com.devin.csuite.domain.model.AcuLimitsResponse
import com.devin.csuite.domain.model.BillingCycle
import com.devin.csuite.domain.model.BillingCyclesResponse
import com.devin.csuite.domain.model.DailyConsumption
import com.devin.csuite.domain.model.DailyConsumptionResponse
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var metricsRepository: MetricsRepository
    private lateinit var viewModel: BillingViewModel

    private val testBillingCycle = BillingCycle(
        cycleStart = "2026-06-01T00:00:00Z",
        cycleEnd = "2026-06-30T00:00:00Z",
        acusUsed = 500.0,
        acuLimit = 1000.0,
        status = "active"
    )

    private val testBillingCyclesResponse = BillingCyclesResponse(
        cycles = listOf(testBillingCycle),
        currentCycle = testBillingCycle
    )

    private val testDailyConsumption = listOf(
        DailyConsumption(date = "2026-06-13", acusConsumed = 30.0),
        DailyConsumption(date = "2026-06-14", acusConsumed = 35.0),
        DailyConsumption(date = "2026-06-15", acusConsumed = 40.0),
        DailyConsumption(date = "2026-06-16", acusConsumed = 45.0),
        DailyConsumption(date = "2026-06-17", acusConsumed = 50.0),
        DailyConsumption(date = "2026-06-18", acusConsumed = 55.0),
        DailyConsumption(date = "2026-06-19", acusConsumed = 60.0)
    )

    private val testDailyConsumptionResponse = DailyConsumptionResponse(
        daily = testDailyConsumption,
        totalAcus = 315.0
    )

    private val testAcuLimitsResponse = AcuLimitsResponse(
        acuLimit = 1000.0,
        acusUsed = 500.0,
        remaining = 500.0
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
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.success(testBillingCyclesResponse))
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.success(testDailyConsumptionResponse))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimitsResponse))
    }

    // --- Projection Computation Tests ---

    @Test
    fun `projection computed correctly - dailyAvg times remainingDays plus currentUsed`() = runTest {
        setupSuccessRepository()
        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        val projection = viewModel.uiState.value.projection
        assertNotNull(projection)
        projection!!

        val expectedDailyAvg = testDailyConsumption.takeLast(7).map { it.acusConsumed }.average()
        assertEquals(expectedDailyAvg, projection.dailyAvg, 0.001)
        assertEquals(500.0, projection.currentUsed, 0.001)
        assertEquals(1000.0, projection.limit, 0.001)

        val expectedProjectedTotal = (expectedDailyAvg * projection.remainingDays) + 500.0
        assertEquals(expectedProjectedTotal, projection.projectedTotal, 0.001)
    }

    @Test
    fun `projection formula matches - dailyAvg x remainingDays plus currentUsed equals projectedTotal`() = runTest {
        setupSuccessRepository()
        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        val projection = viewModel.uiState.value.projection!!
        val recomputed = (projection.dailyAvg * projection.remainingDays) + projection.currentUsed
        assertEquals(recomputed, projection.projectedTotal, 0.001)
    }

    @Test
    fun `projection percentage computed correctly`() = runTest {
        setupSuccessRepository()
        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        val projection = viewModel.uiState.value.projection!!
        val expectedPercentage = (projection.projectedTotal / projection.limit) * 100
        assertEquals(expectedPercentage, projection.projectedPercentage, 0.001)
    }

    @Test
    fun `projection is null when daily consumption is empty`() = runTest {
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.success(testBillingCyclesResponse))
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(
            Result.success(DailyConsumptionResponse(daily = emptyList(), totalAcus = 0.0))
        )
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimitsResponse))

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.projection)
    }

    @Test
    fun `projection is null when cycles fail to load`() = runTest {
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.failure(Exception("Cycles error")))
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.success(testDailyConsumptionResponse))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimitsResponse))

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.projection)
    }

    @Test
    fun `projection is null when acu limits fail`() = runTest {
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.success(testBillingCyclesResponse))
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.success(testDailyConsumptionResponse))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.failure(Exception("Limits error")))

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.projection)
    }

    // --- Projection Warning Threshold Tests ---

    @Test
    fun `projection under 80 percent of limit - green zone`() = runTest {
        val lowUsageLimits = AcuLimitsResponse(acuLimit = 10000.0, acusUsed = 100.0, remaining = 9900.0)
        val lowDailyConsumption = listOf(
            DailyConsumption(date = "2026-06-19", acusConsumed = 5.0)
        )
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.success(testBillingCyclesResponse))
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(
            Result.success(DailyConsumptionResponse(daily = lowDailyConsumption, totalAcus = 5.0))
        )
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(lowUsageLimits))

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        val projection = viewModel.uiState.value.projection!!
        assertTrue("Should be under 80%: ${projection.projectedPercentage}", projection.projectedPercentage < 80)
        assertFalse(projection.isOverBudget)
    }

    @Test
    fun `projection over 100 percent - over budget`() = runTest {
        val highUsageLimits = AcuLimitsResponse(acuLimit = 500.0, acusUsed = 480.0, remaining = 20.0)
        val highDailyConsumption = listOf(
            DailyConsumption(date = "2026-06-13", acusConsumed = 100.0),
            DailyConsumption(date = "2026-06-14", acusConsumed = 100.0),
            DailyConsumption(date = "2026-06-15", acusConsumed = 100.0),
            DailyConsumption(date = "2026-06-16", acusConsumed = 100.0),
            DailyConsumption(date = "2026-06-17", acusConsumed = 100.0),
            DailyConsumption(date = "2026-06-18", acusConsumed = 100.0),
            DailyConsumption(date = "2026-06-19", acusConsumed = 100.0)
        )
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.success(testBillingCyclesResponse))
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(
            Result.success(DailyConsumptionResponse(daily = highDailyConsumption, totalAcus = 700.0))
        )
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(highUsageLimits))

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        val projection = viewModel.uiState.value.projection!!
        assertTrue("Should be over 100%: ${projection.projectedPercentage}", projection.projectedPercentage > 100)
        assertTrue(projection.isOverBudget)
    }

    @Test
    fun `projection zero limit yields 0 percentage`() = runTest {
        val zeroLimitResponse = AcuLimitsResponse(acuLimit = 0.0, acusUsed = 100.0, remaining = 0.0)
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.success(testBillingCyclesResponse))
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.success(testDailyConsumptionResponse))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(zeroLimitResponse))

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        val projection = viewModel.uiState.value.projection!!
        assertEquals(0.0, projection.projectedPercentage, 0.001)
    }

    // --- Cycle Selection State Management Tests ---

    @Test
    fun `default selected cycle index is 0`() = runTest {
        setupSuccessRepository()
        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.selectedCycleIndex)
    }

    @Test
    fun `selectCycle updates selected cycle index`() = runTest {
        setupSuccessRepository()
        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.selectCycle(2)
        assertEquals(2, viewModel.uiState.value.selectedCycleIndex)

        viewModel.selectCycle(0)
        assertEquals(0, viewModel.uiState.value.selectedCycleIndex)
    }

    // --- ACU Limit Update Flow Tests ---

    @Test
    fun `showEditLimitSheet opens sheet with current limit value`() = runTest {
        setupSuccessRepository()
        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.showEditLimitSheet()

        val state = viewModel.uiState.value
        assertTrue(state.showEditLimitSheet)
        assertEquals("1000", state.editLimitValue)
        assertNull(state.permissionError)
        assertNull(state.updateLimitState)
    }

    @Test
    fun `dismissEditLimitSheet closes all dialogs and clears errors`() = runTest {
        setupSuccessRepository()
        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.showEditLimitSheet()
        viewModel.requestConfirmation()
        assertTrue(viewModel.uiState.value.showConfirmDialog)

        viewModel.dismissEditLimitSheet()

        val state = viewModel.uiState.value
        assertFalse(state.showEditLimitSheet)
        assertFalse(state.showConfirmDialog)
        assertNull(state.permissionError)
        assertNull(state.updateLimitState)
    }

    @Test
    fun `updateEditLimitValue changes the edit value`() = runTest {
        setupSuccessRepository()
        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.showEditLimitSheet()
        viewModel.updateEditLimitValue("2000")
        assertEquals("2000", viewModel.uiState.value.editLimitValue)
    }

    @Test
    fun `two-step confirmation - requestConfirmation shows dialog`() = runTest {
        setupSuccessRepository()
        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.showEditLimitSheet()
        assertFalse(viewModel.uiState.value.showConfirmDialog)

        viewModel.requestConfirmation()
        assertTrue(viewModel.uiState.value.showConfirmDialog)
    }

    @Test
    fun `two-step confirmation - dismissConfirmation hides dialog`() = runTest {
        setupSuccessRepository()
        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.showEditLimitSheet()
        viewModel.requestConfirmation()
        assertTrue(viewModel.uiState.value.showConfirmDialog)

        viewModel.dismissConfirmation()
        assertFalse(viewModel.uiState.value.showConfirmDialog)
    }

    @Test
    fun `confirmUpdateLimit success updates limits and closes sheet`() = runTest {
        setupSuccessRepository()
        val updatedLimits = AcuLimitsResponse(acuLimit = 2000.0, acusUsed = 500.0, remaining = 1500.0)
        coEvery { metricsRepository.updateAcuLimits(2000.0) } returns Result.success(updatedLimits)

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.showEditLimitSheet()
        viewModel.updateEditLimitValue("2000")
        viewModel.requestConfirmation()
        viewModel.confirmUpdateLimit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showEditLimitSheet)
        assertTrue(state.acuLimitsState is UiState.Success)
        assertEquals(2000.0, (state.acuLimitsState as UiState.Success).data.acuLimit, 0.001)
        assertTrue(state.updateLimitState is UiState.Success)
    }

    @Test
    fun `confirmUpdateLimit recalculates projection after success`() = runTest {
        setupSuccessRepository()
        val updatedLimits = AcuLimitsResponse(acuLimit = 2000.0, acusUsed = 500.0, remaining = 1500.0)
        coEvery { metricsRepository.updateAcuLimits(2000.0) } returns Result.success(updatedLimits)

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        val projectionBefore = viewModel.uiState.value.projection
        assertNotNull(projectionBefore)

        viewModel.showEditLimitSheet()
        viewModel.updateEditLimitValue("2000")
        viewModel.requestConfirmation()
        viewModel.confirmUpdateLimit()
        advanceUntilIdle()

        val projectionAfter = viewModel.uiState.value.projection
        assertNotNull(projectionAfter)
        assertEquals(2000.0, projectionAfter!!.limit, 0.001)
    }

    @Test
    fun `confirmUpdateLimit with invalid value does nothing`() = runTest {
        setupSuccessRepository()
        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.showEditLimitSheet()
        viewModel.updateEditLimitValue("not-a-number")
        viewModel.requestConfirmation()
        viewModel.confirmUpdateLimit()
        advanceUntilIdle()

        coVerify(exactly = 0) { metricsRepository.updateAcuLimits(any()) }
    }

    // --- 403 Error Handling Tests ---

    @Test
    fun `403 error sets insufficient permissions message`() = runTest {
        setupSuccessRepository()
        coEvery { metricsRepository.updateAcuLimits(any()) } returns Result.failure(
            ApiException(403, "Forbidden")
        )

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.showEditLimitSheet()
        viewModel.updateEditLimitValue("2000")
        viewModel.requestConfirmation()
        viewModel.confirmUpdateLimit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Insufficient permissions to update ACU limits", state.permissionError)
        assertTrue(state.updateLimitState is UiState.Error)
        assertEquals(
            "Insufficient permissions to update ACU limits",
            (state.updateLimitState as UiState.Error).message
        )
    }

    @Test
    fun `non-403 error uses generic error message`() = runTest {
        setupSuccessRepository()
        coEvery { metricsRepository.updateAcuLimits(any()) } returns Result.failure(
            ApiException(500, "Internal Server Error")
        )

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.showEditLimitSheet()
        viewModel.updateEditLimitValue("2000")
        viewModel.requestConfirmation()
        viewModel.confirmUpdateLimit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Internal Server Error", state.permissionError)
    }

    @Test
    fun `generic exception uses error message`() = runTest {
        setupSuccessRepository()
        coEvery { metricsRepository.updateAcuLimits(any()) } returns Result.failure(
            Exception("Network timeout")
        )

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.showEditLimitSheet()
        viewModel.updateEditLimitValue("2000")
        viewModel.requestConfirmation()
        viewModel.confirmUpdateLimit()
        advanceUntilIdle()

        assertEquals("Network timeout", viewModel.uiState.value.permissionError)
    }

    @Test
    fun `null error message uses fallback`() = runTest {
        setupSuccessRepository()
        coEvery { metricsRepository.updateAcuLimits(any()) } returns Result.failure(
            Exception(null as String?)
        )

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.showEditLimitSheet()
        viewModel.updateEditLimitValue("2000")
        viewModel.requestConfirmation()
        viewModel.confirmUpdateLimit()
        advanceUntilIdle()

        assertEquals("Failed to update ACU limits", viewModel.uiState.value.permissionError)
    }

    // --- Refresh Tests ---

    @Test
    fun `refresh sets isRefreshing to true then false`() = runTest {
        setupSuccessRepository()
        viewModel = BillingViewModel(metricsRepository)
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
        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        coVerify(exactly = 2) { metricsRepository.getBillingCycles() }
        coVerify(exactly = 2) { metricsRepository.getDailyConsumption() }
        coVerify(exactly = 2) { metricsRepository.getAcuLimits() }
    }

    // --- Section Independent Loading Tests ---

    @Test
    fun `cycles failure does not affect daily consumption or limits`() = runTest {
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.failure(Exception("Cycles error")))
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.success(testDailyConsumptionResponse))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimitsResponse))

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.cyclesState is UiState.Error)
        assertTrue(state.dailyConsumptionState is UiState.Success)
        assertTrue(state.acuLimitsState is UiState.Success)
    }

    @Test
    fun `daily consumption failure does not affect cycles or limits`() = runTest {
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.success(testBillingCyclesResponse))
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.failure(Exception("Daily error")))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimitsResponse))

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.cyclesState is UiState.Success)
        assertTrue(state.dailyConsumptionState is UiState.Error)
        assertTrue(state.acuLimitsState is UiState.Success)
    }

    @Test
    fun `all sections fail independently`() = runTest {
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.failure(Exception("Cycles fail")))
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.failure(Exception("Daily fail")))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.failure(Exception("Limits fail")))

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.cyclesState is UiState.Error)
        assertEquals("Cycles fail", (state.cyclesState as UiState.Error).message)
        assertTrue(state.dailyConsumptionState is UiState.Error)
        assertEquals("Daily fail", (state.dailyConsumptionState as UiState.Error).message)
        assertTrue(state.acuLimitsState is UiState.Error)
        assertEquals("Limits fail", (state.acuLimitsState as UiState.Error).message)
    }

    // --- Initial State Tests ---

    @Test
    fun `initial state has all sections loading`() = runTest {
        setupSuccessRepository()
        viewModel = BillingViewModel(metricsRepository)

        val state = viewModel.uiState.value
        assertEquals(UiState.Loading, state.cyclesState)
        assertEquals(UiState.Loading, state.dailyConsumptionState)
        assertEquals(UiState.Loading, state.acuLimitsState)
        assertEquals(0, state.selectedCycleIndex)
        assertFalse(state.isRefreshing)
        assertNull(state.projection)
        assertFalse(state.showEditLimitSheet)
        assertFalse(state.showConfirmDialog)
        assertEquals("", state.editLimitValue)
        assertNull(state.updateLimitState)
        assertNull(state.permissionError)
    }

    @Test
    fun `successful load transitions all sections to Success with projection`() = runTest {
        setupSuccessRepository()
        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.cyclesState is UiState.Success)
        assertTrue(state.dailyConsumptionState is UiState.Success)
        assertTrue(state.acuLimitsState is UiState.Success)
        assertNotNull(state.projection)
        assertFalse(state.isRefreshing)
    }

    // --- Edge Case Tests ---

    @Test
    fun `projection uses last 7 days for daily average`() = runTest {
        val tenDaysConsumption = (1..10).map {
            DailyConsumption(date = "2026-06-${String.format("%02d", it + 9)}", acusConsumed = it * 10.0)
        }
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.success(testBillingCyclesResponse))
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(
            Result.success(DailyConsumptionResponse(daily = tenDaysConsumption, totalAcus = 550.0))
        )
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimitsResponse))

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        val projection = viewModel.uiState.value.projection!!
        val last7 = tenDaysConsumption.takeLast(7).map { it.acusConsumed }.average()
        assertEquals(last7, projection.dailyAvg, 0.001)
    }

    @Test
    fun `projection with fewer than 7 days of data uses all available days`() = runTest {
        val threeDaysConsumption = listOf(
            DailyConsumption(date = "2026-06-17", acusConsumed = 20.0),
            DailyConsumption(date = "2026-06-18", acusConsumed = 30.0),
            DailyConsumption(date = "2026-06-19", acusConsumed = 40.0)
        )
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.success(testBillingCyclesResponse))
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(
            Result.success(DailyConsumptionResponse(daily = threeDaysConsumption, totalAcus = 90.0))
        )
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimitsResponse))

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        val projection = viewModel.uiState.value.projection!!
        assertEquals(30.0, projection.dailyAvg, 0.001)
    }

    @Test
    fun `projection uses first cycle when currentCycle is null`() = runTest {
        val noCurrent = BillingCyclesResponse(
            cycles = listOf(testBillingCycle),
            currentCycle = null
        )
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.success(noCurrent))
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.success(testDailyConsumptionResponse))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimitsResponse))

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.projection)
    }

    @Test
    fun `projection is null when no cycles exist and currentCycle is null`() = runTest {
        val emptyCycles = BillingCyclesResponse(cycles = emptyList(), currentCycle = null)
        coEvery { metricsRepository.getBillingCycles() } returns flowOf(Result.success(emptyCycles))
        coEvery { metricsRepository.getDailyConsumption() } returns flowOf(Result.success(testDailyConsumptionResponse))
        coEvery { metricsRepository.getAcuLimits() } returns flowOf(Result.success(testAcuLimitsResponse))

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.projection)
    }

    @Test
    fun `confirmUpdateLimit sets loading state before API call`() = runTest {
        setupSuccessRepository()
        val updatedLimits = AcuLimitsResponse(acuLimit = 2000.0, acusUsed = 500.0, remaining = 1500.0)
        coEvery { metricsRepository.updateAcuLimits(2000.0) } returns Result.success(updatedLimits)

        viewModel = BillingViewModel(metricsRepository)
        advanceUntilIdle()

        viewModel.showEditLimitSheet()
        viewModel.updateEditLimitValue("2000")
        viewModel.requestConfirmation()

        viewModel.uiState.test {
            val beforeConfirm = awaitItem()
            assertTrue(beforeConfirm.showConfirmDialog)

            viewModel.confirmUpdateLimit()

            val loadingState = awaitItem()
            assertFalse(loadingState.showConfirmDialog)
            assertEquals(UiState.Loading, loadingState.updateLimitState)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
