package com.devin.csuite.presentation.security

import com.devin.csuite.core.UiState
import com.devin.csuite.data.remote.ApiException
import com.devin.csuite.domain.model.security.AuditLogEntry
import com.devin.csuite.domain.model.security.AuditLogsResponse
import com.devin.csuite.domain.model.security.GuardrailViolation
import com.devin.csuite.domain.model.security.GuardrailViolationsResponse
import com.devin.csuite.domain.model.security.IpAccessEntry
import com.devin.csuite.domain.model.security.IpAccessListResponse
import com.devin.csuite.domain.repository.security.SecurityRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var securityRepository: SecurityRepository
    private lateinit var viewModel: SecurityViewModel

    private val testAuditLogs = listOf(
        AuditLogEntry(
            id = "log-1",
            actorName = "Alice",
            actorEmail = "alice@test.com",
            action = "create",
            resourceType = "session",
            resourceId = "s-1",
            timestamp = System.currentTimeMillis(),
            details = "Created session"
        ),
        AuditLogEntry(
            id = "log-2",
            actorName = "Bob",
            actorEmail = "bob@test.com",
            action = "delete",
            resourceType = "api_key",
            resourceId = "k-1",
            timestamp = System.currentTimeMillis() - 60_000,
            details = "Deleted API key"
        )
    )

    private val testAuditLogsResponse = AuditLogsResponse(
        auditLogs = testAuditLogs,
        nextCursor = "cursor-abc",
        hasMore = true
    )

    private val testAuditLogsPage2 = AuditLogsResponse(
        auditLogs = listOf(
            AuditLogEntry(
                id = "log-3",
                actorName = "Charlie",
                actorEmail = "charlie@test.com",
                action = "update",
                resourceType = "guardrail",
                resourceId = "g-1",
                timestamp = System.currentTimeMillis() - 120_000
            )
        ),
        nextCursor = null,
        hasMore = false
    )

    private val testViolations = listOf(
        GuardrailViolation(
            id = "v-1",
            severity = "critical",
            description = "Unauthorized file access",
            sessionId = "s-1",
            timestamp = System.currentTimeMillis(),
            ruleName = "file-access-restriction"
        ),
        GuardrailViolation(
            id = "v-2",
            severity = "high",
            description = "Rate limit exceeded",
            sessionId = "s-2",
            timestamp = System.currentTimeMillis() - 86_400_000,
            ruleName = "rate-limit"
        )
    )

    private val testViolationsResponse = GuardrailViolationsResponse(
        violations = testViolations,
        nextCursor = null,
        hasMore = false
    )

    private val testIpEntries = listOf(
        IpAccessEntry(ip = "192.168.1.1", description = "Office", createdAt = System.currentTimeMillis()),
        IpAccessEntry(ip = "10.0.0.0/8", description = "VPN", createdAt = System.currentTimeMillis())
    )

    private val testIpAccessListResponse = IpAccessListResponse(ipAddresses = testIpEntries)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        securityRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupSuccessRepository() {
        coEvery {
            securityRepository.getAuditLogs(first = any(), after = any(), actionType = any())
        } returns flowOf(Result.success(testAuditLogsResponse))
        coEvery {
            securityRepository.getGuardrailViolations(first = any(), after = any(), severity = any())
        } returns flowOf(Result.success(testViolationsResponse))
        coEvery {
            securityRepository.getIpAccessList()
        } returns flowOf(Result.success(testIpAccessListResponse))
    }

    private fun createViewModel(): SecurityViewModel {
        viewModel = SecurityViewModel(securityRepository)
        return viewModel
    }

    // ---- Audit Log Pagination Tests ----

    @Test
    fun `initial load fetches first page of audit logs`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.auditLogsState is UiState.Success)
        assertEquals(2, state.auditLogs.size)
        assertEquals("cursor-abc", state.auditLogCursor)
        assertTrue(state.auditLogHasMore)
    }

    @Test
    fun `loadMoreAuditLogs appends second page`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        coEvery {
            securityRepository.getAuditLogs(first = 20, after = "cursor-abc", actionType = null)
        } returns flowOf(Result.success(testAuditLogsPage2))

        viewModel.loadMoreAuditLogs()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.auditLogs.size)
        assertEquals("log-3", state.auditLogs[2].id)
        assertNull(state.auditLogCursor)
        assertFalse(state.auditLogHasMore)
    }

    @Test
    fun `loadMoreAuditLogs does nothing when no more pages`() = runTest {
        val noMoreResponse = testAuditLogsResponse.copy(hasMore = false, nextCursor = null)
        coEvery {
            securityRepository.getAuditLogs(first = any(), after = any(), actionType = any())
        } returns flowOf(Result.success(noMoreResponse))
        coEvery {
            securityRepository.getGuardrailViolations(first = any(), after = any(), severity = any())
        } returns flowOf(Result.success(testViolationsResponse))
        coEvery {
            securityRepository.getIpAccessList()
        } returns flowOf(Result.success(testIpAccessListResponse))

        createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.auditLogHasMore)

        viewModel.loadMoreAuditLogs()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.auditLogs.size)
    }

    @Test
    fun `loadMoreAuditLogs does nothing when already loading more`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        coEvery {
            securityRepository.getAuditLogs(first = 20, after = "cursor-abc", actionType = null)
        } returns flowOf(Result.success(testAuditLogsPage2))

        viewModel.loadMoreAuditLogs()
        viewModel.loadMoreAuditLogs()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.auditLogs.size)
    }

    @Test
    fun `audit log load failure on initial load shows error`() = runTest {
        coEvery {
            securityRepository.getAuditLogs(first = any(), after = any(), actionType = any())
        } returns flowOf(Result.failure(Exception("Network error")))
        coEvery {
            securityRepository.getGuardrailViolations(first = any(), after = any(), severity = any())
        } returns flowOf(Result.success(testViolationsResponse))
        coEvery {
            securityRepository.getIpAccessList()
        } returns flowOf(Result.success(testIpAccessListResponse))

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.auditLogsState is UiState.Error)
        assertEquals("Network error", (state.auditLogsState as UiState.Error).message)
    }

    @Test
    fun `audit log pagination failure keeps existing logs`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.auditLogs.size)

        coEvery {
            securityRepository.getAuditLogs(first = 20, after = "cursor-abc", actionType = null)
        } returns flowOf(Result.failure(Exception("Page load failed")))

        viewModel.loadMoreAuditLogs()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.auditLogs.size)
        assertFalse(state.isLoadingMoreAuditLogs)
    }

    // ---- Guardrail API Fallback Tests ----

    @Test
    fun `guardrail 404 shows not available instead of error`() = runTest {
        coEvery {
            securityRepository.getAuditLogs(first = any(), after = any(), actionType = any())
        } returns flowOf(Result.success(testAuditLogsResponse))
        coEvery {
            securityRepository.getGuardrailViolations(first = any(), after = any(), severity = any())
        } returns flowOf(Result.failure(ApiException(404, "Feature not available")))
        coEvery {
            securityRepository.getIpAccessList()
        } returns flowOf(Result.success(testIpAccessListResponse))

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.guardrailAvailable)
        assertTrue(state.guardrailState is UiState.Success)
        assertEquals(emptyList<GuardrailViolation>(), (state.guardrailState as UiState.Success).data)
        assertTrue(state.violationTrendState is UiState.Success)
    }

    @Test
    fun `guardrail 501 shows not available instead of error`() = runTest {
        coEvery {
            securityRepository.getAuditLogs(first = any(), after = any(), actionType = any())
        } returns flowOf(Result.success(testAuditLogsResponse))
        coEvery {
            securityRepository.getGuardrailViolations(first = any(), after = any(), severity = any())
        } returns flowOf(Result.failure(ApiException(501, "Feature not available")))
        coEvery {
            securityRepository.getIpAccessList()
        } returns flowOf(Result.success(testIpAccessListResponse))

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.guardrailAvailable)
        assertTrue(state.guardrailState is UiState.Success)
        assertEquals(emptyList<GuardrailViolation>(), (state.guardrailState as UiState.Success).data)
    }

    @Test
    fun `guardrail non-404-501 error shows error state`() = runTest {
        coEvery {
            securityRepository.getAuditLogs(first = any(), after = any(), actionType = any())
        } returns flowOf(Result.success(testAuditLogsResponse))
        coEvery {
            securityRepository.getGuardrailViolations(first = any(), after = any(), severity = any())
        } returns flowOf(Result.failure(ApiException(500, "Internal server error")))
        coEvery {
            securityRepository.getIpAccessList()
        } returns flowOf(Result.success(testIpAccessListResponse))

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.guardrailAvailable)
        assertTrue(state.guardrailState is UiState.Error)
        assertEquals("Internal server error", (state.guardrailState as UiState.Error).message)
    }

    @Test
    fun `guardrail network error shows error state`() = runTest {
        coEvery {
            securityRepository.getAuditLogs(first = any(), after = any(), actionType = any())
        } returns flowOf(Result.success(testAuditLogsResponse))
        coEvery {
            securityRepository.getGuardrailViolations(first = any(), after = any(), severity = any())
        } returns flowOf(Result.failure(Exception("Network timeout")))
        coEvery {
            securityRepository.getIpAccessList()
        } returns flowOf(Result.success(testIpAccessListResponse))

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.guardrailAvailable)
        assertTrue(state.guardrailState is UiState.Error)
    }

    @Test
    fun `successful guardrail load sets available flag and populates violations`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.guardrailAvailable)
        assertTrue(state.guardrailState is UiState.Success)
        assertEquals(2, state.guardrailViolations.size)
        assertTrue(state.violationTrendState is UiState.Success)
    }

    // ---- IP Access List CRUD Tests ----

    @Test
    fun `initial load fetches IP access list`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.ipAccessState is UiState.Success)
        assertEquals(2, state.ipAddresses.size)
        assertEquals("192.168.1.1", state.ipAddresses[0].ip)
    }

    @Test
    fun `requestAddIp with valid IP shows confirmation`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        viewModel.requestAddIp("10.0.0.1", "New server")

        val state = viewModel.uiState.value
        assertTrue(state.showConfirmAddIp)
        assertEquals("10.0.0.1", state.pendingAddIp)
        assertEquals("New server", state.pendingAddIpDescription)
        assertNull(state.addIpError)
    }

    @Test
    fun `requestAddIp with invalid IP shows error`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        viewModel.requestAddIp("999.1.1.1", "Bad IP")

        val state = viewModel.uiState.value
        assertFalse(state.showConfirmAddIp)
        assertTrue(state.addIpError != null)
    }

    @Test
    fun `confirmAddIp calls repository and updates list on success`() = runTest {
        setupSuccessRepository()
        val updatedList = IpAccessListResponse(
            ipAddresses = testIpEntries + IpAccessEntry(ip = "10.0.0.1", description = "New server")
        )
        coEvery { securityRepository.addIpAddress("10.0.0.1", "New server") } returns Result.success(updatedList)

        createViewModel()
        advanceUntilIdle()

        viewModel.requestAddIp("10.0.0.1", "New server")
        viewModel.confirmAddIp()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.addIpLoading)
        assertFalse(state.showAddIpSheet)
        assertEquals(3, state.ipAddresses.size)
        coVerify { securityRepository.addIpAddress("10.0.0.1", "New server") }
    }

    @Test
    fun `confirmAddIp with blank description sends null`() = runTest {
        setupSuccessRepository()
        val updatedList = IpAccessListResponse(
            ipAddresses = testIpEntries + IpAccessEntry(ip = "10.0.0.1")
        )
        coEvery { securityRepository.addIpAddress("10.0.0.1", null) } returns Result.success(updatedList)

        createViewModel()
        advanceUntilIdle()

        viewModel.requestAddIp("10.0.0.1", "")
        viewModel.confirmAddIp()
        advanceUntilIdle()

        coVerify { securityRepository.addIpAddress("10.0.0.1", null) }
    }

    @Test
    fun `requestRemoveIp shows confirmation dialog`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        viewModel.requestRemoveIp("192.168.1.1")

        val state = viewModel.uiState.value
        assertEquals("192.168.1.1", state.showConfirmRemoveIp)
    }

    @Test
    fun `confirmRemoveIp calls repository and updates list on success`() = runTest {
        setupSuccessRepository()
        val updatedList = IpAccessListResponse(
            ipAddresses = listOf(testIpEntries[1])
        )
        coEvery { securityRepository.removeIpAddress("192.168.1.1") } returns Result.success(updatedList)

        createViewModel()
        advanceUntilIdle()

        viewModel.requestRemoveIp("192.168.1.1")
        viewModel.confirmRemoveIp()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.removeIpLoading)
        assertEquals(1, state.ipAddresses.size)
        assertEquals("10.0.0.0/8", state.ipAddresses[0].ip)
        coVerify { securityRepository.removeIpAddress("192.168.1.1") }
    }

    @Test
    fun `cancelAddIp dismisses confirmation`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        viewModel.requestAddIp("10.0.0.1", "test")
        assertTrue(viewModel.uiState.value.showConfirmAddIp)

        viewModel.cancelAddIp()
        assertFalse(viewModel.uiState.value.showConfirmAddIp)
    }

    @Test
    fun `cancelRemoveIp dismisses confirmation`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        viewModel.requestRemoveIp("192.168.1.1")
        assertEquals("192.168.1.1", viewModel.uiState.value.showConfirmRemoveIp)

        viewModel.cancelRemoveIp()
        assertNull(viewModel.uiState.value.showConfirmRemoveIp)
    }

    // ---- 403 Error Handling for IP Mutations ----

    @Test
    fun `addIp 403 error shows insufficient permissions`() = runTest {
        setupSuccessRepository()
        coEvery { securityRepository.addIpAddress(any(), any()) } returns
            Result.failure(ApiException(403, "Insufficient permissions"))

        createViewModel()
        advanceUntilIdle()

        viewModel.requestAddIp("10.0.0.1", "test")
        viewModel.confirmAddIp()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Insufficient permissions", state.ipMutationError)
        assertFalse(state.addIpLoading)
    }

    @Test
    fun `removeIp 403 error shows insufficient permissions`() = runTest {
        setupSuccessRepository()
        coEvery { securityRepository.removeIpAddress(any()) } returns
            Result.failure(ApiException(403, "Insufficient permissions"))

        createViewModel()
        advanceUntilIdle()

        viewModel.requestRemoveIp("192.168.1.1")
        viewModel.confirmRemoveIp()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Insufficient permissions", state.ipMutationError)
        assertNull(state.removeIpLoading)
    }

    @Test
    fun `addIp non-403 error shows error message`() = runTest {
        setupSuccessRepository()
        coEvery { securityRepository.addIpAddress(any(), any()) } returns
            Result.failure(ApiException(500, "Server error"))

        createViewModel()
        advanceUntilIdle()

        viewModel.requestAddIp("10.0.0.1", "test")
        viewModel.confirmAddIp()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Server error", state.ipMutationError)
    }

    @Test
    fun `dismissMutationError clears error`() = runTest {
        setupSuccessRepository()
        coEvery { securityRepository.addIpAddress(any(), any()) } returns
            Result.failure(ApiException(403, "Insufficient permissions"))

        createViewModel()
        advanceUntilIdle()

        viewModel.requestAddIp("10.0.0.1", "test")
        viewModel.confirmAddIp()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.ipMutationError != null)

        viewModel.dismissMutationError()
        assertNull(viewModel.uiState.value.ipMutationError)
    }

    // ---- IP Access List 403 on Load ----

    @Test
    fun `IP access list 403 shows permission error`() = runTest {
        coEvery {
            securityRepository.getAuditLogs(first = any(), after = any(), actionType = any())
        } returns flowOf(Result.success(testAuditLogsResponse))
        coEvery {
            securityRepository.getGuardrailViolations(first = any(), after = any(), severity = any())
        } returns flowOf(Result.success(testViolationsResponse))
        coEvery {
            securityRepository.getIpAccessList()
        } returns flowOf(Result.failure(ApiException(403, "Insufficient permissions")))

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.ipAccessState is UiState.Error)
        assertEquals(
            "Insufficient permissions to view IP access list",
            (state.ipAccessState as UiState.Error).message
        )
    }

    // ---- Filter Application Tests ----

    @Test
    fun `setActionTypeFilter re-fetches audit logs with filter`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        val filteredResponse = AuditLogsResponse(
            auditLogs = listOf(testAuditLogs[0]),
            nextCursor = null,
            hasMore = false
        )
        coEvery {
            securityRepository.getAuditLogs(first = 20, after = null, actionType = "create")
        } returns flowOf(Result.success(filteredResponse))

        viewModel.setActionTypeFilter("create")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("create", state.selectedActionTypeFilter)
        assertEquals(1, state.auditLogs.size)
        assertEquals("create", state.auditLogs[0].action)
    }

    @Test
    fun `clearing action type filter re-fetches all audit logs`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        viewModel.setActionTypeFilter(null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedActionTypeFilter)
        coVerify(atLeast = 2) {
            securityRepository.getAuditLogs(first = 20, after = null, actionType = null)
        }
    }

    @Test
    fun `setSeverityFilter re-fetches guardrail violations with filter`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        val filteredResponse = GuardrailViolationsResponse(
            violations = listOf(testViolations[0]),
            nextCursor = null,
            hasMore = false
        )
        coEvery {
            securityRepository.getGuardrailViolations(first = 50, after = null, severity = "critical")
        } returns flowOf(Result.success(filteredResponse))

        viewModel.setSeverityFilter("critical")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("critical", state.selectedSeverityFilter)
        assertEquals(1, state.guardrailViolations.size)
        assertEquals("critical", state.guardrailViolations[0].severity)
    }

    @Test
    fun `clearing severity filter re-fetches all violations`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        viewModel.setSeverityFilter(null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedSeverityFilter)
        coVerify(atLeast = 2) {
            securityRepository.getGuardrailViolations(first = 50, after = null, severity = null)
        }
    }

    // ---- Feature Flag for Guardrail Violations Beta API ----

    @Test
    fun `guardrailAvailable starts true and remains true on success`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.guardrailAvailable)
    }

    @Test
    fun `guardrailAvailable set to false on 404 and recovers on refresh success`() = runTest {
        coEvery {
            securityRepository.getAuditLogs(first = any(), after = any(), actionType = any())
        } returns flowOf(Result.success(testAuditLogsResponse))
        coEvery {
            securityRepository.getGuardrailViolations(first = any(), after = any(), severity = any())
        } returns flowOf(Result.failure(ApiException(404, "Feature not available")))
        coEvery {
            securityRepository.getIpAccessList()
        } returns flowOf(Result.success(testIpAccessListResponse))

        createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.guardrailAvailable)

        coEvery {
            securityRepository.getGuardrailViolations(first = any(), after = any(), severity = any())
        } returns flowOf(Result.success(testViolationsResponse))

        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.guardrailAvailable)
        assertEquals(2, viewModel.uiState.value.guardrailViolations.size)
    }

    // ---- Tab Selection ----

    @Test
    fun `selectTab updates selected tab`() = runTest {
        setupSuccessRepository()
        createViewModel()

        assertEquals(SecurityTab.AUDIT_LOGS, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(SecurityTab.GUARDRAILS)
        assertEquals(SecurityTab.GUARDRAILS, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(SecurityTab.IP_ACCESS)
        assertEquals(SecurityTab.IP_ACCESS, viewModel.uiState.value.selectedTab)
    }

    // ---- Refresh ----

    @Test
    fun `refresh reloads all data`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
        coVerify(atLeast = 2) {
            securityRepository.getAuditLogs(first = any(), after = any(), actionType = any())
        }
        coVerify(atLeast = 2) {
            securityRepository.getGuardrailViolations(first = any(), after = any(), severity = any())
        }
        coVerify(atLeast = 2) {
            securityRepository.getIpAccessList()
        }
    }

    // ---- Sheet State Management ----

    @Test
    fun `showAddIpSheet and dismissAddIpSheet toggle state`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        viewModel.showAddIpSheet()
        assertTrue(viewModel.uiState.value.showAddIpSheet)
        assertEquals("", viewModel.uiState.value.pendingAddIp)
        assertEquals("", viewModel.uiState.value.pendingAddIpDescription)

        viewModel.dismissAddIpSheet()
        assertFalse(viewModel.uiState.value.showAddIpSheet)
    }

    @Test
    fun `showFilterSheet and dismissFilterSheet toggle state`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        viewModel.showFilterSheet()
        assertTrue(viewModel.uiState.value.showFilterSheet)

        viewModel.dismissFilterSheet()
        assertFalse(viewModel.uiState.value.showFilterSheet)
    }

    // ---- Audit Log Expand/Collapse ----

    @Test
    fun `toggleAuditLogExpanded toggles and untoggles`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.expandedAuditLogId)

        viewModel.toggleAuditLogExpanded("log-1")
        assertEquals("log-1", viewModel.uiState.value.expandedAuditLogId)

        viewModel.toggleAuditLogExpanded("log-1")
        assertNull(viewModel.uiState.value.expandedAuditLogId)
    }

    @Test
    fun `toggling different log collapses previous`() = runTest {
        setupSuccessRepository()
        createViewModel()
        advanceUntilIdle()

        viewModel.toggleAuditLogExpanded("log-1")
        assertEquals("log-1", viewModel.uiState.value.expandedAuditLogId)

        viewModel.toggleAuditLogExpanded("log-2")
        assertEquals("log-2", viewModel.uiState.value.expandedAuditLogId)
    }
}
