package com.devin.csuite.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devin.csuite.core.UiState
import com.devin.csuite.domain.model.ActiveUser
import com.devin.csuite.domain.model.BillingCycle
import com.devin.csuite.domain.model.MetricDataPoint
import com.devin.csuite.domain.model.Session
import com.devin.csuite.core.NetworkMonitor
import com.devin.csuite.data.local.datasource.LocalMetricsDataSource
import com.devin.csuite.domain.repository.MetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val acuState: UiState<BillingCycle> = UiState.Loading,
    val activeSessionsState: UiState<Int> = UiState.Loading,
    val mauState: UiState<Int> = UiState.Loading,
    val prCountState: UiState<Int> = UiState.Loading,
    val sessionChartData: UiState<List<MetricDataPoint>> = UiState.Loading,
    val dauChartData: UiState<List<MetricDataPoint>> = UiState.Loading,
    val topUsers: UiState<List<ActiveUser>> = UiState.Loading,
    val recentSessions: UiState<List<Session>> = UiState.Loading,
    val isRefreshing: Boolean = false,
    val selectedTimeRange: String = "30d",
    val showCriticalAlert: Boolean = false,
    val criticalAlertMessage: String = "",
    val isStale: Boolean = false,
    val isOffline: Boolean = false,
    val lastUpdatedMs: Long? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val metricsRepository: MetricsRepository,
    private val localMetrics: LocalMetricsDataSource,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            networkMonitor.isConnected.collect { connected ->
                _uiState.value = _uiState.value.copy(isOffline = !connected)
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadData()
    }

    fun setTimeRange(range: String) {
        _uiState.value = _uiState.value.copy(selectedTimeRange = range)
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Tier 1: Hero KPIs (parallel)
            val acuDeferred = async { loadAcu() }
            val sessionsDeferred = async { loadActiveSessions() }
            val mauDeferred = async { loadMau() }

            acuDeferred.await()
            sessionsDeferred.await()
            mauDeferred.await()

            // Tier 2: Secondary data (after Tier 1)
            val prDeferred = async { loadPrCount() }
            val sessionChartDeferred = async { loadSessionChart() }
            val dauDeferred = async { loadDauChart() }
            val topUsersDeferred = async { loadTopUsers() }
            val recentDeferred = async { loadRecentSessions() }

            prDeferred.await()
            sessionChartDeferred.await()
            dauDeferred.await()
            topUsersDeferred.await()
            recentDeferred.await()

            val lastUpdated = localMetrics.getLastUpdated("organizations")
            val staleThreshold = 15 * 60 * 1000L
            val isStale = lastUpdated != null && (System.currentTimeMillis() - lastUpdated) > staleThreshold
            _uiState.value = _uiState.value.copy(
                isRefreshing = false,
                lastUpdatedMs = lastUpdated ?: System.currentTimeMillis(),
                isStale = isStale
            )
        }
    }

    private suspend fun loadAcu() {
        metricsRepository.getBillingCycles().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    val current = response.currentCycle ?: response.cycles.firstOrNull()
                    if (current != null) {
                        val usagePercent = if (current.acuLimit > 0) {
                            (current.acusUsed / current.acuLimit) * 100
                        } else 0.0
                        val showAlert = usagePercent > 80
                        _uiState.value = _uiState.value.copy(
                            acuState = UiState.Success(current),
                            showCriticalAlert = showAlert,
                            criticalAlertMessage = if (showAlert) {
                                "ACU usage at ${String.format("%.0f", usagePercent)}% of limit"
                            } else ""
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            acuState = UiState.Success(BillingCycle())
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        acuState = UiState.Error(error.message ?: "Failed to load ACU data")
                    )
                }
            )
        }
    }

    private suspend fun loadActiveSessions() {
        metricsRepository.getSessions(limit = 100, status = "running").firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        activeSessionsState = UiState.Success(response.totalCount)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        activeSessionsState = UiState.Error(error.message ?: "Failed to load sessions")
                    )
                }
            )
        }
    }

    private suspend fun loadMau() {
        metricsRepository.getMauMetrics().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        mauState = UiState.Success(response.mau)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        mauState = UiState.Error(error.message ?: "Failed to load MAU")
                    )
                }
            )
        }
    }

    private suspend fun loadPrCount() {
        metricsRepository.getPrMetrics().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        prCountState = UiState.Success(response.totalPrs)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        prCountState = UiState.Error(error.message ?: "Failed to load PR metrics")
                    )
                }
            )
        }
    }

    private suspend fun loadSessionChart() {
        metricsRepository.getSessionMetrics().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        sessionChartData = UiState.Success(response.data)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        sessionChartData = UiState.Error(error.message ?: "Failed to load session chart")
                    )
                }
            )
        }
    }

    private suspend fun loadDauChart() {
        metricsRepository.getDauMetrics().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        dauChartData = UiState.Success(response.data)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        dauChartData = UiState.Error(error.message ?: "Failed to load DAU chart")
                    )
                }
            )
        }
    }

    private suspend fun loadTopUsers() {
        metricsRepository.getActiveUsers().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        topUsers = UiState.Success(response.users.take(5))
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        topUsers = UiState.Error(error.message ?: "Failed to load top users")
                    )
                }
            )
        }
    }

    private suspend fun loadRecentSessions() {
        metricsRepository.getSessions(limit = 5).firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        recentSessions = UiState.Success(response.sessions.take(5))
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        recentSessions = UiState.Error(error.message ?: "Failed to load recent sessions")
                    )
                }
            )
        }
    }
}
