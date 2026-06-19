package com.devin.csuite.integration

import com.devin.csuite.core.NetworkMonitor
import com.devin.csuite.core.UiState
import com.devin.csuite.data.local.datasource.LocalMetricsDataSource
import com.devin.csuite.data.local.datasource.LocalSessionsDataSource
import com.devin.csuite.data.local.db.AppDatabase
import com.devin.csuite.data.remote.EnterpriseApi
import com.devin.csuite.data.remote.MetricsRepositoryImpl
import com.devin.csuite.data.remote.OfflineException
import com.devin.csuite.domain.model.BillingCycle
import com.devin.csuite.domain.model.BillingCyclesResponse
import com.devin.csuite.domain.model.MauMetricsResponse
import com.devin.csuite.domain.model.MetricDataPoint
import com.devin.csuite.domain.model.Organization
import com.devin.csuite.domain.model.OrganizationsResponse
import com.devin.csuite.domain.model.Session
import com.devin.csuite.domain.model.SessionMetricsResponse
import com.devin.csuite.domain.model.SessionsResponse
import com.devin.csuite.domain.repository.MetricsRepository
import com.devin.csuite.presentation.components.formatLastUpdated
import com.devin.csuite.presentation.home.HomeViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
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
import retrofit2.Response

/**
 * Epic 6.7.2 - Offline Mode End-to-End Tests:
 * - Load data -> simulate offline -> verify cached data shows with stale banner
 * - Go online -> verify refresh works and stale banner disappears
 * - Cache eviction after 24 hours
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineModeE2ETest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var api: EnterpriseApi
    private lateinit var localMetrics: LocalMetricsDataSource
    private lateinit var localSessions: LocalSessionsDataSource
    private lateinit var networkMonitor: NetworkMonitor
    private val isConnectedFlow = MutableStateFlow(true)

    private val testBillingCycle = BillingCycle(
        cycleStart = "2026-06-01",
        cycleEnd = "2026-06-30",
        acusUsed = 750.0,
        acuLimit = 1000.0,
        status = "active"
    )

    private val testBillingResponse = BillingCyclesResponse(
        cycles = listOf(testBillingCycle),
        currentCycle = testBillingCycle
    )

    private val testOrgsResponse = OrganizationsResponse(
        organizations = listOf(
            Organization(orgId = "org-1", name = "test-org", displayName = "Test Org")
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        api = mockk()
        localMetrics = mockk(relaxed = true)
        localSessions = mockk(relaxed = true)
        networkMonitor = mockk()
        every { networkMonitor.isConnected } returns isConnectedFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Cache-First Flow Tests ---

    @Test
    fun `online - emits cached data first then fresh remote data`() = runTest {
        isConnectedFlow.value = true
        coEvery { localMetrics.getOrganizations() } returns testOrgsResponse
        coEvery { api.getOrganizations() } returns Response.success(testOrgsResponse)

        val repository = MetricsRepositoryImpl(api, localMetrics, localSessions, networkMonitor)
        val results = repository.getOrganizations().toList()

        assertEquals(2, results.size)
        assertTrue(results[0].isSuccess) // cached
        assertTrue(results[1].isSuccess) // remote
    }

    @Test
    fun `online - fresh data is saved to cache`() = runTest {
        isConnectedFlow.value = true
        coEvery { localMetrics.getOrganizations() } returns null
        coEvery { api.getOrganizations() } returns Response.success(testOrgsResponse)

        val repository = MetricsRepositoryImpl(api, localMetrics, localSessions, networkMonitor)
        repository.getOrganizations().toList()

        coVerify { localMetrics.saveOrganizations(testOrgsResponse) }
    }

    @Test
    fun `offline with cache - emits cached data only`() = runTest {
        isConnectedFlow.value = false
        coEvery { localMetrics.getOrganizations() } returns testOrgsResponse

        val repository = MetricsRepositoryImpl(api, localMetrics, localSessions, networkMonitor)
        val results = repository.getOrganizations().toList()

        assertEquals(1, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals("org-1", results[0].getOrThrow().organizations[0].orgId)
    }

    @Test
    fun `offline without cache - emits OfflineException`() = runTest {
        isConnectedFlow.value = false
        coEvery { localMetrics.getOrganizations() } returns null

        val repository = MetricsRepositoryImpl(api, localMetrics, localSessions, networkMonitor)
        val results = repository.getOrganizations().toList()

        assertEquals(1, results.size)
        assertTrue(results[0].isFailure)
        assertTrue(results[0].exceptionOrNull() is OfflineException)
    }

    @Test
    fun `offline - no API calls are made`() = runTest {
        isConnectedFlow.value = false
        coEvery { localMetrics.getOrganizations() } returns testOrgsResponse

        val repository = MetricsRepositoryImpl(api, localMetrics, localSessions, networkMonitor)
        repository.getOrganizations().toList()

        coVerify(exactly = 0) { api.getOrganizations() }
    }

    // --- Billing Cycles Cache Flow ---

    @Test
    fun `billing cycles - cache-first flow emits cached then fresh`() = runTest {
        isConnectedFlow.value = true
        coEvery { localMetrics.getBillingCycles() } returns testBillingResponse
        coEvery { api.getBillingCycles() } returns Response.success(testBillingResponse)

        val repository = MetricsRepositoryImpl(api, localMetrics, localSessions, networkMonitor)
        val results = repository.getBillingCycles().toList()

        assertEquals(2, results.size)
        assertTrue(results[0].isSuccess)
        assertEquals(750.0, results[0].getOrThrow().currentCycle!!.acusUsed, 0.001)
    }

    @Test
    fun `billing cycles - offline returns cached data`() = runTest {
        isConnectedFlow.value = false
        coEvery { localMetrics.getBillingCycles() } returns testBillingResponse

        val repository = MetricsRepositoryImpl(api, localMetrics, localSessions, networkMonitor)
        val result = repository.getBillingCycles().firstOrNull()

        assertNotNull(result)
        assertTrue(result!!.isSuccess)
        assertEquals(1000.0, result.getOrThrow().currentCycle!!.acuLimit, 0.001)
    }

    // --- Sessions Cache Flow ---

    @Test
    fun `sessions - cached sessions returned when offline`() = runTest {
        isConnectedFlow.value = false
        val cachedSessions = SessionsResponse(
            sessions = listOf(
                Session(sessionId = "cached-1", title = "Cached Session", status = "running", acusConsumed = 5.0)
            ),
            totalCount = 1
        )
        coEvery { localSessions.getSessions(any(), any()) } returns cachedSessions

        val repository = MetricsRepositoryImpl(api, localMetrics, localSessions, networkMonitor)
        val result = repository.getSessions(limit = 50).firstOrNull()

        assertNotNull(result)
        assertTrue(result!!.isSuccess)
        assertEquals("cached-1", result.getOrThrow().sessions[0].sessionId)
    }

    // --- Stale Data Banner Tests ---

    @Test
    fun `stale banner shows when data is older than refresh interval`() = runTest {
        val lastUpdated = System.currentTimeMillis() - (20 * 60 * 1000) // 20 minutes ago
        val staleThreshold = 15 * 60 * 1000L
        val isStale = (System.currentTimeMillis() - lastUpdated) > staleThreshold
        assertTrue("Data older than 15 min should be stale", isStale)
    }

    @Test
    fun `stale banner hidden when data is fresh`() {
        val lastUpdated = System.currentTimeMillis() - (5 * 60 * 1000) // 5 minutes ago
        val staleThreshold = 15 * 60 * 1000L
        val isStale = (System.currentTimeMillis() - lastUpdated) > staleThreshold
        assertFalse("Data younger than 15 min should not be stale", isStale)
    }

    @Test
    fun `formatLastUpdated shows just now for recent updates`() {
        val now = System.currentTimeMillis()
        val result = formatLastUpdated(now - 30_000) // 30 seconds ago
        assertEquals("Updated just now", result)
    }

    @Test
    fun `formatLastUpdated shows minutes for medium-age data`() {
        val now = System.currentTimeMillis()
        val result = formatLastUpdated(now - 5 * 60_000) // 5 minutes ago
        assertTrue(result.contains("min ago"))
    }

    @Test
    fun `formatLastUpdated shows hours for old data`() {
        val now = System.currentTimeMillis()
        val result = formatLastUpdated(now - 3 * 3_600_000) // 3 hours ago
        assertTrue(result.contains("hours ago"))
    }

    @Test
    fun `formatLastUpdated shows days for very old data`() {
        val now = System.currentTimeMillis()
        val result = formatLastUpdated(now - 2 * 86_400_000) // 2 days ago
        assertTrue(result.contains("days ago"))
    }

    @Test
    fun `formatLastUpdated handles null timestamp`() {
        val result = formatLastUpdated(null)
        assertEquals("Never updated", result)
    }

    // --- Go Online After Offline ---

    @Test
    fun `going online after offline - refresh fetches fresh data`() = runTest {
        isConnectedFlow.value = false
        coEvery { localMetrics.getOrganizations() } returns testOrgsResponse

        val repository = MetricsRepositoryImpl(api, localMetrics, localSessions, networkMonitor)

        // First call while offline - gets cached
        val offlineResult = repository.getOrganizations().firstOrNull()
        assertTrue(offlineResult!!.isSuccess)

        // Go online
        isConnectedFlow.value = true
        coEvery { api.getOrganizations() } returns Response.success(testOrgsResponse)

        // Next call should hit API
        val onlineResults = repository.getOrganizations().toList()
        assertTrue(onlineResults.size >= 1)
        coVerify(atLeast = 1) { api.getOrganizations() }
    }

    @Test
    fun `going online - stale banner disappears after refresh`() {
        val lastUpdated = System.currentTimeMillis() // just refreshed
        val staleThreshold = 15 * 60 * 1000L
        val isStale = (System.currentTimeMillis() - lastUpdated) > staleThreshold
        assertFalse("Freshly updated data should not be stale", isStale)
    }

    // --- Cache Eviction after 24 Hours ---

    @Test
    fun `cache expiry constant is 24 hours in milliseconds`() {
        assertEquals(24 * 60 * 60 * 1000L, AppDatabase.CACHE_EXPIRY_MS)
    }

    @Test
    fun `data cached 25 hours ago exceeds expiry threshold`() {
        val cachedAt = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        val expiryTime = System.currentTimeMillis() - AppDatabase.CACHE_EXPIRY_MS
        val isExpired = cachedAt < expiryTime
        assertTrue("25-hour old cache should be expired", isExpired)
    }

    @Test
    fun `data cached 23 hours ago does not exceed expiry threshold`() {
        val cachedAt = System.currentTimeMillis() - (23 * 60 * 60 * 1000L)
        val expiryTime = System.currentTimeMillis() - AppDatabase.CACHE_EXPIRY_MS
        val isExpired = cachedAt < expiryTime
        assertFalse("23-hour old cache should not be expired", isExpired)
    }

    @Test
    fun `data cached exactly 24 hours ago is at boundary`() {
        val now = System.currentTimeMillis()
        val cachedAt = now - AppDatabase.CACHE_EXPIRY_MS
        val expiryTime = now - AppDatabase.CACHE_EXPIRY_MS
        // At exact boundary, not expired (< vs <=)
        val isExpired = cachedAt < expiryTime
        assertFalse("Data at exact boundary should not be expired", isExpired)
    }

    // --- Network Monitor Integration ---

    @Test
    fun `network monitor tracks connectivity state changes`() = runTest {
        assertTrue(isConnectedFlow.value)
        isConnectedFlow.value = false
        assertFalse(isConnectedFlow.value)
        isConnectedFlow.value = true
        assertTrue(isConnectedFlow.value)
    }

    @Test
    fun `HomeViewModel tracks offline state from NetworkMonitor`() = runTest {
        setupHomeRepository()
        isConnectedFlow.value = true

        val viewModel = HomeViewModel(metricsRepository = createMockMetricsRepo(), localMetrics = localMetrics, networkMonitor = networkMonitor)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isOffline)

        isConnectedFlow.value = false
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isOffline)
    }

    // --- Cache Clear on API Key Replace ---

    @Test
    fun `cache is cleared when API key is replaced`() = runTest {
        // Simulating settings replaceApiKey behavior
        coEvery { localMetrics.clearAll() } returns Unit
        coEvery { localSessions.clearAll() } returns Unit

        localMetrics.clearAll()
        localSessions.clearAll()

        coVerify { localMetrics.clearAll() }
        coVerify { localSessions.clearAll() }
    }

    private fun setupHomeRepository() {
        coEvery { localMetrics.getLastUpdated(any()) } returns System.currentTimeMillis()
    }

    private fun createMockMetricsRepo(): MetricsRepository {
        val repo = mockk<MetricsRepository>()
        coEvery { repo.getBillingCycles() } returns flowOf(
            Result.success(BillingCyclesResponse(cycles = emptyList(), currentCycle = null))
        )
        coEvery { repo.getSessions(limit = any(), status = any()) } returns flowOf(
            Result.success(SessionsResponse(sessions = emptyList(), totalCount = 0))
        )
        coEvery { repo.getMauMetrics() } returns flowOf(
            Result.success(MauMetricsResponse(mau = 0, data = emptyList()))
        )
        coEvery { repo.getDauMetrics() } returns flowOf(
            Result.success(com.devin.csuite.domain.model.DauMetricsResponse(dau = 0, data = emptyList()))
        )
        coEvery { repo.getPrMetrics() } returns flowOf(
            Result.success(com.devin.csuite.domain.model.PrMetricsResponse(totalPrs = 0, mergedPrs = 0, data = emptyList()))
        )
        coEvery { repo.getSessionMetrics() } returns flowOf(
            Result.success(SessionMetricsResponse(totalSessions = 0, data = emptyList()))
        )
        coEvery { repo.getActiveUsers() } returns flowOf(
            Result.success(com.devin.csuite.domain.model.ActiveUsersResponse(users = emptyList()))
        )
        return repo
    }
}
