package com.devin.csuite.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devin.csuite.core.UiState
import com.devin.csuite.data.remote.ApiException
import com.devin.csuite.domain.model.security.AuditLogEntry
import com.devin.csuite.domain.model.security.GuardrailViolation
import com.devin.csuite.domain.model.security.IpAccessEntry
import com.devin.csuite.domain.model.security.ViolationTrendPoint
import com.devin.csuite.domain.repository.security.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityUiState(
    val auditLogsState: UiState<List<AuditLogEntry>> = UiState.Loading,
    val guardrailState: UiState<List<GuardrailViolation>> = UiState.Loading,
    val ipAccessState: UiState<List<IpAccessEntry>> = UiState.Loading,
    val violationTrendState: UiState<List<ViolationTrendPoint>> = UiState.Loading,
    val guardrailAvailable: Boolean = true,
    val isRefreshing: Boolean = false,
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val auditLogCursor: String? = null,
    val auditLogHasMore: Boolean = false,
    val isLoadingMoreAuditLogs: Boolean = false,
    val guardrailViolations: List<GuardrailViolation> = emptyList(),
    val ipAddresses: List<IpAccessEntry> = emptyList(),
    val expandedAuditLogId: String? = null,
    val selectedActionTypeFilter: String? = null,
    val selectedSeverityFilter: String? = null,
    val showAddIpSheet: Boolean = false,
    val showFilterSheet: Boolean = false,
    val addIpError: String? = null,
    val addIpLoading: Boolean = false,
    val removeIpLoading: String? = null,
    val showConfirmAddIp: Boolean = false,
    val pendingAddIp: String = "",
    val pendingAddIpDescription: String = "",
    val showConfirmRemoveIp: String? = null,
    val ipMutationError: String? = null,
    val selectedTab: SecurityTab = SecurityTab.AUDIT_LOGS
)

