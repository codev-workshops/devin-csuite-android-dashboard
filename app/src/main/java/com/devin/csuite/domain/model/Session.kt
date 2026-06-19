package com.devin.csuite.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Session(
    @SerialName("session_id") val sessionId: String = "",
    @SerialName("title") val title: String? = null,
    @SerialName("status") val status: String = "",
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
    @SerialName("acus_consumed") val acusConsumed: Double = 0.0,
    @SerialName("user_email") val userEmail: String? = null,
    @SerialName("origin") val origin: String? = null,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("pull_requests") val pullRequests: List<SessionPullRequest> = emptyList(),
    @SerialName("url") val url: String? = null,
    @SerialName("org_id") val orgId: String? = null,
    @SerialName("playbook_id") val playbookId: String? = null,
    @SerialName("status_enum") val statusEnum: String? = null
)

@Immutable
@Serializable
data class SessionPullRequest(
    @SerialName("url") val url: String = "",
    @SerialName("title") val title: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("number") val number: Int? = null,
    @SerialName("repo") val repo: String? = null
)

@Serializable
data class SessionsResponse(
    @SerialName("sessions") val sessions: List<Session> = emptyList(),
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false
)

@Serializable
data class InsightsResponse(
    @SerialName("insights") val insights: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("session_id") val sessionId: String? = null
)
