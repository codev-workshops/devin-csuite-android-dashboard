package com.devin.csuite.deeplink

import android.content.Context
import android.content.Intent

object ShareHelper {

    fun shareDeepLink(context: Context, route: String, title: String = "Devin Dashboard") {
        val deepLink = DeepLinkHandler.createDeepLink(route)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Check this out in Devin Dashboard: $deepLink")
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
}
