package com.devin.csuite.data.local.datasource

import com.devin.csuite.data.local.db.AppDatabase
import com.devin.csuite.data.local.db.dao.SessionsCacheDao
import com.devin.csuite.data.local.db.entity.CachedSession
import com.devin.csuite.domain.model.Session
import com.devin.csuite.domain.model.SessionPullRequest
import com.devin.csuite.domain.model.SessionsResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSessionsDataSource @Inject constructor(
    private val sessionsCacheDao: SessionsCacheDao,
    private val json: Json
) {
    suspend fun getSessions(limit: Int = 100, status: String? = null): SessionsResponse? {
        val cached = if (status != null) {
            sessionsCacheDao.getSessionsByStatus(status, limit)
        } else {
            sessionsCacheDao.getRecentSessions(limit)
        }
        if (cached.isEmpty()) return null
        return SessionsResponse(
            sessions = cached.map { it.toDomainModel() },
            totalCount = cached.size
        )
    }

    suspend fun getSession(id: String): Session? {
        return sessionsCacheDao.getSession(id)?.toDomainModel()
    }

    suspend fun getActiveSessionCount(): Int {
        return sessionsCacheDao.getActiveSessionCount()
    }

    suspend fun saveSessions(sessions: List<Session>) {
        val entities = sessions.map { it.toEntity() }
        sessionsCacheDao.upsertSessions(entities)
    }

    suspend fun clearAll() {
        sessionsCacheDao.clearAll()
    }

    suspend fun evictExpired() {
        val expiryTime = System.currentTimeMillis() - AppDatabase.CACHE_EXPIRY_MS
        sessionsCacheDao.deleteExpired(expiryTime)
    }

    private fun Session.toEntity() = CachedSession(
        sessionId = sessionId,
        title = title,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        acusConsumed = acusConsumed,
        userEmail = userEmail,
        origin = origin,
        tagsJson = json.encodeToString(tags),
        pullRequestsJson = json.encodeToString(pullRequests),
        url = url,
        orgId = orgId,
        playbookId = playbookId,
        statusEnum = statusEnum
    )

    private fun CachedSession.toDomainModel() = Session(
        sessionId = sessionId,
        title = title,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        acusConsumed = acusConsumed,
        userEmail = userEmail,
        origin = origin,
        tags = try { json.decodeFromString(tagsJson) } catch (_: Exception) { emptyList() },
        pullRequests = try { json.decodeFromString<List<SessionPullRequest>>(pullRequestsJson) } catch (_: Exception) { emptyList() },
        url = url,
        orgId = orgId,
        playbookId = playbookId,
        statusEnum = statusEnum
    )
}
