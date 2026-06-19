package com.devin.csuite.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devin.csuite.data.local.PreferencesManager
import com.devin.csuite.data.local.SecureKeyStore
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by preferencesManager.themeMode.collectAsState(initial = "dark")

            CompositionLocalProvider(LocalThemeMode provides themeMode) {
                DevinCSuiteTheme(themeMode = themeMode) {
                    MainApp(
                        hasApiKey = secureKeyStore.hasApiKey()
                    )
                }
            }
        }
    }
}

@Composable
fun MainApp(hasApiKey: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (hasApiKey) Routes.HOME else Routes.WELCOME
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Routes.HOME, Routes.ANALYTICS, Routes.SESSIONS, Routes.BILLING, Routes.SETTINGS
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
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
