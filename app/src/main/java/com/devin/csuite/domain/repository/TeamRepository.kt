package com.devin.csuite.domain.repository

import com.devin.csuite.domain.model.ActiveUsersResponse
import com.devin.csuite.domain.model.DauMetricsResponse
import com.devin.csuite.domain.model.MauMetricsResponse
import com.devin.csuite.domain.model.OrganizationsResponse
import com.devin.csuite.domain.model.RolesResponse
import com.devin.csuite.domain.model.UsersResponse
import com.devin.csuite.domain.model.WauMetricsResponse
import kotlinx.coroutines.flow.Flow

interface TeamRepository {
    fun getDauMetrics(orgId: String?): Flow<Result<DauMetricsResponse>>
    fun getWauMetrics(orgId: String?): Flow<Result<WauMetricsResponse>>
    fun getMauMetrics(orgId: String?): Flow<Result<MauMetricsResponse>>
    fun getActiveUsers(orgId: String?): Flow<Result<ActiveUsersResponse>>
    fun getOrganizations(): Flow<Result<OrganizationsResponse>>
    fun getUsers(orgId: String?): Flow<Result<UsersResponse>>
    fun getRoles(orgId: String?): Flow<Result<RolesResponse>>
}
