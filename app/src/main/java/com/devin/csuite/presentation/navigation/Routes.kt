package com.devin.csuite.presentation.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val WELCOME = "welcome"
    const val API_KEY_INPUT = "api_key_input"
    const val HOME = "home"
    const val ANALYTICS = "analytics"
    const val SESSIONS = "sessions"
    const val SESSION_DETAIL = "session_detail/{devinId}"
    const val BILLING = "billing"
    const val SETTINGS = "settings"

    fun sessionDetail(devinId: String): String = "session_detail/$devinId"
}
