package com.devin.csuite.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class EnterpriseUser(
    @SerialName("user_id") val userId: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("role") val role: String = "",
    @SerialName("last_active") val lastActive: Long = 0,
    @SerialName("session_count") val sessionCount: Int = 0,
    @SerialName("acus_consumed") val acusConsumed: Double = 0.0,
    @SerialName("org_id") val orgId: String? = null
)

@Serializable
data class UsersResponse(
    @SerialName("users") val users: List<EnterpriseUser> = emptyList()
)

@Immutable
@Serializable
data class EnterpriseRole(
    @SerialName("role_id") val roleId: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("user_count") val userCount: Int = 0
)

@Serializable
data class RolesResponse(
    @SerialName("roles") val roles: List<EnterpriseRole> = emptyList()
)
