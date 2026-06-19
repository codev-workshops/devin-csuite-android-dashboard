package com.devin.csuite.domain.repository.security

import com.devin.csuite.domain.model.security.AuditLogsResponse
import com.devin.csuite.domain.model.security.GuardrailViolationsResponse
import com.devin.csuite.domain.model.security.IpAccessListResponse
import kotlinx.coroutines.flow.Flow

interface SecurityRepository {
    fun getAuditLogs(
        first: Int = 20,
        after: String? = null,
        actionType: String? = null
    ): Flow<Result<AuditLogsResponse>>

    fun getGuardrailViolations(
        first: Int = 50,
        after: String? = null,
        severity: String? = null
    ): Flow<Result<GuardrailViolationsResponse>>

    fun getIpAccessList(): Flow<Result<IpAccessListResponse>>

    suspend fun addIpAddress(ip: String, description: String? = null): Result<IpAccessListResponse>

    suspend fun removeIpAddress(ip: String): Result<IpAccessListResponse>
}
