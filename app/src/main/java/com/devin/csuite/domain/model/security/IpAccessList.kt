package com.devin.csuite.domain.model.security

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class IpAccessEntry(
    @SerialName("ip") val ip: String = "",
    @SerialName("description") val description: String? = null,
    @SerialName("created_at") val createdAt: Long = 0
)

@Serializable
data class IpAccessListResponse(
    @SerialName("ip_addresses") val ipAddresses: List<IpAccessEntry> = emptyList()
)

@Serializable
data class IpAddRequest(
    @SerialName("ip") val ip: String,
    @SerialName("description") val description: String? = null
)

@Serializable
data class IpRemoveRequest(
    @SerialName("ip") val ip: String
)
