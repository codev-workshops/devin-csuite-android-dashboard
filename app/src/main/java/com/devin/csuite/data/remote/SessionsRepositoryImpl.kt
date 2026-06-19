package com.devin.csuite.data.remote

import com.devin.csuite.core.NetworkMonitor
import com.devin.csuite.data.local.datasource.LocalSessionsDataSource
import com.devin.csuite.domain.model.InsightsResponse
import com.devin.csuite.domain.model.Session
import com.devin.csuite.domain.model.SessionsResponse
import com.devin.csuite.domain.repository.SessionsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionsRepositoryImpl @Inject constructor(
    private val api: SessionsApi,
    private val localSessions: LocalSessionsDataSource,
    private val networkMonitor: NetworkMonitor
) : SessionsRepository {

    override fun getSessions(
        first: Int?,
        after: String?,
        status: String?,
        origin: String?,
        user: String?,
        tags: String?,
        createdAfter: Long?,
        createdBefore: Long?
    ): Flow<Result<SessionsResponse>> = flow {
        val hasFilters = after != null || origin != null || user != null || tags != null ||
                createdAfter != null || createdBefore != null
        if (!hasFilters) {
            val cached = localSessions.getSessions(first ?: 100, status)
            if (cached != null) emit(Result.success(cached))
        }

        if (networkMonitor.isConnected.value) {
            val result = safeApiCall {
                api.getSessions(first, after, status, origin, user, tags, createdAfter, createdBefore)
            }
            result.getOrNull()?.let { localSessions.saveSessions(it.sessions) }
            emit(result)
        } else if (hasFilters) {
            emit(Result.failure(OfflineException()))
        }
    }

    override fun getSessionDetail(devinId: String): Flow<Result<Session>> = flow {
        val cached = localSessions.getSession(devinId)
        if (cached != null) emit(Result.success(cached))

        if (networkMonitor.isConnected.value) {
            val result = safeApiCall { api.getSessionDetail(devinId) }
            result.getOrNull()?.let { localSessions.saveSessions(listOf(it)) }
            emit(result)
        } else if (cached == null) {
            emit(Result.failure(OfflineException()))
        }
    }

    override fun generateInsights(devinId: String): Flow<Result<InsightsResponse>> = flow {
        if (!networkMonitor.isConnected.value) {
            emit(Result.failure(OfflineException()))
            return@flow
        }
        emit(safeApiCall { api.generateInsights(devinId) })
    }

    private suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorMessage = when (response.code()) {
                    401, 403 -> "Invalid or expired API key"
                    429 -> "Rate limited. Please try again later"
                    404 -> "Session not found"
                    else -> "API error: ${response.code()} ${response.message()}"
                }
                Result.failure(ApiException(response.code(), errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
