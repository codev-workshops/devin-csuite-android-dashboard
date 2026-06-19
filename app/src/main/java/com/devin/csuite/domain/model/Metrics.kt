package com.devin.csuite.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class MetricDataPoint(
    @SerialName("date") val date: String = "",
    @SerialName("value") val value: Double = 0.0
)

@Serializable
data class MauMetricsResponse(
    @SerialName("mau") val mau: Int = 0,
    @SerialName("data") val data: List<MetricDataPoint> = emptyList()
)

@Serializable
data class DauMetricsResponse(
    @SerialName("dau") val dau: Int = 0,
    @SerialName("data") val data: List<MetricDataPoint> = emptyList()
)

@Serializable
data class PrMetricsResponse(
    @SerialName("total_prs") val totalPrs: Int = 0,
    @SerialName("merged_prs") val mergedPrs: Int = 0,
    @SerialName("data") val data: List<MetricDataPoint> = emptyList()
)

@Serializable
data class SessionMetricsResponse(
    @SerialName("total_sessions") val totalSessions: Int = 0,
    @SerialName("data") val data: List<MetricDataPoint> = emptyList()
)

@Immutable
@Serializable
data class ActiveUser(
    @SerialName("user_email") val userEmail: String = "",
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("session_count") val sessionCount: Int = 0,
    @SerialName("acus_consumed") val acusConsumed: Double = 0.0
)

@Serializable
data class ActiveUsersResponse(
    @SerialName("users") val users: List<ActiveUser> = emptyList()
)

@Serializable
data class WauMetricsResponse(
    @SerialName("wau") val wau: Int = 0,
    @SerialName("data") val data: List<MetricDataPoint> = emptyList()
)


