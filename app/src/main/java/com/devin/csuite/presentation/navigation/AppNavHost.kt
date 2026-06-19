package com.devin.csuite.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devin.csuite.presentation.analytics.AnalyticsScreen
import com.devin.csuite.presentation.billing.BillingScreen
import com.devin.csuite.presentation.home.HomeScreen
import com.devin.csuite.presentation.onboarding.ApiKeyInputScreen
import com.devin.csuite.presentation.onboarding.WelcomeScreen
import com.devin.csuite.presentation.sessions.SessionDetailScreen
import com.devin.csuite.presentation.sessions.SessionsScreen
import com.devin.csuite.presentation.settings.SettingsScreen

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
            AnalyticsScreen()
        }

        composable(Routes.SESSIONS) {
            SessionsScreen(
                onSessionClick = { sessionId ->
                    navController.navigate(Routes.sessionDetail(sessionId))
                }
            )
        }

        composable(
            route = Routes.SESSION_DETAIL,
            arguments = listOf(
                navArgument("devinId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val devinId = backStackEntry.arguments?.getString("devinId") ?: ""
            SessionDetailScreen(
                devinId = devinId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.BILLING) {
            BillingScreen()
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
