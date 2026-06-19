package com.devin.csuite.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Organization(
    @SerialName("org_id") val orgId: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("display_name") val displayName: String? = null
)

@Serializable
data class OrganizationsResponse(
    @SerialName("organizations") val organizations: List<Organization> = emptyList()
)
