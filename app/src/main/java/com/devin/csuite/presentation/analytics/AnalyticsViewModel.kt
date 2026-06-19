package com.devin.csuite.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devin.csuite.core.UiState
import com.devin.csuite.domain.model.AcuLimitsResponse
import com.devin.csuite.domain.model.DailyConsumption
import com.devin.csuite.domain.model.MetricDataPoint
import com.devin.csuite.domain.model.SessionMetricsResponse
import com.devin.csuite.domain.repository.MetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val consumptionState: UiState<List<DailyConsumption>> = UiState.Loading,
    val acuLimitsState: UiState<AcuLimitsResponse> = UiState.Loading,
    val sessionMetricsState: UiState<SessionMetricsResponse> = UiState.Loading,
    val prChartData: UiState<PrChartData> = UiState.Loading,
    val searchChartData: UiState<List<MetricDataPoint>> = UiState.Loading,
    val isRefreshing: Boolean = false,
    val selectedOrigins: Set<String> = emptySet(),
    val showOriginFilter: Boolean = false
)

data class PrChartData(
    val totalPrs: Int,
    val mergedPrs: Int,
    val data: List<MetricDataPoint>
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val metricsRepository: MetricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    companion object {
        val ORIGIN_OPTIONS = listOf(
            "Webapp", "Slack", "Teams", "API", "CLI", "Linear", "Jira", "Scheduled"
        )
    }

    init {
        loadData()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadData()
    }

    fun toggleOriginFilter() {
        _uiState.value = _uiState.value.copy(showOriginFilter = !_uiState.value.showOriginFilter)
    }

    fun toggleOrigin(origin: String) {
        val current = _uiState.value.selectedOrigins.toMutableSet()
        if (current.contains(origin)) {
            current.remove(origin)
        } else {
            current.add(origin)
        }
        _uiState.value = _uiState.value.copy(selectedOrigins = current)
    }

    fun clearOrigins() {
        _uiState.value = _uiState.value.copy(selectedOrigins = emptySet())
    }

    private fun loadData() {
        viewModelScope.launch {
            // Tier 1: ACU limits + consumption (parallel)
            val consumptionDeferred = async { loadConsumption() }
            val acuLimitsDeferred = async { loadAcuLimits() }

            consumptionDeferred.await()
            acuLimitsDeferred.await()

            // Tier 2: Sessions, PRs, Searches (parallel)
            val sessionsDeferred = async { loadSessionMetrics() }
            val prsDeferred = async { loadPrChart() }
            val searchesDeferred = async { loadSearchChart() }

            sessionsDeferred.await()
            prsDeferred.await()
            searchesDeferred.await()

            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    private suspend fun loadConsumption() {
        metricsRepository.getDailyConsumption().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        consumptionState = UiState.Success(response.daily)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        consumptionState = UiState.Error(
                            error.message ?: "Failed to load consumption data"
                        )
                    )
                }
            )
        }
    }

    private suspend fun loadAcuLimits() {
        metricsRepository.getAcuLimits().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        acuLimitsState = UiState.Success(response)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        acuLimitsState = UiState.Error(
                            error.message ?: "Failed to load ACU limits"
                        )
                    )
                }
            )
        }
    }

    private suspend fun loadSessionMetrics() {
        metricsRepository.getSessionMetrics().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        sessionMetricsState = UiState.Success(response)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        sessionMetricsState = UiState.Error(
                            error.message ?: "Failed to load session metrics"
                        )
                    )
                }
            )
        }
    }

    private suspend fun loadPrChart() {
        metricsRepository.getPrMetrics().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        prChartData = UiState.Success(
                            PrChartData(
                                totalPrs = response.totalPrs,
                                mergedPrs = response.mergedPrs,
                                data = response.data
                            )
                        )
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        prChartData = UiState.Error(
                            error.message ?: "Failed to load PR metrics"
                        )
                    )
                }
            )
        }
    }

    private suspend fun loadSearchChart() {
        metricsRepository.getSearchMetrics().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        searchChartData = UiState.Success(response.data)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        searchChartData = UiState.Error(
                            error.message ?: "Failed to load search metrics"
                        )
                    )
                }
            )
        }
    }
}
