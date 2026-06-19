package com.devin.csuite.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devin.csuite.core.Constants
import com.devin.csuite.data.local.SecureKeyStore
import com.devin.csuite.data.remote.ApiException
import com.devin.csuite.domain.model.Organization
import com.devin.csuite.domain.repository.MetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val apiKey: String = "",
    val isLoading: Boolean = false,
    val isValid: Boolean = false,
    val errorMessage: String? = null,
    val organizations: List<Organization> = emptyList()
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val secureKeyStore: SecureKeyStore,
    private val metricsRepository: MetricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateApiKey(key: String) {
        _uiState.value = _uiState.value.copy(
            apiKey = key,
            errorMessage = null
        )
    }

    fun validateAndStore() {
        val key = _uiState.value.apiKey.trim()

        if (!key.startsWith(Constants.KEY_PREFIX)) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "API key must start with '${Constants.KEY_PREFIX}'"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Temporarily store the key for the API call
            secureKeyStore.storeApiKey(key)

            val result = metricsRepository.validateApiKey()

            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isValid = true,
                        organizations = response.organizations
                    )
                },
                onFailure = { error ->
                    secureKeyStore.clearApiKey()
                    val message = when {
                        error is ApiException && (error.code == 401 || error.code == 403) ->
                            "Invalid API key. Please check and try again."
                        error.message?.contains("Unable to resolve host") == true ->
                            "Network error. Please check your connection and try again."
                        else -> error.message ?: "Validation failed. Please try again."
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = message
                    )
                }
            )
        }
    }
}