enum class SecurityTab(val title: String) {
    AUDIT_LOGS("Audit Logs"),
    GUARDRAILS("Guardrails"),
    IP_ACCESS("IP Access")
}

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityRepository: SecurityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadAllData()
    }

    fun selectTab(tab: SecurityTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    private fun loadAllData() {
        viewModelScope.launch {
            val auditDeferred = async { loadAuditLogs(reset = true) }
            val guardrailDeferred = async { loadGuardrailViolations() }
            val ipDeferred = async { loadIpAccessList() }

            auditDeferred.await()
            guardrailDeferred.await()
            ipDeferred.await()

            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    private suspend fun loadAuditLogs(reset: Boolean) {
        val cursor = if (reset) null else _uiState.value.auditLogCursor
        val actionType = _uiState.value.selectedActionTypeFilter

        securityRepository.getAuditLogs(
            first = 20,
            after = cursor,
            actionType = actionType
        ).firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    val currentLogs = if (reset) emptyList() else _uiState.value.auditLogs
                    val allLogs = currentLogs + response.auditLogs
                    _uiState.value = _uiState.value.copy(
                        auditLogsState = UiState.Success(allLogs),
                        auditLogs = allLogs,
                        auditLogCursor = response.nextCursor,
                        auditLogHasMore = response.hasMore,
                        isLoadingMoreAuditLogs = false
                    )
                },
                onFailure = { error ->
                    if (reset) {
                        _uiState.value = _uiState.value.copy(
                            auditLogsState = UiState.Error(error.message ?: "Failed to load audit logs"),
                            isLoadingMoreAuditLogs = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoadingMoreAuditLogs = false)
                    }
                }
            )
        }
    }

    fun loadMoreAuditLogs() {
        if (_uiState.value.isLoadingMoreAuditLogs || !_uiState.value.auditLogHasMore) return
        _uiState.value = _uiState.value.copy(isLoadingMoreAuditLogs = true)
        viewModelScope.launch {
            loadAuditLogs(reset = false)
        }
    }

    private suspend fun loadGuardrailViolations() {
        val severity = _uiState.value.selectedSeverityFilter

        securityRepository.getGuardrailViolations(
            first = 50,
            severity = severity
        ).firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    val violations = response.violations
                    _uiState.value = _uiState.value.copy(
                        guardrailState = UiState.Success(violations),
                        guardrailViolations = violations,
                        guardrailAvailable = true,
                        violationTrendState = UiState.Success(buildTrendData(violations))
                    )
                },
                onFailure = { error ->
                    val isUnavailable = error is ApiException &&
                        (error.code == 404 || error.code == 501)
                    _uiState.value = _uiState.value.copy(
                        guardrailState = if (isUnavailable) {
                            UiState.Success(emptyList())
                        } else {
                            UiState.Error(error.message ?: "Failed to load guardrail violations")
                        },
                        guardrailAvailable = !isUnavailable,
                        guardrailViolations = emptyList(),
                        violationTrendState = if (isUnavailable) {
                            UiState.Success(emptyList())
                        } else {
                            UiState.Error("Failed to load trend data")
                        }
                    )
                }
            )
        }
    }

    private fun buildTrendData(violations: List<GuardrailViolation>): List<ViolationTrendPoint> {
        if (violations.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000
        val recent = violations.filter { it.timestamp >= thirtyDaysAgo }
        return recent
            .groupBy { dayKey(it.timestamp) }
            .map { (date, items) -> ViolationTrendPoint(date = date, count = items.size) }
            .sortedBy { it.date }
    }

    private fun dayKey(timestampMs: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }
        val y = cal.get(java.util.Calendar.YEAR)
        val m = cal.get(java.util.Calendar.MONTH) + 1
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
    }

    private suspend fun loadIpAccessList() {
        securityRepository.getIpAccessList().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        ipAccessState = UiState.Success(response.ipAddresses),
                        ipAddresses = response.ipAddresses
                    )
                },
                onFailure = { error ->
                    val isForbidden = error is ApiException && error.code == 403
                    _uiState.value = _uiState.value.copy(
                        ipAccessState = if (isForbidden) {
                            UiState.Error("Insufficient permissions to view IP access list")
                        } else {
                            UiState.Error(error.message ?: "Failed to load IP access list")
                        }
                    )
                }
            )
        }
    }

    fun toggleAuditLogExpanded(logId: String) {
        val current = _uiState.value.expandedAuditLogId
        _uiState.value = _uiState.value.copy(
            expandedAuditLogId = if (current == logId) null else logId
        )
    }

    fun setActionTypeFilter(actionType: String?) {
        _uiState.value = _uiState.value.copy(selectedActionTypeFilter = actionType)
        viewModelScope.launch { loadAuditLogs(reset = true) }
    }

    fun setSeverityFilter(severity: String?) {
        _uiState.value = _uiState.value.copy(selectedSeverityFilter = severity)
        viewModelScope.launch { loadGuardrailViolations() }
    }

    fun showAddIpSheet() {
        _uiState.value = _uiState.value.copy(
            showAddIpSheet = true,
            addIpError = null,
            pendingAddIp = "",
            pendingAddIpDescription = ""
        )
    }

    fun dismissAddIpSheet() {
        _uiState.value = _uiState.value.copy(
            showAddIpSheet = false,
            showConfirmAddIp = false,
            addIpError = null
        )
    }

    fun showFilterSheet() {
        _uiState.value = _uiState.value.copy(showFilterSheet = true)
    }

    fun dismissFilterSheet() {
        _uiState.value = _uiState.value.copy(showFilterSheet = false)
    }

    fun requestAddIp(ip: String, description: String) {
        val validation = IpValidator.validate(ip)
        if (validation is IpValidator.ValidationResult.Invalid) {
            _uiState.value = _uiState.value.copy(addIpError = validation.reason)
            return
        }
        _uiState.value = _uiState.value.copy(
            showConfirmAddIp = true,
            pendingAddIp = ip,
            pendingAddIpDescription = description,
            addIpError = null
        )
    }

    fun confirmAddIp() {
        val ip = _uiState.value.pendingAddIp
        val desc = _uiState.value.pendingAddIpDescription.ifBlank { null }
        _uiState.value = _uiState.value.copy(
            addIpLoading = true,
            showConfirmAddIp = false,
            ipMutationError = null
        )
        viewModelScope.launch {
            val result = securityRepository.addIpAddress(ip, desc)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        addIpLoading = false,
                        showAddIpSheet = false,
                        ipAccessState = UiState.Success(response.ipAddresses),
                        ipAddresses = response.ipAddresses
                    )
                },
                onFailure = { error ->
                    val isForbidden = error is ApiException && error.code == 403
                    _uiState.value = _uiState.value.copy(
                        addIpLoading = false,
                        ipMutationError = if (isForbidden) {
                            "Insufficient permissions"
                        } else {
                            error.message ?: "Failed to add IP"
                        }
                    )
                }
            )
        }
    }

    fun cancelAddIp() {
        _uiState.value = _uiState.value.copy(showConfirmAddIp = false)
    }

    fun requestRemoveIp(ip: String) {
        _uiState.value = _uiState.value.copy(showConfirmRemoveIp = ip, ipMutationError = null)
    }

    fun confirmRemoveIp() {
        val ip = _uiState.value.showConfirmRemoveIp ?: return
        _uiState.value = _uiState.value.copy(
            removeIpLoading = ip,
            showConfirmRemoveIp = null,
            ipMutationError = null
        )
        viewModelScope.launch {
            val result = securityRepository.removeIpAddress(ip)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        removeIpLoading = null,
                        ipAccessState = UiState.Success(response.ipAddresses),
                        ipAddresses = response.ipAddresses
                    )
                },
                onFailure = { error ->
                    val isForbidden = error is ApiException && error.code == 403
                    _uiState.value = _uiState.value.copy(
                        removeIpLoading = null,
                        ipMutationError = if (isForbidden) {
                            "Insufficient permissions"
                        } else {
                            error.message ?: "Failed to remove IP"
                        }
                    )
                }
            )
        }
    }

    fun cancelRemoveIp() {
        _uiState.value = _uiState.value.copy(showConfirmRemoveIp = null)
    }

    fun dismissMutationError() {
        _uiState.value = _uiState.value.copy(ipMutationError = null)
    }
}
