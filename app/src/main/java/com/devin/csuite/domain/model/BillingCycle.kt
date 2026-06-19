package com.devin.csuite.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class BillingCycle(
    @SerialName("cycle_start") val cycleStart: String = "",
    @SerialName("cycle_end") val cycleEnd: String = "",
    @SerialName("acus_used") val acusUsed: Double = 0.0,
    @SerialName("acu_limit") val acuLimit: Double = 0.0,
    @SerialName("status") val status: String = ""
)

@Serializable
data class BillingCyclesResponse(
    @SerialName("cycles") val cycles: List<BillingCycle> = emptyList(),
    @SerialName("current_cycle") val currentCycle: BillingCycle? = null
)
