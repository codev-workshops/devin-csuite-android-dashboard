package com.devin.csuite.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.devin.csuite.presentation.home.HomeScreen
import com.devin.csuite.presentation.onboarding.ApiKeyInputScreen
import com.devin.csuite.presentation.onboarding.WelcomeScreen
import com.devin.csuite.presentation.settings.SettingsScreen
import com.devin.csuite.presentation.PlaceholderScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(Routes.API_KEY_INPUT) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.API_KEY_INPUT) {
            ApiKeyInputScreen(
                onValidationSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.API_KEY_INPUT) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen()
        }

        composable(Routes.ANALYTICS) {
            PlaceholderScreen(title = "Analytics", subtitle = "Coming in Phase 2")
        }

        composable(Routes.SESSIONS) {
            PlaceholderScreen(title = "Sessions", subtitle = "Coming in Phase 4")
        }

        composable(Routes.BILLING) {
            PlaceholderScreen(title = "Billing", subtitle = "Coming in Phase 2")
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateToOnboarding = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
