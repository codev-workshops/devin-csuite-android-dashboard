package com.devin.csuite.deeplink

import android.content.Intent
import android.net.Uri
import com.devin.csuite.presentation.navigation.Routes

object DeepLinkHandler {
    const val SCHEME = "devin-dashboard"

    fun parseDeepLink(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        if (uri.scheme != SCHEME) return null
        return mapUriToRoute(uri)
    }

    fun parseDeepLink(uri: Uri?): String? {
        if (uri == null || uri.scheme != SCHEME) return null
        return mapUriToRoute(uri)
    }

    private fun mapUriToRoute(uri: Uri): String? {
        val path = uri.path ?: return null
        return when {
            path == "/home" || path == "/" -> Routes.HOME
            path == "/analytics" -> Routes.ANALYTICS
            path == "/team" -> Routes.TEAM
            path.startsWith("/sessions/") -> {
                val sessionId = path.removePrefix("/sessions/")
                if (sessionId.isNotBlank()) Routes.sessionDetail(sessionId) else Routes.SESSIONS
            }
            path == "/sessions" -> Routes.SESSIONS
            path == "/billing" -> Routes.BILLING
            path == "/security" -> Routes.SECURITY
            path == "/settings" -> Routes.SETTINGS
            else -> null
        }
    }

    fun createDeepLink(route: String): String {
        val path = when {
            route == Routes.HOME -> "/home"
            route == Routes.ANALYTICS -> "/analytics"
            route == Routes.TEAM -> "/team"
            route == Routes.SESSIONS -> "/sessions"
            route.startsWith("session_detail/") -> {
                val id = route.removePrefix("session_detail/")
                "/sessions/$id"
            }
            route == Routes.BILLING -> "/billing"
            route == Routes.SECURITY -> "/security"
            route == Routes.SETTINGS -> "/settings"
            else -> "/home"
        }
        return "$SCHEME://$path"
    }
}
