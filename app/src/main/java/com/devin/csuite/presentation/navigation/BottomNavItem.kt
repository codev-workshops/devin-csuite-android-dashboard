package com.devin.csuite.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(Routes.HOME, "Home", Icons.Default.Home)
    data object Analytics : BottomNavItem(Routes.ANALYTICS, "Analytics", Icons.Default.Analytics)
    data object Sessions : BottomNavItem(Routes.SESSIONS, "Sessions", Icons.Default.Terminal)
    data object Billing : BottomNavItem(Routes.BILLING, "Billing", Icons.Default.Payments)
    data object Settings : BottomNavItem(Routes.SETTINGS, "Settings", Icons.Default.Settings)

    companion object {
        val items = listOf(Home, Analytics, Sessions, Billing, Settings)
    }
}
