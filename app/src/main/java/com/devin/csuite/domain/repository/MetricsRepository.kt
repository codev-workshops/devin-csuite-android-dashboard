package com.devin.csuite.domain.repository

import com.devin.csuite.domain.model.ActiveUsersResponse
import com.devin.csuite.domain.model.BillingCyclesResponse
import com.devin.csuite.domain.model.DauMetricsResponse
import com.devin.csuite.domain.model.MauMetricsResponse
import com.devin.csuite.domain.model.OrganizationsResponse
import com.devin.csuite.domain.model.PrMetricsResponse
import com.devin.csuite.domain.model.SessionMetricsResponse
import com.devin.csuite.domain.model.SessionsResponse
import kotlinx.coroutines.flow.Flow

interface MetricsRepository {
    fun getOrganizations(): Flow<Result<OrganizationsResponse>>
    fun getBillingCycles(): Flow<Result<BillingCyclesResponse>>
    fun getSessions(limit: Int = 100, status: String? = null): Flow<Result<SessionsResponse>>
    fun getMauMetrics(): Flow<Result<MauMetricsResponse>>
    fun getDauMetrics(): Flow<Result<DauMetricsResponse>>
    fun getPrMetrics(): Flow<Result<PrMetricsResponse>>
    fun getSessionMetrics(): Flow<Result<SessionMetricsResponse>>
    fun getActiveUsers(): Flow<Result<ActiveUsersResponse>>
    suspend fun validateApiKey(): Result<OrganizationsResponse>
}
