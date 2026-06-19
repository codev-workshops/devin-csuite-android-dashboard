package com.devin.csuite.presentation.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devin.csuite.core.UiState
import com.devin.csuite.domain.model.ActiveUser
import com.devin.csuite.domain.model.EnterpriseRole
import com.devin.csuite.domain.model.EnterpriseUser
import com.devin.csuite.domain.model.MetricDataPoint
import com.devin.csuite.domain.model.Organization
import com.devin.csuite.domain.repository.TeamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EngagementData(
    val dauData: List<MetricDataPoint> = emptyList(),
    val wauData: List<MetricDataPoint> = emptyList(),
    val mauData: List<MetricDataPoint> = emptyList(),
    val dauTotal: Int = 0,
    val wauTotal: Int = 0,
    val mauTotal: Int = 0
)

data class FunnelData(
    val totalUsers: Int = 0,
    val mau: Int = 0,
    val wau: Int = 0,
    val dau: Int = 0
)

data class OrgBreakdown(
    val orgName: String,
    val sessionCount: Int,
    val acuUsage: Double
)

data class TeamUiState(
    val engagementState: UiState<EngagementData> = UiState.Loading,
    val activeUsersState: UiState<List<ActiveUser>> = UiState.Loading,
    val funnelState: UiState<FunnelData> = UiState.Loading,
    val orgBreakdownState: UiState<List<OrgBreakdown>> = UiState.Loading,
    val roleDistributionState: UiState<List<EnterpriseRole>> = UiState.Loading,
    val usersState: UiState<List<EnterpriseUser>> = UiState.Loading,
    val isRefreshing: Boolean = false,
    val selectedOrgId: String? = null,
    val selectedRoleFilter: String? = null,
    val searchQuery: String = "",
    val visibleLines: Set<String> = setOf("DAU", "WAU", "MAU")
)

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val teamRepository: TeamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    private var allUsers: List<EnterpriseUser> = emptyList()
    private var allActiveUsers: List<ActiveUser> = emptyList()
    private var allRoles: List<EnterpriseRole> = emptyList()

    init {
        loadData()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadData()
    }

    fun setOrgFilter(orgId: String?) {
        _uiState.value = _uiState.value.copy(selectedOrgId = orgId)
        loadData()
    }

    fun setRoleFilter(role: String?) {
        _uiState.value = _uiState.value.copy(selectedRoleFilter = role)
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun toggleLine(line: String) {
        val current = _uiState.value.visibleLines.toMutableSet()
        if (current.contains(line)) {
            if (current.size > 1) current.remove(line)
        } else {
            current.add(line)
        }
        _uiState.value = _uiState.value.copy(visibleLines = current)
    }

    private fun loadData() {
        val orgId = _uiState.value.selectedOrgId

        viewModelScope.launch {
            val engagementDeferred = async { loadEngagement(orgId) }
            val activeUsersDeferred = async { loadActiveUsers(orgId) }
            val orgsDeferred = async { loadOrgBreakdown() }
            val usersDeferred = async { loadUsers(orgId) }
            val rolesDeferred = async { loadRoles(orgId) }

            engagementDeferred.await()
            activeUsersDeferred.await()
            orgsDeferred.await()
            usersDeferred.await()
            rolesDeferred.await()

            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    private suspend fun loadEngagement(orgId: String?) {
        val dauResult = teamRepository.getDauMetrics(orgId).firstOrNull()
        val wauResult = teamRepository.getWauMetrics(orgId).firstOrNull()
        val mauResult = teamRepository.getMauMetrics(orgId).firstOrNull()

        val dauResponse = dauResult?.getOrNull()
        val wauResponse = wauResult?.getOrNull()
        val mauResponse = mauResult?.getOrNull()

        if (dauResponse != null || wauResponse != null || mauResponse != null) {
            val engagement = EngagementData(
                dauData = dauResponse?.data ?: emptyList(),
                wauData = wauResponse?.data ?: emptyList(),
                mauData = mauResponse?.data ?: emptyList(),
                dauTotal = dauResponse?.dau ?: 0,
                wauTotal = wauResponse?.wau ?: 0,
                mauTotal = mauResponse?.mau ?: 0
            )
            _uiState.value = _uiState.value.copy(
                engagementState = UiState.Success(engagement),
                funnelState = UiState.Success(
                    FunnelData(
                        totalUsers = allUsers.size.coerceAtLeast(mauResponse?.mau ?: 0),
                        mau = mauResponse?.mau ?: 0,
                        wau = wauResponse?.wau ?: 0,
                        dau = dauResponse?.dau ?: 0
                    )
                )
            )
        } else {
            val errorMsg = dauResult?.exceptionOrNull()?.message
                ?: wauResult?.exceptionOrNull()?.message
                ?: mauResult?.exceptionOrNull()?.message
                ?: "Failed to load engagement data"
            _uiState.value = _uiState.value.copy(
                engagementState = UiState.Error(errorMsg),
                funnelState = UiState.Error(errorMsg)
            )
        }
    }

    private suspend fun loadActiveUsers(orgId: String?) {
        teamRepository.getActiveUsers(orgId).firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    allActiveUsers = response.users
                    applyActiveUsersFilter()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        activeUsersState = UiState.Error(
                            error.message ?: "Failed to load active users"
                        )
                    )
                }
            )
        }
    }

    private suspend fun loadOrgBreakdown() {
        teamRepository.getOrganizations().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    val breakdowns = response.organizations.map { org ->
                        OrgBreakdown(
                            orgName = org.displayName ?: org.name,
                            sessionCount = 0,
                            acuUsage = 0.0
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        orgBreakdownState = UiState.Success(breakdowns)
                    )
                    enrichOrgBreakdown(response.organizations)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        orgBreakdownState = UiState.Error(
                            error.message ?: "Failed to load organizations"
                        )
                    )
                }
            )
        }
    }

    private suspend fun enrichOrgBreakdown(organizations: List<Organization>) {
        if (allUsers.isEmpty()) return
        val breakdowns = organizations.map { org ->
            val orgUsers = allUsers.filter { it.orgId == org.orgId }
            OrgBreakdown(
                orgName = org.displayName ?: org.name,
                sessionCount = orgUsers.sumOf { it.sessionCount },
                acuUsage = orgUsers.sumOf { it.acusConsumed }
            )
        }.sortedByDescending { it.sessionCount }
        _uiState.value = _uiState.value.copy(
            orgBreakdownState = UiState.Success(breakdowns)
        )
    }

    private suspend fun loadUsers(orgId: String?) {
        teamRepository.getUsers(orgId).firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    allUsers = response.users
                    applyFilters()
                    updateFunnelWithTotalUsers(response.users.size)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        usersState = UiState.Error(
                            error.message ?: "Failed to load users"
                        )
                    )
                }
            )
        }
    }

    private suspend fun loadRoles(orgId: String?) {
        teamRepository.getRoles(orgId).firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    allRoles = response.roles
                    _uiState.value = _uiState.value.copy(
                        roleDistributionState = UiState.Success(response.roles)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        roleDistributionState = UiState.Error(
                            error.message ?: "Failed to load roles"
                        )
                    )
                }
            )
        }
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery.lowercase()
        val roleFilter = _uiState.value.selectedRoleFilter
        val filtered = allUsers.filter { user ->
            val matchesSearch = query.isEmpty() ||
                    (user.displayName?.lowercase()?.contains(query) == true) ||
                    user.email.lowercase().contains(query)
            val matchesRole = roleFilter == null || user.role == roleFilter
            matchesSearch && matchesRole
        }.sortedByDescending { it.sessionCount }
        _uiState.value = _uiState.value.copy(
            usersState = UiState.Success(filtered)
        )
        applyActiveUsersFilter()
    }

    private fun applyActiveUsersFilter() {
        val query = _uiState.value.searchQuery.lowercase()
        val filtered = allActiveUsers.filter { user ->
            query.isEmpty() ||
                    (user.displayName?.lowercase()?.contains(query) == true) ||
                    user.userEmail.lowercase().contains(query)
        }.sortedByDescending { it.sessionCount }
        _uiState.value = _uiState.value.copy(
            activeUsersState = UiState.Success(filtered)
        )
    }

    private fun updateFunnelWithTotalUsers(totalUsers: Int) {
        val currentFunnel = (_uiState.value.funnelState as? UiState.Success)?.data
        if (currentFunnel != null) {
            _uiState.value = _uiState.value.copy(
                funnelState = UiState.Success(
                    currentFunnel.copy(
                        totalUsers = totalUsers.coerceAtLeast(currentFunnel.mau)
                    )
                )
            )
        }
    }
}
