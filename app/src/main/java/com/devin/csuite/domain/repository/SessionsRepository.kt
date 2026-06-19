package com.devin.csuite.domain.repository

import com.devin.csuite.domain.model.InsightsResponse
import com.devin.csuite.domain.model.Session
import com.devin.csuite.domain.model.SessionsResponse
import kotlinx.coroutines.flow.Flow

interface SessionsRepository {

    fun getSessions(
        first: Int? = null,
        after: String? = null,
        status: String? = null,
        origin: String? = null,
        user: String? = null,
        tags: String? = null,
        createdAfter: Long? = null,
        createdBefore: Long? = null
    ): Flow<Result<SessionsResponse>>

    fun getSessionDetail(devinId: String): Flow<Result<Session>>

    fun generateInsights(devinId: String): Flow<Result<InsightsResponse>>
}
