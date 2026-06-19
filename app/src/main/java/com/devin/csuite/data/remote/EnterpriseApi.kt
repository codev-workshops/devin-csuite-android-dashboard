package com.devin.csuite.data.remote

import com.devin.csuite.domain.model.AcuLimitUpdateRequest
import com.devin.csuite.domain.model.AcuLimitsResponse
import com.devin.csuite.domain.model.BillingCyclesResponse
import com.devin.csuite.domain.model.DailyConsumptionResponse
import com.devin.csuite.domain.model.DauMetricsResponse
import com.devin.csuite.domain.model.ActiveUsersResponse
import com.devin.csuite.domain.model.MauMetricsResponse
import com.devin.csuite.domain.model.OrganizationsResponse
import com.devin.csuite.domain.model.PrMetricsResponse
import com.devin.csuite.domain.model.SearchMetricsResponse
import com.devin.csuite.domain.model.SessionMetricsResponse
import com.devin.csuite.domain.model.SessionsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface EnterpriseApi {

    @GET("v3/enterprise/organizations")
    suspend fun getOrganizations(): Response<OrganizationsResponse>

    @GET("v3/enterprise/consumption/cycles")
    suspend fun getBillingCycles(): Response<BillingCyclesResponse>

    @GET("v3/enterprise/consumption/daily")
    suspend fun getDailyConsumption(): Response<DailyConsumptionResponse>

    @GET("v3/enterprise/acu-limits")
    suspend fun getAcuLimits(): Response<AcuLimitsResponse>

    @PUT("v3/enterprise/acu-limits")
    suspend fun updateAcuLimits(
        @Body request: AcuLimitUpdateRequest
    ): Response<AcuLimitsResponse>

    @GET("v3/enterprise/sessions")
    suspend fun getSessions(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("status") status: String? = null
    ): Response<SessionsResponse>

    @GET("v3/enterprise/metrics/mau")
    suspend fun getMauMetrics(): Response<MauMetricsResponse>

    @GET("v3/enterprise/metrics/dau")
    suspend fun getDauMetrics(): Response<DauMetricsResponse>

    @GET("v3/enterprise/metrics/prs")
    suspend fun getPrMetrics(): Response<PrMetricsResponse>

    @GET("v3/enterprise/metrics/sessions")
    suspend fun getSessionMetrics(): Response<SessionMetricsResponse>

    @GET("v3/enterprise/metrics/searches")
    suspend fun getSearchMetrics(): Response<SearchMetricsResponse>

    @GET("v3/enterprise/metrics/active-users")
    suspend fun getActiveUsers(): Response<ActiveUsersResponse>
}
