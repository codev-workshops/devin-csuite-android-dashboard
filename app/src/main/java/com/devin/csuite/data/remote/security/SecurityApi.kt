package com.devin.csuite.data.remote.security

import com.devin.csuite.domain.model.security.AuditLogsResponse
import com.devin.csuite.domain.model.security.GuardrailViolationsResponse
import com.devin.csuite.domain.model.security.IpAccessListResponse
import com.devin.csuite.domain.model.security.IpAddRequest
import com.devin.csuite.domain.model.security.IpRemoveRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Query

interface SecurityApi {

    @GET("v3/enterprise/audit-logs")
    suspend fun getAuditLogs(
        @Query("first") first: Int = 20,
        @Query("after") after: String? = null,
        @Query("action_type") actionType: String? = null
    ): Response<AuditLogsResponse>

    @GET("v3beta1/enterprise/guardrail-violations")
    suspend fun getGuardrailViolations(
        @Query("first") first: Int = 50,
        @Query("after") after: String? = null,
        @Query("severity") severity: String? = null
    ): Response<GuardrailViolationsResponse>

    @GET("v3/enterprise/ip-access-list")
    suspend fun getIpAccessList(): Response<IpAccessListResponse>

    @POST("v3/enterprise/ip-access-list")
    suspend fun addIpAddress(
        @Body request: IpAddRequest
    ): Response<IpAccessListResponse>

    @HTTP(method = "DELETE", path = "v3/enterprise/ip-access-list", hasBody = true)
    suspend fun removeIpAddress(
        @Body request: IpRemoveRequest
    ): Response<IpAccessListResponse>
}
