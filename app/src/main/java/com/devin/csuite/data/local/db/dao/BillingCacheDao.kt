package com.devin.csuite.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.devin.csuite.data.local.db.entity.CachedBillingCycle

@Dao
interface BillingCacheDao {

    @Upsert
    suspend fun upsertCycle(cycle: CachedBillingCycle)

    @Upsert
    suspend fun upsertCycles(cycles: List<CachedBillingCycle>)

    @Query("SELECT * FROM cached_billing_cycles WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentCycle(): CachedBillingCycle?

    @Query("SELECT * FROM cached_billing_cycles ORDER BY cycleStart DESC")
    suspend fun getAllCycles(): List<CachedBillingCycle>

    @Query("DELETE FROM cached_billing_cycles")
    suspend fun clearAll()

    @Query("DELETE FROM cached_billing_cycles WHERE cachedAt < :expiryTime")
    suspend fun deleteExpired(expiryTime: Long)
}
