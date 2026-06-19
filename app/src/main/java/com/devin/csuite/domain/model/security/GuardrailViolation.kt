package com.devin.csuite.domain.model.security

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class GuardrailViolation(
    @SerialName("id") val id: String = "",
    @SerialName("severity") val severity: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("timestamp") val timestamp: Long = 0,
    @SerialName("rule_name") val ruleName: String? = null
)

@Serializable
data class GuardrailViolationsResponse(
    @SerialName("violations") val violations: List<GuardrailViolation> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false
)

@Immutable
@Serializable
data class ViolationTrendPoint(
    @SerialName("date") val date: String = "",
    @SerialName("count") val count: Int = 0
)
