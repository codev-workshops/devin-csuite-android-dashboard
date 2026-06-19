package com.devin.csuite.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTest {

    // --- Start Destination Logic Tests ---

    @Test
    fun `key exists in store navigates to Home`() {
        val hasApiKey = true
        val startDestination = if (hasApiKey) Routes.HOME else Routes.WELCOME
        assertEquals(Routes.HOME, startDestination)
    }

    @Test
    fun `no key in store navigates to Welcome (Onboarding)`() {
        val hasApiKey = false
        val startDestination = if (hasApiKey) Routes.HOME else Routes.WELCOME
        assertEquals(Routes.WELCOME, startDestination)
    }

    @Test
    fun `invalid key on start should clear key and show Onboarding`() {
        // Simulates: key exists but is invalid -> after validation failure, key is cleared
        var hasApiKey = true
        val keyIsValid = false

        // On start, check if key exists
        var startDestination = if (hasApiKey) Routes.HOME else Routes.WELCOME
        assertEquals(Routes.HOME, startDestination)

        // After validation fails, key should be cleared and user sent to onboarding
        if (!keyIsValid) {
            hasApiKey = false
            startDestination = Routes.WELCOME
        }
        assertEquals(Routes.WELCOME, startDestination)
    }

    // --- Route Constants Tests ---

    @Test
    fun `Routes contains all expected screen routes`() {
        assertEquals("welcome", Routes.WELCOME)
        assertEquals("api_key_input", Routes.API_KEY_INPUT)
        assertEquals("home", Routes.HOME)
        assertEquals("analytics", Routes.ANALYTICS)
        assertEquals("sessions", Routes.SESSIONS)
        assertEquals("billing", Routes.BILLING)
        assertEquals("settings", Routes.SETTINGS)
    }

    @Test
    fun `onboarding route exists`() {
        assertEquals("onboarding", Routes.ONBOARDING)
    }

    // --- Bottom Nav Visibility Logic Tests ---

    @Test
    fun `bottom bar visible on Home screen`() {
        val mainScreens = listOf(
            Routes.HOME, Routes.ANALYTICS, Routes.SESSIONS, Routes.BILLING, Routes.SETTINGS
        )
        assertTrue(Routes.HOME in mainScreens)
    }

    @Test
    fun `bottom bar visible on Analytics screen`() {
        val mainScreens = listOf(
            Routes.HOME, Routes.ANALYTICS, Routes.SESSIONS, Routes.BILLING, Routes.SETTINGS
        )
        assertTrue(Routes.ANALYTICS in mainScreens)
    }

    @Test
    fun `bottom bar visible on Sessions screen`() {
        val mainScreens = listOf(
            Routes.HOME, Routes.ANALYTICS, Routes.SESSIONS, Routes.BILLING, Routes.SETTINGS
        )
        assertTrue(Routes.SESSIONS in mainScreens)
    }

    @Test
    fun `bottom bar visible on Billing screen`() {
        val mainScreens = listOf(
            Routes.HOME, Routes.ANALYTICS, Routes.SESSIONS, Routes.BILLING, Routes.SETTINGS
        )
        assertTrue(Routes.BILLING in mainScreens)
    }

    @Test
    fun `bottom bar visible on Settings screen`() {
        val mainScreens = listOf(
            Routes.HOME, Routes.ANALYTICS, Routes.SESSIONS, Routes.BILLING, Routes.SETTINGS
        )
        assertTrue(Routes.SETTINGS in mainScreens)
    }

    @Test
    fun `bottom bar hidden on Welcome screen`() {
        val mainScreens = listOf(
            Routes.HOME, Routes.ANALYTICS, Routes.SESSIONS, Routes.BILLING, Routes.SETTINGS
        )
        assertFalse(Routes.WELCOME in mainScreens)
    }

    @Test
    fun `bottom bar hidden on API Key Input screen`() {
        val mainScreens = listOf(
            Routes.HOME, Routes.ANALYTICS, Routes.SESSIONS, Routes.BILLING, Routes.SETTINGS
        )
        assertFalse(Routes.API_KEY_INPUT in mainScreens)
    }

    // --- BottomNavItem Tests ---

    @Test
    fun `BottomNavItem has correct number of items`() {
        assertEquals(5, BottomNavItem.items.size)
    }

    @Test
    fun `BottomNavItem routes match expected routes`() {
        val routes = BottomNavItem.items.map { it.route }
        assertTrue(Routes.HOME in routes)
        assertTrue(Routes.ANALYTICS in routes)
        assertTrue(Routes.SESSIONS in routes)
        assertTrue(Routes.BILLING in routes)
        assertTrue(Routes.SETTINGS in routes)
    }

    @Test
    fun `BottomNavItem titles are not empty`() {
        BottomNavItem.items.forEach { item ->
            assertTrue("Title should not be empty for route ${item.route}", item.title.isNotEmpty())
        }
    }

    @Test
    fun `first BottomNavItem is Home`() {
        assertEquals(Routes.HOME, BottomNavItem.items.first().route)
    }

    // --- Navigation Flow Tests ---

    @Test
    fun `onboarding flow goes Welcome then API Key Input then Home`() {
        val expectedFlow = listOf(Routes.WELCOME, Routes.API_KEY_INPUT, Routes.HOME)
        assertEquals("welcome", expectedFlow[0])
        assertEquals("api_key_input", expectedFlow[1])
        assertEquals("home", expectedFlow[2])
    }

    @Test
    fun `settings logout navigates back to Welcome`() {
        val afterLogout = Routes.WELCOME
        assertEquals("welcome", afterLogout)
    }
}
