package com.devin.csuite.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class DailyConsumption(
    @SerialName("date") val date: String = "",
    @SerialName("acus_consumed") val acusConsumed: Double = 0.0,
    @SerialName("org_id") val orgId: String? = null,
    @SerialName("org_name") val orgName: String? = null
)

@Serializable
data class DailyConsumptionResponse(
    @SerialName("daily") val daily: List<DailyConsumption> = emptyList(),
    @SerialName("total_acus") val totalAcus: Double = 0.0
)

@Immutable
@Serializable
data class AcuLimits(
    @SerialName("acu_limit") val acuLimit: Double = 0.0,
    @SerialName("acus_used") val acusUsed: Double = 0.0,
    @SerialName("remaining") val remaining: Double = 0.0
)

@Serializable
data class AcuLimitsResponse(
    @SerialName("acu_limit") val acuLimit: Double = 0.0,
    @SerialName("acus_used") val acusUsed: Double = 0.0,
    @SerialName("remaining") val remaining: Double = 0.0
)

@Serializable
data class AcuLimitUpdateRequest(
    @SerialName("acu_limit") val acuLimit: Double
)

@Serializable
data class SearchMetricsResponse(
    @SerialName("total_searches") val totalSearches: Int = 0,
    @SerialName("data") val data: List<MetricDataPoint> = emptyList()
)
