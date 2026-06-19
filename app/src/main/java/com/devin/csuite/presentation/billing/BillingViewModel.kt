package com.devin.csuite.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devin.csuite.core.UiState
import com.devin.csuite.data.remote.ApiException
import com.devin.csuite.domain.model.AcuLimitsResponse
import com.devin.csuite.domain.model.BillingCycle
import com.devin.csuite.domain.model.BillingCyclesResponse
import com.devin.csuite.domain.model.DailyConsumption
import com.devin.csuite.domain.repository.MetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BillingUiState(
    val cyclesState: UiState<BillingCyclesResponse> = UiState.Loading,
    val dailyConsumptionState: UiState<List<DailyConsumption>> = UiState.Loading,
    val acuLimitsState: UiState<AcuLimitsResponse> = UiState.Loading,
    val selectedCycleIndex: Int = 0,
    val isRefreshing: Boolean = false,
    val projection: ProjectionData? = null,
    val showEditLimitSheet: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val editLimitValue: String = "",
    val updateLimitState: UiState<Unit>? = null,
    val permissionError: String? = null
)

data class ProjectionData(
    val dailyAvg: Double,
    val remainingDays: Int,
    val currentUsed: Double,
    val projectedTotal: Double,
    val limit: Double
) {
    val projectedPercentage: Double
        get() = if (limit > 0) (projectedTotal / limit) * 100 else 0.0

    val isOverBudget: Boolean
        get() = projectedTotal > limit
}

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val metricsRepository: MetricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadData()
    }

    fun selectCycle(index: Int) {
        _uiState.value = _uiState.value.copy(selectedCycleIndex = index)
    }

    fun showEditLimitSheet() {
        val currentLimit = (_uiState.value.acuLimitsState as? UiState.Success)?.data?.acuLimit ?: 0.0
        _uiState.value = _uiState.value.copy(
            showEditLimitSheet = true,
            editLimitValue = String.format("%.0f", currentLimit),
            permissionError = null,
            updateLimitState = null
        )
    }

    fun dismissEditLimitSheet() {
        _uiState.value = _uiState.value.copy(
            showEditLimitSheet = false,
            showConfirmDialog = false,
            permissionError = null,
            updateLimitState = null
        )
    }

    fun updateEditLimitValue(value: String) {
        _uiState.value = _uiState.value.copy(editLimitValue = value)
    }

    fun requestConfirmation() {
        _uiState.value = _uiState.value.copy(showConfirmDialog = true)
    }

    fun dismissConfirmation() {
        _uiState.value = _uiState.value.copy(showConfirmDialog = false)
    }

    fun confirmUpdateLimit() {
        val newLimit = _uiState.value.editLimitValue.toDoubleOrNull() ?: return
        _uiState.value = _uiState.value.copy(
            showConfirmDialog = false,
            updateLimitState = UiState.Loading
        )

        viewModelScope.launch {
            val result = metricsRepository.updateAcuLimits(newLimit)
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        acuLimitsState = UiState.Success(response),
                        updateLimitState = UiState.Success(Unit),
                        showEditLimitSheet = false
                    )
                    computeProjection()
                },
                onFailure = { error ->
                    val message = if (error is ApiException && error.code == 403) {
                        "Insufficient permissions to update ACU limits"
                    } else {
                        error.message ?: "Failed to update ACU limits"
                    }
                    _uiState.value = _uiState.value.copy(
                        permissionError = message,
                        updateLimitState = UiState.Error(message)
                    )
                }
            )
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val cyclesDeferred = async { loadCycles() }
            val dailyDeferred = async { loadDailyConsumption() }
            val limitsDeferred = async { loadAcuLimits() }

            cyclesDeferred.await()
            dailyDeferred.await()
            limitsDeferred.await()

            computeProjection()

            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    private suspend fun loadCycles() {
        metricsRepository.getBillingCycles().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        cyclesState = UiState.Success(response)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        cyclesState = UiState.Error(
                            error.message ?: "Failed to load billing cycles"
                        )
                    )
                }
            )
        }
    }

    private suspend fun loadDailyConsumption() {
        metricsRepository.getDailyConsumption().firstOrNull()?.let { result ->
            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        dailyConsumptionState = UiState.Success(response.daily)
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        dailyConsumptionState = UiState.Error(
                            error.message ?: "Failed to load daily consumption"
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

    private fun computeProjection() {
        val daily = (_uiState.value.dailyConsumptionState as? UiState.Success)?.data ?: return
        val limits = (_uiState.value.acuLimitsState as? UiState.Success)?.data ?: return
        val cycles = (_uiState.value.cyclesState as? UiState.Success)?.data ?: return

        val currentCycle = cycles.currentCycle ?: cycles.cycles.firstOrNull() ?: return

        if (daily.isEmpty()) return

        val dailyAvg = daily.takeLast(7).map { it.acusConsumed }.average()
        val remainingDays = estimateRemainingDays(currentCycle)
        val projectedTotal = (dailyAvg * remainingDays) + limits.acusUsed

        _uiState.value = _uiState.value.copy(
            projection = ProjectionData(
                dailyAvg = dailyAvg,
                remainingDays = remainingDays,
                currentUsed = limits.acusUsed,
                projectedTotal = projectedTotal,
                limit = limits.acuLimit
            )
        )
    }

    private fun estimateRemainingDays(cycle: BillingCycle): Int {
        return try {
            val end = java.time.LocalDate.parse(cycle.cycleEnd.take(10))
            val now = java.time.LocalDate.now()
            java.time.temporal.ChronoUnit.DAYS.between(now, end).toInt().coerceAtLeast(0)
        } catch (_: Exception) {
            14
        }
    }
}
