package com.devin.csuite.data.remote

import com.devin.csuite.domain.model.InsightsResponse
import com.devin.csuite.domain.model.Session
import com.devin.csuite.domain.model.SessionsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SessionsApi {

    @GET("v3/enterprise/sessions")
    suspend fun getSessions(
        @Query("first") first: Int? = null,
        @Query("after") after: String? = null,
        @Query("status") status: String? = null,
        @Query("origin") origin: String? = null,
        @Query("user") user: String? = null,
        @Query("tags") tags: String? = null,
        @Query("created_after") createdAfter: Long? = null,
        @Query("created_before") createdBefore: Long? = null
    ): Response<SessionsResponse>

    @GET("v3/enterprise/sessions/{devin_id}")
    suspend fun getSessionDetail(
        @Path("devin_id") devinId: String
    ): Response<Session>

    @POST("v3/enterprise/sessions/{devin_id}/insights/generate")
    suspend fun generateInsights(
        @Path("devin_id") devinId: String
    ): Response<InsightsResponse>
}
