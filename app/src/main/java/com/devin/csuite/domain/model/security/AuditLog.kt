package com.devin.csuite.domain.model.security

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class AuditLogEntry(
    @SerialName("id") val id: String = "",
    @SerialName("actor_name") val actorName: String? = null,
    @SerialName("actor_email") val actorEmail: String = "",
    @SerialName("action") val action: String = "",
    @SerialName("resource_type") val resourceType: String = "",
    @SerialName("resource_id") val resourceId: String = "",
    @SerialName("timestamp") val timestamp: Long = 0,
    @SerialName("details") val details: String? = null
)

@Serializable
data class AuditLogsResponse(
    @SerialName("audit_logs") val auditLogs: List<AuditLogEntry> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false
)
