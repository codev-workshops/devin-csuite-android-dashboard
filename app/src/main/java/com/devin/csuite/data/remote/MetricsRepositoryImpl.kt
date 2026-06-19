package com.devin.csuite.data.remote

import com.devin.csuite.core.NetworkMonitor
import com.devin.csuite.data.local.datasource.LocalMetricsDataSource
import com.devin.csuite.data.local.datasource.LocalSessionsDataSource
import com.devin.csuite.domain.model.AcuLimitUpdateRequest
import com.devin.csuite.domain.model.AcuLimitsResponse
import com.devin.csuite.domain.model.ActiveUsersResponse
import com.devin.csuite.domain.model.BillingCyclesResponse
import com.devin.csuite.domain.model.DailyConsumptionResponse
import com.devin.csuite.domain.model.DauMetricsResponse
import com.devin.csuite.domain.model.MauMetricsResponse
import com.devin.csuite.domain.model.OrganizationsResponse
import com.devin.csuite.domain.model.PrMetricsResponse
import com.devin.csuite.domain.model.SearchMetricsResponse
import com.devin.csuite.domain.model.SessionMetricsResponse
import com.devin.csuite.domain.model.SessionsResponse
import com.devin.csuite.domain.repository.MetricsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetricsRepositoryImpl @Inject constructor(
    private val api: EnterpriseApi,
    private val localMetrics: LocalMetricsDataSource,
    private val localSessions: LocalSessionsDataSource,
    private val networkMonitor: NetworkMonitor
) : MetricsRepository {

    override fun getOrganizations(): Flow<Result<OrganizationsResponse>> = cacheFirstFlow(
        getCached = { localMetrics.getOrganizations() },
        fetchRemote = { safeApiCall { api.getOrganizations() } },
        saveToCache = { localMetrics.saveOrganizations(it) }
    )

    override fun getBillingCycles(): Flow<Result<BillingCyclesResponse>> = cacheFirstFlow(
        getCached = { localMetrics.getBillingCycles() },
        fetchRemote = { safeApiCall { api.getBillingCycles() } },
        saveToCache = { localMetrics.saveBillingCycles(it) }
    )

    override fun getDailyConsumption(): Flow<Result<DailyConsumptionResponse>> = cacheFirstFlow(
        getCached = { localMetrics.getDailyConsumption() },
        fetchRemote = { safeApiCall { api.getDailyConsumption() } },
        saveToCache = { localMetrics.saveDailyConsumption(it) }
    )

    override fun getAcuLimits(): Flow<Result<AcuLimitsResponse>> = cacheFirstFlow(
        getCached = { localMetrics.getAcuLimits() },
        fetchRemote = { safeApiCall { api.getAcuLimits() } },
        saveToCache = { localMetrics.saveAcuLimits(it) }
    )

    override suspend fun updateAcuLimits(newLimit: Double): Result<AcuLimitsResponse> {
        val result = safeApiCall { api.updateAcuLimits(AcuLimitUpdateRequest(newLimit)) }
        result.getOrNull()?.let { localMetrics.saveAcuLimits(it) }
        return result
    }

    override fun getSessions(limit: Int, status: String?): Flow<Result<SessionsResponse>> = cacheFirstFlow(
        getCached = { localSessions.getSessions(limit, status) },
        fetchRemote = { safeApiCall { api.getSessions(limit = limit, status = status) } },
        saveToCache = { localSessions.saveSessions(it.sessions) }
    )

    override fun getMauMetrics(): Flow<Result<MauMetricsResponse>> = cacheFirstFlow(
        getCached = { localMetrics.getMauMetrics() },
        fetchRemote = { safeApiCall { api.getMauMetrics() } },
        saveToCache = { localMetrics.saveMauMetrics(it) }
    )

    override fun getDauMetrics(): Flow<Result<DauMetricsResponse>> = cacheFirstFlow(
        getCached = { localMetrics.getDauMetrics() },
        fetchRemote = { safeApiCall { api.getDauMetrics() } },
        saveToCache = { localMetrics.saveDauMetrics(it) }
    )

    override fun getPrMetrics(): Flow<Result<PrMetricsResponse>> = cacheFirstFlow(
        getCached = { localMetrics.getPrMetrics() },
        fetchRemote = { safeApiCall { api.getPrMetrics() } },
        saveToCache = { localMetrics.savePrMetrics(it) }
    )

    override fun getSessionMetrics(): Flow<Result<SessionMetricsResponse>> = cacheFirstFlow(
        getCached = { localMetrics.getSessionMetrics() },
        fetchRemote = { safeApiCall { api.getSessionMetrics() } },
        saveToCache = { localMetrics.saveSessionMetrics(it) }
    )

    override fun getSearchMetrics(): Flow<Result<SearchMetricsResponse>> = cacheFirstFlow(
        getCached = { localMetrics.getSearchMetrics() },
        fetchRemote = { safeApiCall { api.getSearchMetrics() } },
        saveToCache = { localMetrics.saveSearchMetrics(it) }
    )

    override fun getActiveUsers(): Flow<Result<ActiveUsersResponse>> = cacheFirstFlow(
        getCached = { localMetrics.getActiveUsers() },
        fetchRemote = { safeApiCall { api.getActiveUsers() } },
        saveToCache = { localMetrics.saveActiveUsers(it) }
    )

    override suspend fun validateApiKey(): Result<OrganizationsResponse> {
        return safeApiCall { api.getOrganizations() }
    }

    private fun <T> cacheFirstFlow(
        getCached: suspend () -> T?,
        fetchRemote: suspend () -> Result<T>,
        saveToCache: suspend (T) -> Unit
    ): Flow<Result<T>> = flow {
        val cached = getCached()
        if (cached != null) {
            emit(Result.success(cached))
        }

        if (networkMonitor.isConnected.value) {
            val remoteResult = fetchRemote()
            remoteResult.getOrNull()?.let { saveToCache(it) }
            emit(remoteResult)
        } else if (cached == null) {
            emit(Result.failure(OfflineException()))
        }
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
                    else -> "API error: ${response.code()} ${response.message()}"
                }
                Result.failure(ApiException(response.code(), errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ApiException(val code: Int, message: String) : Exception(message)

class OfflineException : Exception("No internet connection. Showing cached data.")
