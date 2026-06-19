package com.devin.csuite.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devin.csuite.data.local.PreferencesManager
import com.devin.csuite.data.local.SecureKeyStore
import com.devin.csuite.domain.repository.MetricsRepository
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
    val showReplaceDialog: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val secureKeyStore: SecureKeyStore,
    private val preferencesManager: PreferencesManager,
    private val metricsRepository: MetricsRepository
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

            _uiState.value = _uiState.value.copy(
                maskedApiKey = masked,
                themeMode = theme,
                refreshInterval = interval
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
        _uiState.value = _uiState.value.copy(showReplaceDialog = false)
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
