package com.devin.csuite.data.remote

import com.devin.csuite.domain.model.ActiveUsersResponse
import com.devin.csuite.domain.model.BillingCyclesResponse
import com.devin.csuite.domain.model.DauMetricsResponse
import com.devin.csuite.domain.model.MauMetricsResponse
import com.devin.csuite.domain.model.OrganizationsResponse
import com.devin.csuite.domain.model.PrMetricsResponse
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
    private val api: EnterpriseApi
) : MetricsRepository {

    override fun getOrganizations(): Flow<Result<OrganizationsResponse>> = flow {
        emit(safeApiCall { api.getOrganizations() })
    }

    override fun getBillingCycles(): Flow<Result<BillingCyclesResponse>> = flow {
        emit(safeApiCall { api.getBillingCycles() })
    }

    override fun getSessions(limit: Int, status: String?): Flow<Result<SessionsResponse>> = flow {
        emit(safeApiCall { api.getSessions(limit = limit, status = status) })
    }

    override fun getMauMetrics(): Flow<Result<MauMetricsResponse>> = flow {
        emit(safeApiCall { api.getMauMetrics() })
    }

    override fun getDauMetrics(): Flow<Result<DauMetricsResponse>> = flow {
        emit(safeApiCall { api.getDauMetrics() })
    }

    override fun getPrMetrics(): Flow<Result<PrMetricsResponse>> = flow {
        emit(safeApiCall { api.getPrMetrics() })
    }

    override fun getSessionMetrics(): Flow<Result<SessionMetricsResponse>> = flow {
        emit(safeApiCall { api.getSessionMetrics() })
    }

    override fun getActiveUsers(): Flow<Result<ActiveUsersResponse>> = flow {
        emit(safeApiCall { api.getActiveUsers() })
    }

    override suspend fun validateApiKey(): Result<OrganizationsResponse> {
        return safeApiCall { api.getOrganizations() }
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
