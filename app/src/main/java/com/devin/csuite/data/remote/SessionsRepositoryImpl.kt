package com.devin.csuite.data.remote

import com.devin.csuite.domain.model.InsightsResponse
import com.devin.csuite.domain.model.Session
import com.devin.csuite.domain.model.SessionsResponse
import com.devin.csuite.domain.repository.SessionsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionsRepositoryImpl @Inject constructor(
    private val api: SessionsApi
) : SessionsRepository {

    override fun getSessions(
        first: Int?,
        after: String?,
        status: String?,
        origin: String?,
        user: String?,
        tags: String?,
        createdAfter: Long?,
        createdBefore: Long?
    ): Flow<Result<SessionsResponse>> = flow {
        emit(safeApiCall {
            api.getSessions(
                first = first,
                after = after,
                status = status,
                origin = origin,
                user = user,
                tags = tags,
                createdAfter = createdAfter,
                createdBefore = createdBefore
            )
        })
    }

    override fun getSessionDetail(devinId: String): Flow<Result<Session>> = flow {
        emit(safeApiCall { api.getSessionDetail(devinId) })
    }

    override fun generateInsights(devinId: String): Flow<Result<InsightsResponse>> = flow {
        emit(safeApiCall { api.generateInsights(devinId) })
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
                    404 -> "Session not found"
                    else -> "API error: ${response.code()} ${response.message()}"
                }
                Result.failure(ApiException(response.code(), errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
