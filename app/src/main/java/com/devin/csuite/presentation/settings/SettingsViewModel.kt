package com.devin.csuite.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devin.csuite.data.local.PreferencesManager
import com.devin.csuite.data.local.SecureKeyStore
import com.devin.csuite.data.local.datasource.LocalMetricsDataSource
import com.devin.csuite.data.local.datasource.LocalSessionsDataSource
import com.devin.csuite.domain.repository.MetricsRepository
import com.devin.csuite.notification.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val maskedApiKey: String = "",
    val themeMode: String = "dark",
    val refreshInterval: Int = 0,
    val isValidating: Boolean = false,
    val validationResult: String? = null,
    val showReplaceDialog: Boolean = false,
    val acuOverageEnabled: Boolean = true,
    val errorSpikeEnabled: Boolean = true,
    val guardrailEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val secureKeyStore: SecureKeyStore,
    private val preferencesManager: PreferencesManager,
    private val metricsRepository: MetricsRepository,
    private val localMetrics: LocalMetricsDataSource,
    private val localSessions: LocalSessionsDataSource,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val apiKey = secureKeyStore.getApiKey()
            val masked = apiKey?.let {
                if (it.length > 8) "${it.take(4)}${"*".repeat(it.length - 8)}${it.takeLast(4)}"
                else "****"
            } ?: "Not set"

            val theme = preferencesManager.themeMode.first()
            val interval = preferencesManager.refreshInterval.first()

            val acuEnabled = notificationHelper.isNotificationEnabled(NotificationHelper.CHANNEL_ACU_OVERAGE)
            val errorEnabled = notificationHelper.isNotificationEnabled(NotificationHelper.CHANNEL_ERROR_SPIKE)
            val guardrailEnabled = notificationHelper.isNotificationEnabled(NotificationHelper.CHANNEL_GUARDRAIL)

            _uiState.value = _uiState.value.copy(
                maskedApiKey = masked,
                themeMode = theme,
                refreshInterval = interval,
                acuOverageEnabled = acuEnabled,
                errorSpikeEnabled = errorEnabled,
                guardrailEnabled = guardrailEnabled
            )
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
            _uiState.value = _uiState.value.copy(themeMode = mode)
        }
    }

    fun setRefreshInterval(minutes: Int) {
        viewModelScope.launch {
            preferencesManager.setRefreshInterval(minutes)
            _uiState.value = _uiState.value.copy(refreshInterval = minutes)
        }
    }

    fun showReplaceDialog() {
        _uiState.value = _uiState.value.copy(showReplaceDialog = true)
    }

    fun dismissReplaceDialog() {
        _uiState.value = _uiState.value.copy(showReplaceDialog = false)
    }

    fun replaceApiKey() {
        secureKeyStore.clearApiKey()
        viewModelScope.launch {
            localMetrics.clearAll()
            localSessions.clearAll()
        }
        _uiState.value = _uiState.value.copy(showReplaceDialog = false)
    }

    fun setNotificationEnabled(channelId: String, enabled: Boolean) {
        viewModelScope.launch {
            notificationHelper.setNotificationEnabled(channelId, enabled)
            when (channelId) {
                NotificationHelper.CHANNEL_ACU_OVERAGE ->
                    _uiState.value = _uiState.value.copy(acuOverageEnabled = enabled)
                NotificationHelper.CHANNEL_ERROR_SPIKE ->
                    _uiState.value = _uiState.value.copy(errorSpikeEnabled = enabled)
                NotificationHelper.CHANNEL_GUARDRAIL ->
                    _uiState.value = _uiState.value.copy(guardrailEnabled = enabled)
            }
        }
    }

    fun validateApiKey() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isValidating = true, validationResult = null)
            val result = metricsRepository.validateApiKey()
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isValidating = false,
                        validationResult = "Valid"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isValidating = false,
                        validationResult = "Invalid: ${error.message}"
                    )
                }
            )
        }
    }
}
