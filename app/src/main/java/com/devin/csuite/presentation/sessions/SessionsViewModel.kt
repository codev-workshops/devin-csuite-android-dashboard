package com.devin.csuite.presentation.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devin.csuite.core.UiState
import com.devin.csuite.data.local.PreferencesManager
import com.devin.csuite.domain.model.InsightsResponse
import com.devin.csuite.domain.model.Session
import com.devin.csuite.domain.repository.SessionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionFilters(
    val status: String? = null,
    val origins: Set<String> = emptySet(),
    val user: String? = null,
    val tag: String? = null,
    val createdAfter: Long? = null,
    val createdBefore: Long? = null
)

data class StatusDistribution(
    val running: Int = 0,
    val completed: Int = 0,
    val error: Int = 0,
    val suspended: Int = 0,
    val other: Int = 0
) {
    val total: Int get() = running + completed + error + suspended + other
}

data class SessionsUiState(
    val sessionsState: UiState<List<Session>> = UiState.Loading,
    val sessions: List<Session> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
    val filters: SessionFilters = SessionFilters(),
    val activeFilterCount: Int = 0,
    val statusDistribution: StatusDistribution = StatusDistribution(),
    val errorRate: Double = 0.0,
    val totalSessionCount: Int = 0,
    val lastUpdated: Long = 0,

    val selectedSession: UiState<Session> = UiState.Loading,
    val insightsState: UiState<InsightsResponse>? = null
)

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val sessionsRepository: SessionsRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    companion object {
        private const val PAGE_SIZE = 20
    }

    init {
        loadSessions()
        startAutoRefresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadSessions()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.nextCursor == null) return
        _uiState.value = state.copy(isLoadingMore = true)
        loadSessionsPage(state.nextCursor, append = true)
    }

    fun applyFilters(filters: SessionFilters) {
        val filterCount = listOfNotNull(
            filters.status,
            filters.origins.takeIf { it.isNotEmpty() }?.toString(),
            filters.user,
            filters.tag
        ).size

        _uiState.value = _uiState.value.copy(
            filters = filters,
            activeFilterCount = filterCount,
            sessionsState = UiState.Loading
        )
        loadSessions()
    }

    fun clearFilters() {
        applyFilters(SessionFilters())
    }

    fun filterByStatus(status: String?) {
        val currentFilters = _uiState.value.filters
        applyFilters(currentFilters.copy(status = status))
    }

    fun loadSessionDetail(devinId: String) {
        _uiState.value = _uiState.value.copy(selectedSession = UiState.Loading)
        viewModelScope.launch {
            sessionsRepository.getSessionDetail(devinId).firstOrNull()?.let { result ->
                result.fold(
                    onSuccess = { session ->
                        _uiState.value = _uiState.value.copy(
                            selectedSession = UiState.Success(session)
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            selectedSession = UiState.Error(
                                error.message ?: "Failed to load session details"
                            )
                        )
                    }
                )
            }
        }
    }

    fun generateInsights(devinId: String) {
        _uiState.value = _uiState.value.copy(insightsState = UiState.Loading)
        viewModelScope.launch {
            sessionsRepository.generateInsights(devinId).firstOrNull()?.let { result ->
                result.fold(
                    onSuccess = { insights ->
                        _uiState.value = _uiState.value.copy(
                            insightsState = UiState.Success(insights)
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            insightsState = UiState.Error(
                                error.message ?: "Failed to generate insights"
                            )
                        )
                    }
                )
            }
        }
    }

    fun clearInsights() {
        _uiState.value = _uiState.value.copy(insightsState = null)
    }

    private fun loadSessions() {
        loadSessionsPage(cursor = null, append = false)
    }

    private fun loadSessionsPage(cursor: String?, append: Boolean) {
        viewModelScope.launch {
            val filters = _uiState.value.filters
            val originsParam = filters.origins.takeIf { it.isNotEmpty() }?.joinToString(",")

            sessionsRepository.getSessions(
                first = PAGE_SIZE,
                after = cursor,
                status = filters.status,
                origin = originsParam,
                user = filters.user,
                tags = filters.tag,
                createdAfter = filters.createdAfter,
                createdBefore = filters.createdBefore
            ).firstOrNull()?.let { result ->
                result.fold(
                    onSuccess = { response ->
                        val allSessions = if (append) {
                            _uiState.value.sessions + response.sessions
                        } else {
                            response.sessions
                        }
                        val distribution = computeStatusDistribution(allSessions)
                        val errorRate = if (distribution.total > 0) {
                            (distribution.error.toDouble() / distribution.total) * 100.0
                        } else {
                            0.0
                        }

                        _uiState.value = _uiState.value.copy(
                            sessionsState = UiState.Success(allSessions),
                            sessions = allSessions,
                            isRefreshing = false,
                            isLoadingMore = false,
                            hasMore = response.hasMore || response.nextCursor != null,
                            nextCursor = response.nextCursor,
                            statusDistribution = distribution,
                            errorRate = errorRate,
                            totalSessionCount = response.totalCount.takeIf { it > 0 }
                                ?: allSessions.size,
                            lastUpdated = System.currentTimeMillis()
                        )
                    },
                    onFailure = { error ->
                        if (!append) {
                            _uiState.value = _uiState.value.copy(
                                sessionsState = UiState.Error(
                                    error.message ?: "Failed to load sessions"
                                ),
                                isRefreshing = false,
                                isLoadingMore = false
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoadingMore = false
                            )
                        }
                    }
                )
            }
        }
    }

    private fun computeStatusDistribution(sessions: List<Session>): StatusDistribution {
        var running = 0
        var completed = 0
        var error = 0
        var suspended = 0
        var other = 0

        for (session in sessions) {
            when (session.status.lowercase()) {
                "running" -> running++
                "completed", "exit" -> completed++
                "error", "failed" -> error++
                "suspended" -> suspended++
                else -> other++
            }
        }

        return StatusDistribution(
            running = running,
            completed = completed,
            error = error,
            suspended = suspended,
            other = other
        )
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            val intervalMinutes = preferencesManager.refreshInterval.first()
            if (intervalMinutes <= 0) return@launch

            val intervalMs = intervalMinutes * 60 * 1000L
            while (true) {
                delay(intervalMs)
                silentRefresh()
            }
        }
    }

    private fun silentRefresh() {
        viewModelScope.launch {
            val filters = _uiState.value.filters
            val originsParam = filters.origins.takeIf { it.isNotEmpty() }?.joinToString(",")

            sessionsRepository.getSessions(
                first = PAGE_SIZE,
                after = null,
                status = filters.status,
                origin = originsParam,
                user = filters.user,
                tags = filters.tag,
                createdAfter = filters.createdAfter,
                createdBefore = filters.createdBefore
            ).firstOrNull()?.let { result ->
                result.fold(
                    onSuccess = { response ->
                        val distribution = computeStatusDistribution(response.sessions)
                        val errorRate = if (distribution.total > 0) {
                            (distribution.error.toDouble() / distribution.total) * 100.0
                        } else {
                            0.0
                        }

                        _uiState.value = _uiState.value.copy(
                            sessionsState = UiState.Success(response.sessions),
                            sessions = response.sessions,
                            hasMore = response.hasMore || response.nextCursor != null,
                            nextCursor = response.nextCursor,
                            statusDistribution = distribution,
                            errorRate = errorRate,
                            totalSessionCount = response.totalCount.takeIf { it > 0 }
                                ?: response.sessions.size,
                            lastUpdated = System.currentTimeMillis()
                        )
                    },
                    onFailure = { /* silent fail on auto-refresh */ }
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
