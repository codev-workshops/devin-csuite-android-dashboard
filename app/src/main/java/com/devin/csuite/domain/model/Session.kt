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
    @SerialName("origin") val origin: String? = null
)

@Serializable
data class SessionsResponse(
    @SerialName("sessions") val sessions: List<Session> = emptyList(),
    @SerialName("total_count") val totalCount: Int = 0
)
