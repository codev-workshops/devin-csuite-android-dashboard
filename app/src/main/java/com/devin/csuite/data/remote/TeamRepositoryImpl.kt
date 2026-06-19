package com.devin.csuite.data.remote

import com.devin.csuite.core.NetworkMonitor
import com.devin.csuite.data.local.datasource.LocalMetricsDataSource
import com.devin.csuite.domain.model.ActiveUsersResponse
import com.devin.csuite.domain.model.DauMetricsResponse
import com.devin.csuite.domain.model.MauMetricsResponse
import com.devin.csuite.domain.model.OrganizationsResponse
import com.devin.csuite.domain.model.RolesResponse
import com.devin.csuite.domain.model.UsersResponse
import com.devin.csuite.domain.model.WauMetricsResponse
import com.devin.csuite.domain.repository.TeamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeamRepositoryImpl @Inject constructor(
    private val api: EnterpriseApi,
    private val localMetrics: LocalMetricsDataSource,
    private val networkMonitor: NetworkMonitor
) : TeamRepository {

    override fun getDauMetrics(orgId: String?): Flow<Result<DauMetricsResponse>> = flow {
        if (orgId == null) {
            localMetrics.getDauMetrics()?.let { emit(Result.success(it)) }
        }
        if (networkMonitor.isConnected.value) {
            val result = safeApiCall {
                if (orgId != null) api.getOrgDauMetrics(orgId) else api.getDauMetrics()
            }
            if (orgId == null) result.getOrNull()?.let { localMetrics.saveDauMetrics(it) }
            emit(result)
        }
    }

    override fun getWauMetrics(orgId: String?): Flow<Result<WauMetricsResponse>> = flow {
        if (networkMonitor.isConnected.value) {
            emit(safeApiCall {
                if (orgId != null) api.getOrgWauMetrics(orgId) else api.getWauMetrics()
            })
        } else {
            emit(Result.failure(OfflineException()))
        }
    }

    override fun getMauMetrics(orgId: String?): Flow<Result<MauMetricsResponse>> = flow {
        if (orgId == null) {
            localMetrics.getMauMetrics()?.let { emit(Result.success(it)) }
        }
        if (networkMonitor.isConnected.value) {
            val result = safeApiCall {
                if (orgId != null) api.getOrgMauMetrics(orgId) else api.getMauMetrics()
            }
            if (orgId == null) result.getOrNull()?.let { localMetrics.saveMauMetrics(it) }
            emit(result)
        }
    }

    override fun getActiveUsers(orgId: String?): Flow<Result<ActiveUsersResponse>> = flow {
        if (orgId == null) {
            localMetrics.getActiveUsers()?.let { emit(Result.success(it)) }
        }
        if (networkMonitor.isConnected.value) {
            val result = safeApiCall {
                if (orgId != null) api.getOrgActiveUsers(orgId) else api.getActiveUsers()
            }
            if (orgId == null) result.getOrNull()?.let { localMetrics.saveActiveUsers(it) }
            emit(result)
        }
    }

    override fun getOrganizations(): Flow<Result<OrganizationsResponse>> = flow {
        localMetrics.getOrganizations()?.let { emit(Result.success(it)) }
        if (networkMonitor.isConnected.value) {
            val result = safeApiCall { api.getOrganizations() }
            result.getOrNull()?.let { localMetrics.saveOrganizations(it) }
            emit(result)
        }
    }

    override fun getUsers(orgId: String?): Flow<Result<UsersResponse>> = flow {
        if (networkMonitor.isConnected.value) {
            emit(safeApiCall {
                if (orgId != null) api.getOrgUsers(orgId) else api.getUsers()
            })
        } else {
            emit(Result.failure(OfflineException()))
        }
    }

    override fun getRoles(orgId: String?): Flow<Result<RolesResponse>> = flow {
        if (networkMonitor.isConnected.value) {
            emit(safeApiCall {
                if (orgId != null) api.getOrgRoles(orgId) else api.getRoles()
            })
        } else {
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
