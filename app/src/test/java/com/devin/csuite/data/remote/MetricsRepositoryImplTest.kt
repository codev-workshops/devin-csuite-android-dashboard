package com.devin.csuite.data.remote

import com.devin.csuite.core.NetworkMonitor
import com.devin.csuite.data.local.datasource.LocalMetricsDataSource
import com.devin.csuite.data.local.datasource.LocalSessionsDataSource
import com.devin.csuite.domain.model.ActiveUser
import com.devin.csuite.domain.model.ActiveUsersResponse
import com.devin.csuite.domain.model.BillingCycle
import com.devin.csuite.domain.model.BillingCyclesResponse
import com.devin.csuite.domain.model.DauMetricsResponse
import com.devin.csuite.domain.model.MauMetricsResponse
import com.devin.csuite.domain.model.MetricDataPoint
import com.devin.csuite.domain.model.Organization
import com.devin.csuite.domain.model.OrganizationsResponse
import com.devin.csuite.domain.model.PrMetricsResponse
import com.devin.csuite.domain.model.Session
import com.devin.csuite.domain.model.SessionMetricsResponse
import com.devin.csuite.domain.model.SessionsResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MetricsRepositoryImplTest {

    private lateinit var api: EnterpriseApi
    private lateinit var localMetrics: LocalMetricsDataSource
    private lateinit var localSessions: LocalSessionsDataSource
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var repository: MetricsRepositoryImpl

    @Before
    fun setup() {
        api = mockk()
        localMetrics = mockk(relaxUnitFun = true)
        localSessions = mockk(relaxUnitFun = true)
        networkMonitor = mockk()
        every { networkMonitor.isConnected } returns MutableStateFlow(true)
        coEvery { localMetrics.getOrganizations() } returns null
        coEvery { localMetrics.getBillingCycles() } returns null
        coEvery { localMetrics.getMauMetrics() } returns null
        coEvery { localMetrics.getDauMetrics() } returns null
        coEvery { localMetrics.getPrMetrics() } returns null
        coEvery { localMetrics.getSessionMetrics() } returns null
        coEvery { localMetrics.getSearchMetrics() } returns null
        coEvery { localMetrics.getActiveUsers() } returns null
        coEvery { localMetrics.getAcuLimits() } returns null
        coEvery { localMetrics.getDailyConsumption() } returns null
        coEvery { localSessions.getSessions(any(), any()) } returns null
        repository = MetricsRepositoryImpl(api, localMetrics, localSessions, networkMonitor)
    }

    // --- API Response Parsing Tests ---

    @Test
    fun `getOrganizations returns parsed organizations on success`() = runTest {
        val response = OrganizationsResponse(
            organizations = listOf(
                Organization(orgId = "org-1", name = "test-org", displayName = "Test Org"),
                Organization(orgId = "org-2", name = "another-org", displayName = "Another Org")
            )
        )
        coEvery { api.getOrganizations() } returns Response.success(response)

        val result = repository.getOrganizations().firstOrNull()
        assertNotNull(result)
        assertTrue(result!!.isSuccess)
        assertEquals(2, result.getOrThrow().organizations.size)
        assertEquals("org-1", result.getOrThrow().organizations[0].orgId)
    }

    @Test
    fun `getBillingCycles returns parsed billing data on success`() = runTest {
        val cycle = BillingCycle(
            cycleStart = "2026-06-01", cycleEnd = "2026-06-30",
            acusUsed = 500.0, acuLimit = 1000.0, status = "active"
        )
        val response = BillingCyclesResponse(cycles = listOf(cycle), currentCycle = cycle)
        coEvery { api.getBillingCycles() } returns Response.success(response)

        val result = repository.getBillingCycles().firstOrNull()
        assertNotNull(result)
        assertTrue(result!!.isSuccess)
        assertEquals(500.0, result.getOrThrow().currentCycle!!.acusUsed, 0.001)
        assertEquals(1000.0, result.getOrThrow().currentCycle!!.acuLimit, 0.001)
    }

    @Test
    fun `getSessions returns sessions with total count`() = runTest {
        val response = SessionsResponse(
            sessions = listOf(
                Session(sessionId = "s1", title = "Session 1", status = "running", acusConsumed = 10.0),
                Session(sessionId = "s2", title = "Session 2", status = "finished", acusConsumed = 20.0)
            ),
            totalCount = 100
        )
        coEvery { api.getSessions(limit = 50, offset = 0, status = "running") } returns Response.success(response)

        val result = repository.getSessions(limit = 50, status = "running").firstOrNull()
        assertNotNull(result)
        assertTrue(result!!.isSuccess)
        assertEquals(2, result.getOrThrow().sessions.size)
        assertEquals(100, result.getOrThrow().totalCount)
    }

    @Test
    fun `getMauMetrics returns MAU data`() = runTest {
        val response = MauMetricsResponse(mau = 200, data = listOf(MetricDataPoint("2026-06-01", 180.0)))
        coEvery { api.getMauMetrics() } returns Response.success(response)

        val result = repository.getMauMetrics().firstOrNull()
        assertTrue(result!!.isSuccess)
        assertEquals(200, result.getOrThrow().mau)
        assertEquals(1, result.getOrThrow().data.size)
    }

    @Test
    fun `getDauMetrics returns DAU data with data points`() = runTest {
        val response = DauMetricsResponse(
            dau = 30,
            data = listOf(
                MetricDataPoint("2026-06-18", 28.0),
                MetricDataPoint("2026-06-19", 30.0)
            )
        )
        coEvery { api.getDauMetrics() } returns Response.success(response)

        val result = repository.getDauMetrics().firstOrNull()
        assertTrue(result!!.isSuccess)
        assertEquals(30, result.getOrThrow().dau)
        assertEquals(2, result.getOrThrow().data.size)
    }

    @Test
    fun `getPrMetrics returns PR metrics`() = runTest {
        val response = PrMetricsResponse(totalPrs = 75, mergedPrs = 60, data = emptyList())
        coEvery { api.getPrMetrics() } returns Response.success(response)

        val result = repository.getPrMetrics().firstOrNull()
        assertTrue(result!!.isSuccess)
        assertEquals(75, result.getOrThrow().totalPrs)
        assertEquals(60, result.getOrThrow().mergedPrs)
    }

    @Test
    fun `getSessionMetrics returns session metrics with chart data`() = runTest {
        val response = SessionMetricsResponse(
            totalSessions = 500,
            data = listOf(MetricDataPoint("2026-06-18", 45.0))
        )
        coEvery { api.getSessionMetrics() } returns Response.success(response)

        val result = repository.getSessionMetrics().firstOrNull()
        assertTrue(result!!.isSuccess)
        assertEquals(500, result.getOrThrow().totalSessions)
    }

    @Test
    fun `getActiveUsers returns user list`() = runTest {
        val response = ActiveUsersResponse(
            users = listOf(
                ActiveUser(userEmail = "alice@test.com", displayName = "Alice", sessionCount = 50, acusConsumed = 200.0)
            )
        )
        coEvery { api.getActiveUsers() } returns Response.success(response)

        val result = repository.getActiveUsers().firstOrNull()
        assertTrue(result!!.isSuccess)
        assertEquals(1, result.getOrThrow().users.size)
        assertEquals("alice@test.com", result.getOrThrow().users[0].userEmail)
    }

    // --- Error Mapping Tests ---

    @Test
    fun `401 response maps to Invalid API key error`() = runTest {
        coEvery { api.getOrganizations() } returns Response.error(
            401, "Unauthorized".toResponseBody(null)
        )

        val result = repository.getOrganizations().firstOrNull()
        assertTrue(result!!.isFailure)
        val exception = result.exceptionOrNull() as ApiException
        assertEquals(401, exception.code)
        assertTrue(exception.message!!.contains("Invalid or expired API key"))
    }

    @Test
    fun `403 response maps to Invalid API key error`() = runTest {
        coEvery { api.getBillingCycles() } returns Response.error(
            403, "Forbidden".toResponseBody(null)
        )

        val result = repository.getBillingCycles().firstOrNull()
        assertTrue(result!!.isFailure)
        val exception = result.exceptionOrNull() as ApiException
        assertEquals(403, exception.code)
        assertTrue(exception.message!!.contains("Invalid or expired API key"))
    }

    @Test
    fun `429 response maps to rate limit error`() = runTest {
        coEvery { api.getMauMetrics() } returns Response.error(
            429, "Too Many Requests".toResponseBody(null)
        )

        val result = repository.getMauMetrics().firstOrNull()
        assertTrue(result!!.isFailure)
        val exception = result.exceptionOrNull() as ApiException
        assertEquals(429, exception.code)
        assertTrue(exception.message!!.contains("Rate limited"))
    }

    @Test
    fun `500 response maps to generic API error`() = runTest {
        coEvery { api.getDauMetrics() } returns Response.error(
            500, "Internal Server Error".toResponseBody(null)
        )

        val result = repository.getDauMetrics().firstOrNull()
        assertTrue(result!!.isFailure)
        val exception = result.exceptionOrNull() as ApiException
        assertEquals(500, exception.code)
        assertTrue(exception.message!!.contains("API error: 500"))
    }

    @Test
    fun `IOException maps to network error`() = runTest {
        coEvery { api.getOrganizations() } throws IOException("Network unreachable")

        val result = repository.getOrganizations().firstOrNull()
        assertTrue(result!!.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `SocketTimeoutException maps to timeout error`() = runTest {
        coEvery { api.getBillingCycles() } throws SocketTimeoutException("Connection timed out")

        val result = repository.getBillingCycles().firstOrNull()
        assertTrue(result!!.isFailure)
        assertTrue(result.exceptionOrNull() is SocketTimeoutException)
    }

    @Test
    fun `UnknownHostException maps to DNS error`() = runTest {
        coEvery { api.getPrMetrics() } throws UnknownHostException("Unable to resolve host")

        val result = repository.getPrMetrics().firstOrNull()
        assertTrue(result!!.isFailure)
        assertTrue(result.exceptionOrNull() is UnknownHostException)
    }

    @Test
    fun `null response body returns Empty response body error`() = runTest {
        coEvery { api.getOrganizations() } returns Response.success(null)

        val result = repository.getOrganizations().firstOrNull()
        assertTrue(result!!.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Empty response body"))
    }

    // --- Flow Emission Tests ---

    @Test
    fun `repository returns Flow of Result`() = runTest {
        val response = OrganizationsResponse(organizations = emptyList())
        coEvery { api.getOrganizations() } returns Response.success(response)

        val flow = repository.getOrganizations()
        var emissionCount = 0
        flow.collect {
            emissionCount++
            assertTrue(it.isSuccess)
        }
        assertEquals(1, emissionCount)
    }

    @Test
    fun `getSessions passes parameters correctly`() = runTest {
        val response = SessionsResponse(sessions = emptyList(), totalCount = 0)
        coEvery { api.getSessions(limit = 25, offset = 0, status = "finished") } returns Response.success(response)

        val result = repository.getSessions(limit = 25, status = "finished").firstOrNull()
        assertTrue(result!!.isSuccess)
    }

    @Test
    fun `getSessions with null status passes null`() = runTest {
        val response = SessionsResponse(sessions = emptyList(), totalCount = 0)
        coEvery { api.getSessions(limit = 100, offset = 0, status = null) } returns Response.success(response)

        val result = repository.getSessions(limit = 100, status = null).firstOrNull()
        assertTrue(result!!.isSuccess)
    }

    // --- validateApiKey Tests ---

    @Test
    fun `validateApiKey returns success directly without Flow`() = runTest {
        val response = OrganizationsResponse(
            organizations = listOf(Organization(orgId = "org-1", name = "test"))
        )
        coEvery { api.getOrganizations() } returns Response.success(response)

        val result = repository.validateApiKey()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().organizations.size)
    }

    @Test
    fun `validateApiKey returns failure on 401`() = runTest {
        coEvery { api.getOrganizations() } returns Response.error(
            401, "Unauthorized".toResponseBody(null)
        )

        val result = repository.validateApiKey()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ApiException)
    }

    @Test
    fun `validateApiKey returns failure on network error`() = runTest {
        coEvery { api.getOrganizations() } throws IOException("No internet")

        val result = repository.validateApiKey()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
