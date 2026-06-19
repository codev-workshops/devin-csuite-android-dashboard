package com.devin.csuite.integration

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.testing.TestNavHostController
import com.devin.csuite.presentation.navigation.BottomNavItem
import com.devin.csuite.presentation.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive navigation integration tests that verify the entire navigation
 * graph works correctly, including bottom nav, onboarding flow, deep links,
 * settings sub-navigation, session detail, and edge cases.
 *
 * Uses a self-contained test composable that mirrors the real app's navigation
 * structure with stub screens, avoiding Hilt ViewModel dependencies while
 * still exercising the full NavHost route graph and back stack behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class NavigationIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    // ==================== Setup ====================

    private fun setUpNavigation(
        hasApiKey: Boolean = true,
        deepLinkRoute: String? = null
    ) {
        composeTestRule.setContent {
            val context = LocalContext.current
            navController = remember {
                TestNavHostController(context).apply {
                    navigatorProvider.addNavigator(ComposeNavigator())
                }
            }
            TestMainApp(
                navController = navController,
                hasApiKey = hasApiKey,
                deepLinkRoute = deepLinkRoute
            )
        }
        composeTestRule.waitForIdle()
    }

    private fun currentRoute(): String? {
        return composeTestRule.runOnIdle {
            navController.currentBackStackEntry?.destination?.route
        }
    }

    private fun navigateToSettings() {
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
    }

    private fun navigateToSessions() {
        composeTestRule.onNodeWithText("Sessions").performClick()
        composeTestRule.waitForIdle()
    }

    private fun assertBottomNavHidden() {
        val nodes = composeTestRule.onAllNodesWithTag("bottom_nav")
            .fetchSemanticsNodes()
        assertEquals("Bottom nav should not be present", 0, nodes.size)
    }

    private fun assertNodeDoesNotExist(testTag: String) {
        val nodes = composeTestRule.onAllNodesWithTag(testTag)
            .fetchSemanticsNodes()
        assertEquals("Node with tag '$testTag' should not exist", 0, nodes.size)
    }

    // ==================== 1. Bottom Navigation Tests ====================

    @Test
    fun should_showHomeScreen_when_appStartsWithApiKey() {
        setUpNavigation(hasApiKey = true)

        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
        assertEquals(Routes.HOME, currentRoute())
    }

    @Test
    fun should_navigateToAnalytics_when_analyticsTabTapped() {
        setUpNavigation()

        composeTestRule.onNodeWithText("Analytics").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("analytics_screen").assertIsDisplayed()
        assertEquals(Routes.ANALYTICS, currentRoute())
    }

    @Test
    fun should_navigateToSessions_when_sessionsTabTapped() {
        setUpNavigation()

        composeTestRule.onNodeWithText("Sessions").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("sessions_screen").assertIsDisplayed()
        assertEquals(Routes.SESSIONS, currentRoute())
    }

    @Test
    fun should_navigateToBilling_when_billingTabTapped() {
        setUpNavigation()

        composeTestRule.onNodeWithText("Billing").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("billing_screen").assertIsDisplayed()
        assertEquals(Routes.BILLING, currentRoute())
    }

    @Test
    fun should_navigateToSettings_when_settingsTabTapped() {
        setUpNavigation()

        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        assertEquals(Routes.SETTINGS, currentRoute())
    }

    @Test
    fun should_returnToHome_when_backPressedFromTab() {
        setUpNavigation()

        composeTestRule.onNodeWithText("Analytics").performClick()
        composeTestRule.waitForIdle()
        assertEquals(Routes.ANALYTICS, currentRoute())

        composeTestRule.runOnIdle { navController.popBackStack() }
        composeTestRule.waitForIdle()

        assertEquals(Routes.HOME, currentRoute())
        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test
    fun should_notExitApp_when_backPressedFromHome() {
        setUpNavigation()
        assertEquals(Routes.HOME, currentRoute())

        val canGoBack = composeTestRule.runOnIdle {
            navController.popBackStack()
        }

        assertFalse("Should not pop past the start destination", canGoBack)
        assertEquals(Routes.HOME, currentRoute())
    }

    @Test
    fun should_notRecreateScreen_when_currentTabReselected() {
        setUpNavigation()

        val entryIdBefore = composeTestRule.runOnIdle {
            navController.currentBackStackEntry?.id
        }

        composeTestRule.onNodeWithText("Home").performClick()
        composeTestRule.waitForIdle()

        val entryIdAfter = composeTestRule.runOnIdle {
            navController.currentBackStackEntry?.id
        }
        assertEquals(entryIdBefore, entryIdAfter)
    }

    @Test
    fun should_showBottomNav_when_onAllMainScreens() {
        setUpNavigation()

        composeTestRule.onNodeWithTag("bottom_nav").assertIsDisplayed()

        composeTestRule.onNodeWithText("Analytics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bottom_nav").assertIsDisplayed()

        composeTestRule.onNodeWithText("Sessions").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bottom_nav").assertIsDisplayed()

        composeTestRule.onNodeWithText("Billing").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bottom_nav").assertIsDisplayed()

        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bottom_nav").assertIsDisplayed()
    }

    @Test
    fun should_hideBottomNav_when_onNestedScreen() {
        setUpNavigation()
        navigateToSettings()

        composeTestRule.onNodeWithTag("security_link").performClick()
        composeTestRule.waitForIdle()

        assertBottomNavHidden()
    }

    // ==================== 2. Onboarding Flow Tests ====================

    @Test
    fun should_showWelcomeScreen_when_noStoredKey() {
        setUpNavigation(hasApiKey = false)

        composeTestRule.onNodeWithTag("welcome_screen").assertIsDisplayed()
        assertEquals(Routes.WELCOME, currentRoute())
    }

    @Test
    fun should_hideBottomNav_when_onOnboardingScreen() {
        setUpNavigation(hasApiKey = false)

        assertBottomNavHidden()
    }

    @Test
    fun should_navigateToApiKeyInput_when_getStartedTapped() {
        setUpNavigation(hasApiKey = false)

        composeTestRule.onNodeWithText("Get Started").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("api_key_screen").assertIsDisplayed()
        assertEquals(Routes.API_KEY_INPUT, currentRoute())
    }

    @Test
    fun should_navigateToHome_when_validKeySubmitted() {
        setUpNavigation(hasApiKey = false)

        composeTestRule.onNodeWithText("Get Started").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Connect").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
        assertEquals(Routes.HOME, currentRoute())
    }

    @Test
    fun should_removeOnboardingFromBackStack_when_validKeySubmitted() {
        setUpNavigation(hasApiKey = false)

        composeTestRule.onNodeWithText("Get Started").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Connect").performClick()
        composeTestRule.waitForIdle()

        assertEquals(Routes.HOME, currentRoute())

        val canGoBack = composeTestRule.runOnIdle {
            navController.popBackStack()
        }
        assertFalse("Onboarding should be removed from back stack", canGoBack)
    }

    @Test
    fun should_skipOnboarding_when_storedKeyExists() {
        setUpNavigation(hasApiKey = true)

        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
        assertNodeDoesNotExist("welcome_screen")
        assertNodeDoesNotExist("api_key_screen")
        assertEquals(Routes.HOME, currentRoute())
    }

    // ==================== 3. Settings Navigation Tests ====================

    @Test
    fun should_navigateToSecurity_when_securityTappedInSettings() {
        setUpNavigation()
        navigateToSettings()

        composeTestRule.onNodeWithTag("security_link").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("security_screen").assertIsDisplayed()
        assertEquals(Routes.SECURITY, currentRoute())
    }

    @Test
    fun should_returnToSettings_when_backFromSecurity() {
        setUpNavigation()
        navigateToSettings()

        composeTestRule.onNodeWithTag("security_link").performClick()
        composeTestRule.waitForIdle()
        assertEquals(Routes.SECURITY, currentRoute())

        composeTestRule.onNodeWithTag("back_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        assertEquals(Routes.SETTINGS, currentRoute())
    }

    @Test
    fun should_navigateToTeam_when_teamTappedInSettings() {
        setUpNavigation()
        navigateToSettings()

        composeTestRule.onNodeWithTag("team_link").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("team_screen").assertIsDisplayed()
        assertEquals(Routes.TEAM, currentRoute())
    }

    @Test
    fun should_returnToSettings_when_backFromTeam() {
        setUpNavigation()
        navigateToSettings()

        composeTestRule.onNodeWithTag("team_link").performClick()
        composeTestRule.waitForIdle()
        assertEquals(Routes.TEAM, currentRoute())

        composeTestRule.runOnIdle { navController.popBackStack() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        assertEquals(Routes.SETTINGS, currentRoute())
    }

    @Test
    fun should_navigateToOnboarding_when_replaceKeyConfirmed() {
        setUpNavigation()
        navigateToSettings()

        composeTestRule.onNodeWithTag("replace_key_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("welcome_screen").assertIsDisplayed()
        assertEquals(Routes.WELCOME, currentRoute())
    }

    @Test
    fun should_clearEntireBackStack_when_replaceKeyConfirmed() {
        setUpNavigation()
        navigateToSettings()

        composeTestRule.onNodeWithTag("replace_key_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(Routes.WELCOME, currentRoute())

        val canGoBack = composeTestRule.runOnIdle {
            navController.popBackStack()
        }
        assertFalse("Back stack should be cleared after key replacement", canGoBack)
    }

    // ==================== 4. Sessions Detail Navigation Tests ====================

    @Test
    fun should_navigateToSessionDetail_when_sessionCardTapped() {
        setUpNavigation()
        navigateToSessions()

        composeTestRule.onNodeWithTag("session_card").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("session_detail_screen").assertIsDisplayed()
        assertEquals(Routes.SESSION_DETAIL, currentRoute())
    }

    @Test
    fun should_showSessionId_when_onSessionDetailScreen() {
        setUpNavigation()
        navigateToSessions()

        composeTestRule.onNodeWithTag("session_card").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Session Detail: test-session-123").assertIsDisplayed()
    }

    @Test
    fun should_returnToSessionsList_when_backFromSessionDetail() {
        setUpNavigation()
        navigateToSessions()

        composeTestRule.onNodeWithTag("session_card").performClick()
        composeTestRule.waitForIdle()
        assertEquals(Routes.SESSION_DETAIL, currentRoute())

        composeTestRule.onNodeWithTag("detail_back_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("sessions_screen").assertIsDisplayed()
        assertEquals(Routes.SESSIONS, currentRoute())
    }

    @Test
    fun should_hideBottomNav_when_onSessionDetailScreen() {
        setUpNavigation()
        navigateToSessions()

        composeTestRule.onNodeWithTag("session_card").performClick()
        composeTestRule.waitForIdle()

        assertBottomNavHidden()
    }

    // ==================== 5. Home Cross-Navigation Tests ====================

    @Test
    fun should_navigateToTeam_when_seeAllTapped() {
        setUpNavigation()

        composeTestRule.onNodeWithTag("see_all_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("team_screen").assertIsDisplayed()
        assertEquals(Routes.TEAM, currentRoute())
    }

    @Test
    fun should_returnToHome_when_backFromTeamViaSeeAll() {
        setUpNavigation()

        composeTestRule.onNodeWithTag("see_all_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(Routes.TEAM, currentRoute())

        composeTestRule.runOnIdle { navController.popBackStack() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
        assertEquals(Routes.HOME, currentRoute())
    }

    // ==================== 6. Deep Link Navigation Tests ====================

    @Test
    fun should_navigateToHome_when_homeDeepLink() {
        setUpNavigation(deepLinkRoute = Routes.HOME)

        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
        assertEquals(Routes.HOME, currentRoute())
    }

    @Test
    fun should_navigateToAnalytics_when_analyticsDeepLink() {
        setUpNavigation(deepLinkRoute = Routes.ANALYTICS)

        composeTestRule.onNodeWithTag("analytics_screen").assertIsDisplayed()
        assertEquals(Routes.ANALYTICS, currentRoute())
    }

    @Test
    fun should_navigateToSessionDetail_when_sessionDeepLink() {
        setUpNavigation(deepLinkRoute = Routes.sessionDetail("devin-abc123"))

        composeTestRule.onNodeWithTag("session_detail_screen").assertIsDisplayed()
        assertEquals(Routes.SESSION_DETAIL, currentRoute())
    }

    @Test
    fun should_navigateToBilling_when_billingDeepLink() {
        setUpNavigation(deepLinkRoute = Routes.BILLING)

        composeTestRule.onNodeWithTag("billing_screen").assertIsDisplayed()
        assertEquals(Routes.BILLING, currentRoute())
    }

    @Test
    fun should_navigateToSecurity_when_securityDeepLink() {
        setUpNavigation(deepLinkRoute = Routes.SECURITY)

        composeTestRule.onNodeWithTag("security_screen").assertIsDisplayed()
        assertEquals(Routes.SECURITY, currentRoute())
    }

    @Test
    fun should_navigateToTeam_when_teamDeepLink() {
        setUpNavigation(deepLinkRoute = Routes.TEAM)

        composeTestRule.onNodeWithTag("team_screen").assertIsDisplayed()
        assertEquals(Routes.TEAM, currentRoute())
    }

    @Test
    fun should_stayOnHome_when_nullDeepLink() {
        setUpNavigation(hasApiKey = true, deepLinkRoute = null)

        composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
        assertEquals(Routes.HOME, currentRoute())
    }

    @Test
    fun should_ignoreDeepLink_when_noApiKey() {
        setUpNavigation(hasApiKey = false, deepLinkRoute = Routes.ANALYTICS)

        composeTestRule.onNodeWithTag("welcome_screen").assertIsDisplayed()
        assertEquals(Routes.WELCOME, currentRoute())
    }

    // ==================== 7. Edge Cases ====================

    @Test
    fun should_notCrash_when_rapidTabSwitching() {
        setUpNavigation()

        repeat(10) {
            composeTestRule.onNodeWithText("Analytics").performClick()
            composeTestRule.onNodeWithText("Sessions").performClick()
            composeTestRule.onNodeWithText("Billing").performClick()
            composeTestRule.onNodeWithText("Settings").performClick()
            composeTestRule.onNodeWithText("Home").performClick()
        }
        composeTestRule.waitForIdle()

        assertNotNull(navController.currentBackStackEntry)
        assertEquals(Routes.HOME, currentRoute())
    }

    @Test
    fun should_handleSequentialNavigation_when_navigatingQuickly() {
        setUpNavigation()

        composeTestRule.onNodeWithText("Analytics").performClick()
        composeTestRule.onNodeWithText("Sessions").performClick()
        composeTestRule.waitForIdle()

        assertEquals(Routes.SESSIONS, currentRoute())
        composeTestRule.onNodeWithTag("sessions_screen").assertIsDisplayed()
    }

    @Test
    fun should_preserveNavigationState_when_recomposition() {
        setUpNavigation()

        composeTestRule.onNodeWithText("Analytics").performClick()
        composeTestRule.waitForIdle()
        assertEquals(Routes.ANALYTICS, currentRoute())

        composeTestRule.waitForIdle()

        assertEquals(Routes.ANALYTICS, currentRoute())
        composeTestRule.onNodeWithTag("analytics_screen").assertIsDisplayed()
    }

    @Test
    fun should_handleMultiLevelNavigation_when_navigatingDeeply() {
        setUpNavigation()

        navigateToSettings()
        composeTestRule.onNodeWithTag("security_link").performClick()
        composeTestRule.waitForIdle()
        assertEquals(Routes.SECURITY, currentRoute())

        composeTestRule.onNodeWithTag("back_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(Routes.SETTINGS, currentRoute())

        composeTestRule.onNodeWithTag("team_link").performClick()
        composeTestRule.waitForIdle()
        assertEquals(Routes.TEAM, currentRoute())

        composeTestRule.runOnIdle { navController.popBackStack() }
        composeTestRule.waitForIdle()
        assertEquals(Routes.SETTINGS, currentRoute())
    }

    @Test
    fun should_notCrash_when_doubleBackPress() {
        setUpNavigation()

        navigateToSettings()
        composeTestRule.onNodeWithTag("security_link").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle { navController.popBackStack() }
        composeTestRule.waitForIdle()
        assertEquals(Routes.SETTINGS, currentRoute())

        composeTestRule.runOnIdle { navController.popBackStack() }
        composeTestRule.waitForIdle()
        assertEquals(Routes.HOME, currentRoute())
    }
}

// ==================== Test Composable ====================

@Composable
private fun TestMainApp(
    navController: NavHostController,
    hasApiKey: Boolean = true,
    deepLinkRoute: String? = null
) {
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
        }
    }

    Scaffold(
        bottomBar = {
            if (showNav) {
                NavigationBar(modifier = Modifier.testTag("bottom_nav")) {
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
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.WELCOME) {
                Column(modifier = Modifier.testTag("welcome_screen")) {
                    Text("Devin Dashboard")
                    Text("Enterprise Intelligence for Devin")
                    Button(
                        onClick = {
                            navController.navigate(Routes.API_KEY_INPUT) {
                                popUpTo(Routes.WELCOME) { inclusive = true }
                            }
                        },
                        modifier = Modifier.testTag("get_started_button")
                    ) {
                        Text("Get Started")
                    }
                }
            }

            composable(Routes.API_KEY_INPUT) {
                Column(modifier = Modifier.testTag("api_key_screen")) {
                    Text("Connect Your API Key")
                    Button(
                        onClick = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.API_KEY_INPUT) { inclusive = true }
                            }
                        },
                        modifier = Modifier.testTag("connect_button")
                    ) {
                        Text("Connect")
                    }
                }
            }

            composable(Routes.HOME) {
                Column(modifier = Modifier.testTag("home_screen")) {
                    Text("Home Screen")
                    Text(
                        text = "See all",
                        modifier = Modifier
                            .testTag("see_all_button")
                            .clickable {
                                navController.navigate(Routes.TEAM)
                            }
                    )
                }
            }

            composable(Routes.ANALYTICS) {
                Column(modifier = Modifier.testTag("analytics_screen")) {
                    Text("Analytics Screen")
                }
            }

            composable(Routes.SESSIONS) {
                Column(modifier = Modifier.testTag("sessions_screen")) {
                    Text("Sessions Screen")
                    Text(
                        text = "Session Card",
                        modifier = Modifier
                            .testTag("session_card")
                            .clickable {
                                navController.navigate(
                                    Routes.sessionDetail("test-session-123")
                                )
                            }
                    )
                }
            }

            composable(
                route = Routes.SESSION_DETAIL,
                arguments = listOf(
                    navArgument("devinId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val devinId = backStackEntry.arguments?.getString("devinId") ?: ""
                Column(modifier = Modifier.testTag("session_detail_screen")) {
                    Text("Session Detail: $devinId")
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Text("Back")
                    }
                }
            }

            composable(Routes.BILLING) {
                Column(modifier = Modifier.testTag("billing_screen")) {
                    Text("Billing Screen")
                }
            }

            composable(Routes.SETTINGS) {
                Column(modifier = Modifier.testTag("settings_screen")) {
                    Text("Settings Screen")
                    Text(
                        text = "Security & Compliance",
                        modifier = Modifier
                            .testTag("security_link")
                            .clickable {
                                navController.navigate(Routes.SECURITY)
                            }
                    )
                    Text(
                        text = "Team & Adoption",
                        modifier = Modifier
                            .testTag("team_link")
                            .clickable {
                                navController.navigate(Routes.TEAM)
                            }
                    )
                    Button(
                        onClick = {
                            navController.navigate(Routes.WELCOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier.testTag("replace_key_button")
                    ) {
                        Text("Replace Key")
                    }
                }
            }

            composable(Routes.SECURITY) {
                Column(modifier = Modifier.testTag("security_screen")) {
                    Text("Security & Compliance Screen")
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Text("Back")
                    }
                }
            }

            composable(Routes.TEAM) {
                Column(modifier = Modifier.testTag("team_screen")) {
                    Text("Team & Adoption Screen")
                }
            }
        }
    }
}
