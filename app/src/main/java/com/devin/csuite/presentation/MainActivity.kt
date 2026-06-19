package com.devin.csuite.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devin.csuite.data.local.PreferencesManager
import com.devin.csuite.data.local.SecureKeyStore
import com.devin.csuite.deeplink.DeepLinkHandler
import com.devin.csuite.presentation.navigation.AppNavHost
import com.devin.csuite.presentation.navigation.BottomNavItem
import com.devin.csuite.presentation.navigation.Routes
import com.devin.csuite.presentation.theme.AccentPrimary
import com.devin.csuite.presentation.theme.DevinCSuiteTheme
import com.devin.csuite.presentation.theme.LocalThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var secureKeyStore: SecureKeyStore

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private var pendingDeepLink: String? = null

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingDeepLink = DeepLinkHandler.parseDeepLink(intent)

        setContent {
            val themeMode by preferencesManager.themeMode.collectAsState(initial = "dark")
            val windowSizeClass = calculateWindowSizeClass(this)

            CompositionLocalProvider(LocalThemeMode provides themeMode) {
                DevinCSuiteTheme(themeMode = themeMode) {
                    MainApp(
                        hasApiKey = secureKeyStore.hasApiKey(),
                        deepLinkRoute = pendingDeepLink,
                        onDeepLinkConsumed = { pendingDeepLink = null },
                        useNavigationRail = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingDeepLink = DeepLinkHandler.parseDeepLink(intent)
    }
}

@Composable
fun MainApp(
    hasApiKey: Boolean,
    deepLinkRoute: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    useNavigationRail: Boolean = false
) {
    val navController = rememberNavController()
    val startDestination = if (hasApiKey) Routes.HOME else Routes.WELCOME
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showNav = currentRoute in listOf(
        Routes.HOME, Routes.ANALYTICS, Routes.SESSIONS, Routes.BILLING, Routes.SETTINGS
    ) && currentRoute?.startsWith("session_detail") != true

    LaunchedEffect(deepLinkRoute) {
        deepLinkRoute?.let { route ->
            if (hasApiKey) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            onDeepLinkConsumed()
        }
    }

    if (useNavigationRail && showNav) {
        Row {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                BottomNavItem.items.forEach { item ->
                    NavigationRailItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                            selectedIconColor = AccentPrimary,
                            selectedTextColor = AccentPrimary,
                            indicatorColor = AccentPrimary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            AppNavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        Scaffold(
            bottomBar = {
                if (showNav) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        BottomNavItem.items.forEach { item ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title
                                    )
                                },
                                label = { Text(item.title) },
                                selected = currentRoute == item.route,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AccentPrimary,
                                    selectedTextColor = AccentPrimary,
                                    indicatorColor = AccentPrimary.copy(alpha = 0.12f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
