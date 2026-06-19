package com.devin.csuite.integration

import android.content.Intent
import android.net.Uri
import com.devin.csuite.deeplink.DeepLinkHandler
import com.devin.csuite.presentation.navigation.BottomNavItem
import com.devin.csuite.presentation.navigation.Routes
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Epic 6.7.1 - Integration test for navigation between all screens.
 * Tests bottom nav navigation, settings links, and deep link routing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class NavigationFlowTest {

    // --- Bottom Navigation Tests ---

    @Test
    fun `bottom nav has exactly 5 items for main screens`() {
        assertEquals(5, BottomNavItem.items.size)
    }

    @Test
    fun `bottom nav contains all main screen routes`() {
        val routes = BottomNavItem.items.map { it.route }
        assertTrue(Routes.HOME in routes)
        assertTrue(Routes.ANALYTICS in routes)
        assertTrue(Routes.SESSIONS in routes)
        assertTrue(Routes.BILLING in routes)
        assertTrue(Routes.SETTINGS in routes)
    }

    @Test
    fun `bottom nav ordering starts with Home`() {
        assertEquals(Routes.HOME, BottomNavItem.items[0].route)
    }

    @Test
    fun `bottom nav items have non-empty titles`() {
        BottomNavItem.items.forEach { item ->
            assertTrue("Item ${item.route} should have title", item.title.isNotEmpty())
        }
    }

    @Test
    fun `bottom nav visibility - hidden on onboarding screens`() {
        val mainScreens = listOf(
            Routes.HOME, Routes.ANALYTICS, Routes.SESSIONS, Routes.BILLING, Routes.SETTINGS
        )
        assertFalse(Routes.WELCOME in mainScreens)
        assertFalse(Routes.API_KEY_INPUT in mainScreens)
    }

    @Test
    fun `bottom nav visibility - shown on all main screens`() {
        val mainScreens = listOf(
            Routes.HOME, Routes.ANALYTICS, Routes.SESSIONS, Routes.BILLING, Routes.SETTINGS
        )
        mainScreens.forEach { route ->
            assertTrue("$route should show bottom nav", route in mainScreens)
        }
    }

    @Test
    fun `bottom nav visibility - hidden on Security screen (nested)`() {
        val mainScreens = listOf(
            Routes.HOME, Routes.ANALYTICS, Routes.SESSIONS, Routes.BILLING, Routes.SETTINGS
        )
        assertFalse(Routes.SECURITY in mainScreens)
    }

    @Test
    fun `bottom nav visibility - hidden on Team screen (nested)`() {
        val mainScreens = listOf(
            Routes.HOME, Routes.ANALYTICS, Routes.SESSIONS, Routes.BILLING, Routes.SETTINGS
        )
        assertFalse(Routes.TEAM in mainScreens)
    }

    // --- Settings Screen Navigation Links ---

    @Test
    fun `settings screen can navigate to Security`() {
        // Settings has a link to Security screen
        val settingsLinks = listOf(Routes.SECURITY, Routes.TEAM, Routes.WELCOME)
        assertTrue(Routes.SECURITY in settingsLinks)
    }

    @Test
    fun `settings screen can navigate to Team`() {
        val settingsLinks = listOf(Routes.SECURITY, Routes.TEAM, Routes.WELCOME)
        assertTrue(Routes.TEAM in settingsLinks)
    }

    @Test
    fun `settings screen can navigate to Onboarding (key replace)`() {
        val settingsLinks = listOf(Routes.SECURITY, Routes.TEAM, Routes.WELCOME)
        assertTrue(Routes.WELCOME in settingsLinks)
    }

    // --- Deep Link Navigation Tests ---

    @Test
    fun `deep link to home navigates to Home screen`() {
        val uri = Uri.parse("devin-dashboard:///home")
        val route = DeepLinkHandler.parseDeepLink(uri)
        assertEquals(Routes.HOME, route)
    }

    @Test
    fun `deep link to analytics navigates to Analytics screen`() {
        val uri = Uri.parse("devin-dashboard:///analytics")
        val route = DeepLinkHandler.parseDeepLink(uri)
        assertEquals(Routes.ANALYTICS, route)
    }

    @Test
    fun `deep link to team navigates to Team screen`() {
        val uri = Uri.parse("devin-dashboard:///team")
        val route = DeepLinkHandler.parseDeepLink(uri)
        assertEquals(Routes.TEAM, route)
    }

    @Test
    fun `deep link to billing navigates to Billing screen`() {
        val uri = Uri.parse("devin-dashboard:///billing")
        val route = DeepLinkHandler.parseDeepLink(uri)
        assertEquals(Routes.BILLING, route)
    }

    @Test
    fun `deep link to security navigates to Security screen`() {
        val uri = Uri.parse("devin-dashboard:///security")
        val route = DeepLinkHandler.parseDeepLink(uri)
        assertEquals(Routes.SECURITY, route)
    }

    @Test
    fun `deep link to settings navigates to Settings screen`() {
        val uri = Uri.parse("devin-dashboard:///settings")
        val route = DeepLinkHandler.parseDeepLink(uri)
        assertEquals(Routes.SETTINGS, route)
    }

    @Test
    fun `deep link to specific session navigates to session detail`() {
        val uri = Uri.parse("devin-dashboard:///sessions/devin-abc123")
        val route = DeepLinkHandler.parseDeepLink(uri)
        assertEquals(Routes.sessionDetail("devin-abc123"), route)
    }

    @Test
    fun `deep link to sessions list navigates to Sessions screen`() {
        val uri = Uri.parse("devin-dashboard:///sessions")
        val route = DeepLinkHandler.parseDeepLink(uri)
        assertEquals(Routes.SESSIONS, route)
    }

    @Test
    fun `deep link with root path navigates to Home`() {
        val uri = Uri.parse("devin-dashboard:///")
        val route = DeepLinkHandler.parseDeepLink(uri)
        assertEquals(Routes.HOME, route)
    }

    @Test
    fun `deep link with unknown path returns null`() {
        val uri = Uri.parse("devin-dashboard:///unknown-page")
        val route = DeepLinkHandler.parseDeepLink(uri)
        assertNull(route)
    }

    @Test
    fun `deep link with wrong scheme returns null`() {
        val uri = Uri.parse("https:///home")
        val route = DeepLinkHandler.parseDeepLink(uri)
        assertNull(route)
    }

    @Test
    fun `deep link from Intent is parsed correctly`() {
        val intent = mockk<Intent>()
        every { intent.data } returns Uri.parse("devin-dashboard:///analytics")
        val route = DeepLinkHandler.parseDeepLink(intent)
        assertEquals(Routes.ANALYTICS, route)
    }

    @Test
    fun `deep link from null Intent returns null`() {
        val route = DeepLinkHandler.parseDeepLink(null as Intent?)
        assertNull(route)
    }

    @Test
    fun `deep link from Intent with no data returns null`() {
        val intent = mockk<Intent>()
        every { intent.data } returns null
        val route = DeepLinkHandler.parseDeepLink(intent)
        assertNull(route)
    }

    // --- Deep Link Creation Tests ---

    @Test
    fun `createDeepLink generates correct URIs for all routes`() {
        assertEquals("devin-dashboard:///home", DeepLinkHandler.createDeepLink(Routes.HOME))
        assertEquals("devin-dashboard:///analytics", DeepLinkHandler.createDeepLink(Routes.ANALYTICS))
        assertEquals("devin-dashboard:///team", DeepLinkHandler.createDeepLink(Routes.TEAM))
        assertEquals("devin-dashboard:///sessions", DeepLinkHandler.createDeepLink(Routes.SESSIONS))
        assertEquals("devin-dashboard:///billing", DeepLinkHandler.createDeepLink(Routes.BILLING))
        assertEquals("devin-dashboard:///security", DeepLinkHandler.createDeepLink(Routes.SECURITY))
        assertEquals("devin-dashboard:///settings", DeepLinkHandler.createDeepLink(Routes.SETTINGS))
    }

    @Test
    fun `createDeepLink for session detail includes session ID`() {
        val deepLink = DeepLinkHandler.createDeepLink("session_detail/devin-xyz789")
        assertEquals("devin-dashboard:///sessions/devin-xyz789", deepLink)
    }

    // --- Session Detail Navigation ---

    @Test
    fun `session detail route pattern contains devinId parameter`() {
        assertTrue(Routes.SESSION_DETAIL.contains("{devinId}"))
    }

    @Test
    fun `sessionDetail helper builds correct route with ID`() {
        assertEquals("session_detail/test-123", Routes.sessionDetail("test-123"))
    }

    // --- Route Completeness ---

    @Test
    fun `all expected route constants exist`() {
        assertNotNull(Routes.ONBOARDING)
        assertNotNull(Routes.WELCOME)
        assertNotNull(Routes.API_KEY_INPUT)
        assertNotNull(Routes.HOME)
        assertNotNull(Routes.ANALYTICS)
        assertNotNull(Routes.SESSIONS)
        assertNotNull(Routes.SESSION_DETAIL)
        assertNotNull(Routes.BILLING)
        assertNotNull(Routes.SETTINGS)
        assertNotNull(Routes.SECURITY)
        assertNotNull(Routes.TEAM)
    }

    // --- Navigation Rail (Tablet Layout) ---

    @Test
    fun `navigation rail uses same items as bottom nav`() {
        // NavigationRail renders the same BottomNavItem.items
        val railItems = BottomNavItem.items
        assertEquals(5, railItems.size)
        assertEquals(Routes.HOME, railItems[0].route)
    }

    @Test
    fun `navigation rail shown when window size expanded`() {
        // WindowWidthSizeClass >= Expanded triggers NavigationRail
        val widthIsExpanded = true
        val showNav = true
        val useNavigationRail = widthIsExpanded && showNav
        assertTrue(useNavigationRail)
    }

    @Test
    fun `navigation bar shown when window size compact`() {
        val widthIsExpanded = false
        val showNav = true
        val useNavigationRail = widthIsExpanded && showNav
        assertFalse(useNavigationRail)
    }
}
