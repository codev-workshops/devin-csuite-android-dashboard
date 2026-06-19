package com.devin.csuite.data.remote.security

import com.devin.csuite.data.remote.ApiException
import com.devin.csuite.domain.model.security.AuditLogsResponse
import com.devin.csuite.domain.model.security.GuardrailViolationsResponse
import com.devin.csuite.domain.model.security.IpAccessListResponse
import com.devin.csuite.domain.model.security.IpAddRequest
import com.devin.csuite.domain.model.security.IpRemoveRequest
import com.devin.csuite.domain.repository.security.SecurityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepositoryImpl @Inject constructor(
    private val api: SecurityApi
) : SecurityRepository {

    override fun getAuditLogs(
        first: Int,
        after: String?,
        actionType: String?
    ): Flow<Result<AuditLogsResponse>> = flow {
        emit(safeApiCall { api.getAuditLogs(first = first, after = after, actionType = actionType) })
    }

    override fun getGuardrailViolations(
        first: Int,
        after: String?,
        severity: String?
    ): Flow<Result<GuardrailViolationsResponse>> = flow {
        emit(safeApiCall { api.getGuardrailViolations(first = first, after = after, severity = severity) })
    }

    override fun getIpAccessList(): Flow<Result<IpAccessListResponse>> = flow {
        emit(safeApiCall { api.getIpAccessList() })
    }

    override suspend fun addIpAddress(ip: String, description: String?): Result<IpAccessListResponse> {
        return safeApiCall { api.addIpAddress(IpAddRequest(ip = ip, description = description)) }
    }

    override suspend fun removeIpAddress(ip: String): Result<IpAccessListResponse> {
        return safeApiCall { api.removeIpAddress(IpRemoveRequest(ip = ip)) }
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
                    401, 403 -> "Insufficient permissions"
                    404, 501 -> "Feature not available"
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
