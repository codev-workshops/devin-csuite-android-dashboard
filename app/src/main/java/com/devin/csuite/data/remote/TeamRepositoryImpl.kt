package com.devin.csuite.data.remote

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
    private val api: EnterpriseApi
) : TeamRepository {

    override fun getDauMetrics(orgId: String?): Flow<Result<DauMetricsResponse>> = flow {
        emit(safeApiCall {
            if (orgId != null) api.getOrgDauMetrics(orgId) else api.getDauMetrics()
        })
    }

    override fun getWauMetrics(orgId: String?): Flow<Result<WauMetricsResponse>> = flow {
        emit(safeApiCall {
            if (orgId != null) api.getOrgWauMetrics(orgId) else api.getWauMetrics()
        })
    }

    override fun getMauMetrics(orgId: String?): Flow<Result<MauMetricsResponse>> = flow {
        emit(safeApiCall {
            if (orgId != null) api.getOrgMauMetrics(orgId) else api.getMauMetrics()
        })
    }

    override fun getActiveUsers(orgId: String?): Flow<Result<ActiveUsersResponse>> = flow {
        emit(safeApiCall {
            if (orgId != null) api.getOrgActiveUsers(orgId) else api.getActiveUsers()
        })
    }

    override fun getOrganizations(): Flow<Result<OrganizationsResponse>> = flow {
        emit(safeApiCall { api.getOrganizations() })
    }

    override fun getUsers(orgId: String?): Flow<Result<UsersResponse>> = flow {
        emit(safeApiCall {
            if (orgId != null) api.getOrgUsers(orgId) else api.getUsers()
        })
    }

    override fun getRoles(orgId: String?): Flow<Result<RolesResponse>> = flow {
        emit(safeApiCall {
            if (orgId != null) api.getOrgRoles(orgId) else api.getRoles()
        })
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
