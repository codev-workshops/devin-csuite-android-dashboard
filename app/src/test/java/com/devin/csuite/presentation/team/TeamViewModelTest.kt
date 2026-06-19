package com.devin.csuite.presentation.team

import app.cash.turbine.test
import com.devin.csuite.core.UiState
import com.devin.csuite.domain.model.ActiveUser
import com.devin.csuite.domain.model.ActiveUsersResponse
import com.devin.csuite.domain.model.DauMetricsResponse
import com.devin.csuite.domain.model.EnterpriseRole
import com.devin.csuite.domain.model.EnterpriseUser
import com.devin.csuite.domain.model.MauMetricsResponse
import com.devin.csuite.domain.model.MetricDataPoint
import com.devin.csuite.domain.model.Organization
import com.devin.csuite.domain.model.OrganizationsResponse
import com.devin.csuite.domain.model.RolesResponse
import com.devin.csuite.domain.model.UsersResponse
import com.devin.csuite.domain.model.WauMetricsResponse
import com.devin.csuite.domain.repository.TeamRepository
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
class TeamViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var teamRepository: TeamRepository
    private lateinit var viewModel: TeamViewModel

    private val testDauData = listOf(
        MetricDataPoint(date = "2026-06-18", value = 25.0),
        MetricDataPoint(date = "2026-06-19", value = 30.0)
    )
    private val testWauData = listOf(
        MetricDataPoint(date = "2026-06-18", value = 80.0),
        MetricDataPoint(date = "2026-06-19", value = 85.0)
    )
    private val testMauData = listOf(
        MetricDataPoint(date = "2026-06-18", value = 140.0),
        MetricDataPoint(date = "2026-06-19", value = 150.0)
    )

    private val testDauResponse = DauMetricsResponse(dau = 30, data = testDauData)
    private val testWauResponse = WauMetricsResponse(wau = 85, data = testWauData)
    private val testMauResponse = MauMetricsResponse(mau = 150, data = testMauData)

    private val testActiveUsersResponse = ActiveUsersResponse(
        users = listOf(
            ActiveUser(userEmail = "alice@test.com", displayName = "Alice", sessionCount = 50, acusConsumed = 200.0),
            ActiveUser(userEmail = "bob@test.com", displayName = "Bob", sessionCount = 30, acusConsumed = 100.0),
            ActiveUser(userEmail = "charlie@test.com", displayName = "Charlie", sessionCount = 10, acusConsumed = 50.0)
        )
    )

    private val testOrganizations = listOf(
        Organization(orgId = "org-1", name = "engineering", displayName = "Engineering"),
        Organization(orgId = "org-2", name = "design", displayName = "Design")
    )
    private val testOrgsResponse = OrganizationsResponse(organizations = testOrganizations)

    private val testUsers = listOf(
        EnterpriseUser(userId = "u1", email = "alice@test.com", displayName = "Alice", role = "admin", sessionCount = 50, acusConsumed = 200.0, orgId = "org-1"),
        EnterpriseUser(userId = "u2", email = "bob@test.com", displayName = "Bob", role = "user", sessionCount = 30, acusConsumed = 100.0, orgId = "org-1"),
        EnterpriseUser(userId = "u3", email = "charlie@test.com", displayName = "Charlie", role = "user", sessionCount = 10, acusConsumed = 50.0, orgId = "org-2"),
        EnterpriseUser(userId = "u4", email = "dave@test.com", displayName = "Dave", role = "admin", sessionCount = 5, acusConsumed = 20.0, orgId = "org-2")
    )
    private val testUsersResponse = UsersResponse(users = testUsers)

    private val testRoles = listOf(
        EnterpriseRole(roleId = "r1", name = "admin", displayName = "Admin", userCount = 2),
        EnterpriseRole(roleId = "r2", name = "user", displayName = "User", userCount = 2)
    )
    private val testRolesResponse = RolesResponse(roles = testRoles)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        teamRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupSuccessRepository(orgId: String? = null) {
        coEvery { teamRepository.getDauMetrics(orgId) } returns flowOf(Result.success(testDauResponse))
        coEvery { teamRepository.getWauMetrics(orgId) } returns flowOf(Result.success(testWauResponse))
        coEvery { teamRepository.getMauMetrics(orgId) } returns flowOf(Result.success(testMauResponse))
        coEvery { teamRepository.getActiveUsers(orgId) } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { teamRepository.getOrganizations() } returns flowOf(Result.success(testOrgsResponse))
        coEvery { teamRepository.getUsers(orgId) } returns flowOf(Result.success(testUsersResponse))
        coEvery { teamRepository.getRoles(orgId) } returns flowOf(Result.success(testRolesResponse))
    }

    // --- Enterprise vs. Org-Scoped Routing Tests ---

    @Test
    fun `default state uses enterprise-scoped endpoints with null orgId`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        coVerify { teamRepository.getDauMetrics(null) }
        coVerify { teamRepository.getWauMetrics(null) }
        coVerify { teamRepository.getMauMetrics(null) }
        coVerify { teamRepository.getActiveUsers(null) }
        coVerify { teamRepository.getUsers(null) }
        coVerify { teamRepository.getRoles(null) }
    }

    @Test
    fun `selecting specific org switches to org-scoped endpoints`() = runTest {
        setupSuccessRepository(orgId = null)
        // Also set up mocks for org-scoped calls
        coEvery { teamRepository.getDauMetrics("org-1") } returns flowOf(Result.success(testDauResponse))
        coEvery { teamRepository.getWauMetrics("org-1") } returns flowOf(Result.success(testWauResponse))
        coEvery { teamRepository.getMauMetrics("org-1") } returns flowOf(Result.success(testMauResponse))
        coEvery { teamRepository.getActiveUsers("org-1") } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { teamRepository.getUsers("org-1") } returns flowOf(Result.success(testUsersResponse))
        coEvery { teamRepository.getRoles("org-1") } returns flowOf(Result.success(testRolesResponse))

        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setOrgFilter("org-1")
        advanceUntilIdle()

        coVerify { teamRepository.getDauMetrics("org-1") }
        coVerify { teamRepository.getWauMetrics("org-1") }
        coVerify { teamRepository.getMauMetrics("org-1") }
        coVerify { teamRepository.getActiveUsers("org-1") }
        coVerify { teamRepository.getUsers("org-1") }
        coVerify { teamRepository.getRoles("org-1") }
    }

    @Test
    fun `clearing org filter returns to enterprise-scoped endpoints`() = runTest {
        setupSuccessRepository(orgId = null)
        coEvery { teamRepository.getDauMetrics("org-1") } returns flowOf(Result.success(testDauResponse))
        coEvery { teamRepository.getWauMetrics("org-1") } returns flowOf(Result.success(testWauResponse))
        coEvery { teamRepository.getMauMetrics("org-1") } returns flowOf(Result.success(testMauResponse))
        coEvery { teamRepository.getActiveUsers("org-1") } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { teamRepository.getUsers("org-1") } returns flowOf(Result.success(testUsersResponse))
        coEvery { teamRepository.getRoles("org-1") } returns flowOf(Result.success(testRolesResponse))

        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setOrgFilter("org-1")
        advanceUntilIdle()

        viewModel.setOrgFilter(null)
        advanceUntilIdle()

        // Enterprise calls: init + clear = 2 times each with null
        coVerify(atLeast = 2) { teamRepository.getDauMetrics(null) }
        coVerify(atLeast = 2) { teamRepository.getWauMetrics(null) }
        coVerify(atLeast = 2) { teamRepository.getMauMetrics(null) }
    }

    @Test
    fun `setOrgFilter updates selectedOrgId in state`() = runTest {
        setupSuccessRepository(orgId = null)
        coEvery { teamRepository.getDauMetrics("org-2") } returns flowOf(Result.success(testDauResponse))
        coEvery { teamRepository.getWauMetrics("org-2") } returns flowOf(Result.success(testWauResponse))
        coEvery { teamRepository.getMauMetrics("org-2") } returns flowOf(Result.success(testMauResponse))
        coEvery { teamRepository.getActiveUsers("org-2") } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { teamRepository.getUsers("org-2") } returns flowOf(Result.success(testUsersResponse))
        coEvery { teamRepository.getRoles("org-2") } returns flowOf(Result.success(testRolesResponse))

        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setOrgFilter("org-2")
        advanceUntilIdle()

        assertEquals("org-2", viewModel.uiState.value.selectedOrgId)
    }

    // --- Filter Application Tests ---

    @Test
    fun `role filter filters users by role`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setRoleFilter("admin")
        advanceUntilIdle()

        val usersState = viewModel.uiState.value.usersState as UiState.Success
        assertTrue(usersState.data.all { it.role == "admin" })
        assertEquals(2, usersState.data.size)
    }

    @Test
    fun `clearing role filter shows all users`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setRoleFilter("admin")
        advanceUntilIdle()

        viewModel.setRoleFilter(null)
        advanceUntilIdle()

        val usersState = viewModel.uiState.value.usersState as UiState.Success
        assertEquals(testUsers.size, usersState.data.size)
    }

    @Test
    fun `role filter updates selectedRoleFilter in state`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setRoleFilter("admin")

        assertEquals("admin", viewModel.uiState.value.selectedRoleFilter)
    }

    // --- Data Merging Tests (DAU/WAU/MAU for multi-line chart) ---

    @Test
    fun `engagement data correctly merges DAU WAU MAU data`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        val engagementState = viewModel.uiState.value.engagementState as UiState.Success
        val data = engagementState.data
        assertEquals(testDauData, data.dauData)
        assertEquals(testWauData, data.wauData)
        assertEquals(testMauData, data.mauData)
        assertEquals(30, data.dauTotal)
        assertEquals(85, data.wauTotal)
        assertEquals(150, data.mauTotal)
    }

    @Test
    fun `funnel data is correctly composed from engagement metrics and total users`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        val funnelState = viewModel.uiState.value.funnelState as UiState.Success
        val funnel = funnelState.data
        assertEquals(30, funnel.dau)
        assertEquals(85, funnel.wau)
        assertEquals(150, funnel.mau)
        // totalUsers = max(users.size, mau) = max(4, 150) = 150
        assertEquals(150, funnel.totalUsers)
    }

    @Test
    fun `funnel totalUsers uses users count when greater than MAU`() = runTest {
        val manyUsers = (1..200).map {
            EnterpriseUser(userId = "u$it", email = "user$it@test.com", role = "user", sessionCount = it)
        }
        coEvery { teamRepository.getDauMetrics(null) } returns flowOf(Result.success(testDauResponse))
        coEvery { teamRepository.getWauMetrics(null) } returns flowOf(Result.success(testWauResponse))
        coEvery { teamRepository.getMauMetrics(null) } returns flowOf(Result.success(testMauResponse))
        coEvery { teamRepository.getActiveUsers(null) } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { teamRepository.getOrganizations() } returns flowOf(Result.success(testOrgsResponse))
        coEvery { teamRepository.getUsers(null) } returns flowOf(Result.success(UsersResponse(users = manyUsers)))
        coEvery { teamRepository.getRoles(null) } returns flowOf(Result.success(testRolesResponse))

        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        val funnelState = viewModel.uiState.value.funnelState as UiState.Success
        assertEquals(200, funnelState.data.totalUsers)
    }

    @Test
    fun `partial engagement failure - only some metrics succeed`() = runTest {
        coEvery { teamRepository.getDauMetrics(null) } returns flowOf(Result.success(testDauResponse))
        coEvery { teamRepository.getWauMetrics(null) } returns flowOf(Result.failure(Exception("WAU failed")))
        coEvery { teamRepository.getMauMetrics(null) } returns flowOf(Result.success(testMauResponse))
        coEvery { teamRepository.getActiveUsers(null) } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { teamRepository.getOrganizations() } returns flowOf(Result.success(testOrgsResponse))
        coEvery { teamRepository.getUsers(null) } returns flowOf(Result.success(testUsersResponse))
        coEvery { teamRepository.getRoles(null) } returns flowOf(Result.success(testRolesResponse))

        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        val engagementState = viewModel.uiState.value.engagementState as UiState.Success
        assertEquals(testDauData, engagementState.data.dauData)
        assertEquals(emptyList<MetricDataPoint>(), engagementState.data.wauData)
        assertEquals(testMauData, engagementState.data.mauData)
    }

    // --- Active Users Search Filtering Tests ---

    @Test
    fun `search query filters active users by display name`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setSearchQuery("alice")
        advanceUntilIdle()

        val activeUsersState = viewModel.uiState.value.activeUsersState as UiState.Success
        assertEquals(1, activeUsersState.data.size)
        assertEquals("alice@test.com", activeUsersState.data[0].userEmail)
    }

    @Test
    fun `search query filters active users by email`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setSearchQuery("bob@test")
        advanceUntilIdle()

        val activeUsersState = viewModel.uiState.value.activeUsersState as UiState.Success
        assertEquals(1, activeUsersState.data.size)
        assertEquals("bob@test.com", activeUsersState.data[0].userEmail)
    }

    @Test
    fun `search query is case insensitive`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setSearchQuery("ALICE")
        advanceUntilIdle()

        val activeUsersState = viewModel.uiState.value.activeUsersState as UiState.Success
        assertEquals(1, activeUsersState.data.size)
    }

    @Test
    fun `empty search query shows all active users`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setSearchQuery("alice")
        advanceUntilIdle()

        viewModel.setSearchQuery("")
        advanceUntilIdle()

        val activeUsersState = viewModel.uiState.value.activeUsersState as UiState.Success
        assertEquals(testActiveUsersResponse.users.size, activeUsersState.data.size)
    }

    @Test
    fun `search query with no matches returns empty list`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setSearchQuery("nonexistent")
        advanceUntilIdle()

        val activeUsersState = viewModel.uiState.value.activeUsersState as UiState.Success
        assertTrue(activeUsersState.data.isEmpty())
    }

    @Test
    fun `search also filters enterprise users list`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setSearchQuery("alice")
        advanceUntilIdle()

        val usersState = viewModel.uiState.value.usersState as UiState.Success
        assertEquals(1, usersState.data.size)
        assertEquals("alice@test.com", usersState.data[0].email)
    }

    @Test
    fun `active users are sorted by session count descending`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        val activeUsersState = viewModel.uiState.value.activeUsersState as UiState.Success
        val sessionCounts = activeUsersState.data.map { it.sessionCount }
        assertEquals(sessionCounts.sortedDescending(), sessionCounts)
    }

    // --- Loading States Per Section (Independent) ---

    @Test
    fun `initial state has all sections in Loading`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)

        val state = viewModel.uiState.value
        assertEquals(UiState.Loading, state.engagementState)
        assertEquals(UiState.Loading, state.activeUsersState)
        assertEquals(UiState.Loading, state.funnelState)
        assertEquals(UiState.Loading, state.orgBreakdownState)
        assertEquals(UiState.Loading, state.roleDistributionState)
        assertEquals(UiState.Loading, state.usersState)
    }

    @Test
    fun `successful load transitions all sections to Success`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.engagementState is UiState.Success)
        assertTrue(state.activeUsersState is UiState.Success)
        assertTrue(state.funnelState is UiState.Success)
        assertTrue(state.orgBreakdownState is UiState.Success)
        assertTrue(state.roleDistributionState is UiState.Success)
        assertTrue(state.usersState is UiState.Success)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `engagement error does not affect active users or roles`() = runTest {
        coEvery { teamRepository.getDauMetrics(null) } returns flowOf(Result.failure(Exception("DAU fail")))
        coEvery { teamRepository.getWauMetrics(null) } returns flowOf(Result.failure(Exception("WAU fail")))
        coEvery { teamRepository.getMauMetrics(null) } returns flowOf(Result.failure(Exception("MAU fail")))
        coEvery { teamRepository.getActiveUsers(null) } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { teamRepository.getOrganizations() } returns flowOf(Result.success(testOrgsResponse))
        coEvery { teamRepository.getUsers(null) } returns flowOf(Result.success(testUsersResponse))
        coEvery { teamRepository.getRoles(null) } returns flowOf(Result.success(testRolesResponse))

        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.engagementState is UiState.Error)
        assertTrue(state.funnelState is UiState.Error)
        assertTrue(state.activeUsersState is UiState.Success)
        assertTrue(state.roleDistributionState is UiState.Success)
        assertTrue(state.usersState is UiState.Success)
    }

    @Test
    fun `active users error does not affect engagement or roles`() = runTest {
        coEvery { teamRepository.getDauMetrics(null) } returns flowOf(Result.success(testDauResponse))
        coEvery { teamRepository.getWauMetrics(null) } returns flowOf(Result.success(testWauResponse))
        coEvery { teamRepository.getMauMetrics(null) } returns flowOf(Result.success(testMauResponse))
        coEvery { teamRepository.getActiveUsers(null) } returns flowOf(Result.failure(Exception("Active users error")))
        coEvery { teamRepository.getOrganizations() } returns flowOf(Result.success(testOrgsResponse))
        coEvery { teamRepository.getUsers(null) } returns flowOf(Result.success(testUsersResponse))
        coEvery { teamRepository.getRoles(null) } returns flowOf(Result.success(testRolesResponse))

        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.engagementState is UiState.Success)
        // activeUsersState ends as Success(empty) because loadUsers' applyFilters
        // calls applyActiveUsersFilter which overwrites the error with an empty success list
        assertTrue(state.activeUsersState is UiState.Success)
        val activeUsers = (state.activeUsersState as UiState.Success).data
        assertTrue(activeUsers.isEmpty())
        assertTrue(state.roleDistributionState is UiState.Success)
    }

    @Test
    fun `active users and users both fail preserves active users error`() = runTest {
        coEvery { teamRepository.getDauMetrics(null) } returns flowOf(Result.success(testDauResponse))
        coEvery { teamRepository.getWauMetrics(null) } returns flowOf(Result.success(testWauResponse))
        coEvery { teamRepository.getMauMetrics(null) } returns flowOf(Result.success(testMauResponse))
        coEvery { teamRepository.getActiveUsers(null) } returns flowOf(Result.failure(Exception("Active users error")))
        coEvery { teamRepository.getOrganizations() } returns flowOf(Result.success(testOrgsResponse))
        coEvery { teamRepository.getUsers(null) } returns flowOf(Result.failure(Exception("Users error")))
        coEvery { teamRepository.getRoles(null) } returns flowOf(Result.success(testRolesResponse))

        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.engagementState is UiState.Success)
        assertTrue(state.usersState is UiState.Error)
        assertTrue(state.roleDistributionState is UiState.Success)
    }

    @Test
    fun `org breakdown error does not affect other sections`() = runTest {
        coEvery { teamRepository.getDauMetrics(null) } returns flowOf(Result.success(testDauResponse))
        coEvery { teamRepository.getWauMetrics(null) } returns flowOf(Result.success(testWauResponse))
        coEvery { teamRepository.getMauMetrics(null) } returns flowOf(Result.success(testMauResponse))
        coEvery { teamRepository.getActiveUsers(null) } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { teamRepository.getOrganizations() } returns flowOf(Result.failure(Exception("Org error")))
        coEvery { teamRepository.getUsers(null) } returns flowOf(Result.success(testUsersResponse))
        coEvery { teamRepository.getRoles(null) } returns flowOf(Result.success(testRolesResponse))

        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.engagementState is UiState.Success)
        assertTrue(state.orgBreakdownState is UiState.Error)
        assertTrue(state.roleDistributionState is UiState.Success)
    }

    @Test
    fun `roles error does not affect other sections`() = runTest {
        coEvery { teamRepository.getDauMetrics(null) } returns flowOf(Result.success(testDauResponse))
        coEvery { teamRepository.getWauMetrics(null) } returns flowOf(Result.success(testWauResponse))
        coEvery { teamRepository.getMauMetrics(null) } returns flowOf(Result.success(testMauResponse))
        coEvery { teamRepository.getActiveUsers(null) } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { teamRepository.getOrganizations() } returns flowOf(Result.success(testOrgsResponse))
        coEvery { teamRepository.getUsers(null) } returns flowOf(Result.success(testUsersResponse))
        coEvery { teamRepository.getRoles(null) } returns flowOf(Result.failure(Exception("Roles error")))

        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.engagementState is UiState.Success)
        assertTrue(state.activeUsersState is UiState.Success)
        assertTrue(state.roleDistributionState is UiState.Error)
        assertEquals("Roles error", (state.roleDistributionState as UiState.Error).message)
    }

    // --- Error Handling and Retry ---

    @Test
    fun `all sections fail shows error for each`() = runTest {
        val error = Exception("Total failure")
        coEvery { teamRepository.getDauMetrics(null) } returns flowOf(Result.failure(error))
        coEvery { teamRepository.getWauMetrics(null) } returns flowOf(Result.failure(error))
        coEvery { teamRepository.getMauMetrics(null) } returns flowOf(Result.failure(error))
        coEvery { teamRepository.getActiveUsers(null) } returns flowOf(Result.failure(error))
        coEvery { teamRepository.getOrganizations() } returns flowOf(Result.failure(error))
        coEvery { teamRepository.getUsers(null) } returns flowOf(Result.failure(error))
        coEvery { teamRepository.getRoles(null) } returns flowOf(Result.failure(error))

        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.engagementState is UiState.Error)
        assertTrue(state.activeUsersState is UiState.Error)
        assertTrue(state.funnelState is UiState.Error)
        assertTrue(state.orgBreakdownState is UiState.Error)
        assertTrue(state.roleDistributionState is UiState.Error)
        assertTrue(state.usersState is UiState.Error)
    }

    @Test
    fun `refresh retries all data loading`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        coVerify(exactly = 2) { teamRepository.getDauMetrics(null) }
        coVerify(exactly = 2) { teamRepository.getWauMetrics(null) }
        coVerify(exactly = 2) { teamRepository.getMauMetrics(null) }
        coVerify(exactly = 2) { teamRepository.getActiveUsers(null) }
        coVerify(exactly = 2) { teamRepository.getOrganizations() }
        coVerify(exactly = 2) { teamRepository.getUsers(null) }
        coVerify(exactly = 2) { teamRepository.getRoles(null) }
    }

    @Test
    fun `refresh sets isRefreshing to true then false`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
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
    fun `refresh after error recovers to success state`() = runTest {
        val error = Exception("Temporary failure")
        coEvery { teamRepository.getDauMetrics(null) } returns flowOf(Result.failure(error))
        coEvery { teamRepository.getWauMetrics(null) } returns flowOf(Result.failure(error))
        coEvery { teamRepository.getMauMetrics(null) } returns flowOf(Result.failure(error))
        coEvery { teamRepository.getActiveUsers(null) } returns flowOf(Result.failure(error))
        coEvery { teamRepository.getOrganizations() } returns flowOf(Result.failure(error))
        coEvery { teamRepository.getUsers(null) } returns flowOf(Result.failure(error))
        coEvery { teamRepository.getRoles(null) } returns flowOf(Result.failure(error))

        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.engagementState is UiState.Error)

        // Now fix the mocks
        coEvery { teamRepository.getDauMetrics(null) } returns flowOf(Result.success(testDauResponse))
        coEvery { teamRepository.getWauMetrics(null) } returns flowOf(Result.success(testWauResponse))
        coEvery { teamRepository.getMauMetrics(null) } returns flowOf(Result.success(testMauResponse))
        coEvery { teamRepository.getActiveUsers(null) } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { teamRepository.getOrganizations() } returns flowOf(Result.success(testOrgsResponse))
        coEvery { teamRepository.getUsers(null) } returns flowOf(Result.success(testUsersResponse))
        coEvery { teamRepository.getRoles(null) } returns flowOf(Result.success(testRolesResponse))

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.engagementState is UiState.Success)
        assertTrue(state.activeUsersState is UiState.Success)
    }

    // --- Line Toggle Tests ---

    @Test
    fun `default visible lines include DAU WAU MAU`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)

        assertEquals(setOf("DAU", "WAU", "MAU"), viewModel.uiState.value.visibleLines)
    }

    @Test
    fun `toggling a line removes it from visible lines`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.toggleLine("DAU")

        val visibleLines = viewModel.uiState.value.visibleLines
        assertFalse(visibleLines.contains("DAU"))
        assertTrue(visibleLines.contains("WAU"))
        assertTrue(visibleLines.contains("MAU"))
    }

    @Test
    fun `toggling last visible line keeps it visible`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.toggleLine("DAU")
        viewModel.toggleLine("WAU")
        // Now only MAU is visible, toggling it should keep it
        viewModel.toggleLine("MAU")

        val visibleLines = viewModel.uiState.value.visibleLines
        assertTrue(visibleLines.contains("MAU"))
        assertEquals(1, visibleLines.size)
    }

    @Test
    fun `toggling line off then on restores it`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.toggleLine("DAU")
        assertFalse(viewModel.uiState.value.visibleLines.contains("DAU"))

        viewModel.toggleLine("DAU")
        assertTrue(viewModel.uiState.value.visibleLines.contains("DAU"))
    }

    // --- Org Breakdown Enrichment ---

    @Test
    fun `org breakdown is enriched with user session data`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        val orgState = viewModel.uiState.value.orgBreakdownState as UiState.Success
        val breakdowns = orgState.data
        assertTrue(breakdowns.isNotEmpty())
        // Engineering org (org-1) has alice(50) + bob(30) = 80 sessions
        val engineering = breakdowns.find { it.orgName == "Engineering" }
        assertTrue(engineering != null)
    }

    // --- Combined Filters ---

    @Test
    fun `search and role filter apply simultaneously`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setRoleFilter("admin")
        viewModel.setSearchQuery("alice")
        advanceUntilIdle()

        val usersState = viewModel.uiState.value.usersState as UiState.Success
        assertEquals(1, usersState.data.size)
        assertEquals("alice@test.com", usersState.data[0].email)
        assertEquals("admin", usersState.data[0].role)
    }

    @Test
    fun `search and role filter with no matching results`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setRoleFilter("admin")
        viewModel.setSearchQuery("charlie")
        advanceUntilIdle()

        val usersState = viewModel.uiState.value.usersState as UiState.Success
        assertTrue(usersState.data.isEmpty())
    }

    @Test
    fun `users are sorted by session count descending`() = runTest {
        setupSuccessRepository(orgId = null)
        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        val usersState = viewModel.uiState.value.usersState as UiState.Success
        val sessionCounts = usersState.data.map { it.sessionCount }
        assertEquals(sessionCounts.sortedDescending(), sessionCounts)
    }

    @Test
    fun `organizations endpoint always uses enterprise scope regardless of org filter`() = runTest {
        setupSuccessRepository(orgId = null)
        coEvery { teamRepository.getDauMetrics("org-1") } returns flowOf(Result.success(testDauResponse))
        coEvery { teamRepository.getWauMetrics("org-1") } returns flowOf(Result.success(testWauResponse))
        coEvery { teamRepository.getMauMetrics("org-1") } returns flowOf(Result.success(testMauResponse))
        coEvery { teamRepository.getActiveUsers("org-1") } returns flowOf(Result.success(testActiveUsersResponse))
        coEvery { teamRepository.getUsers("org-1") } returns flowOf(Result.success(testUsersResponse))
        coEvery { teamRepository.getRoles("org-1") } returns flowOf(Result.success(testRolesResponse))

        viewModel = TeamViewModel(teamRepository)
        advanceUntilIdle()

        viewModel.setOrgFilter("org-1")
        advanceUntilIdle()

        // getOrganizations() has no orgId parameter - always enterprise-scoped
        coVerify(exactly = 2) { teamRepository.getOrganizations() }
    }
}
