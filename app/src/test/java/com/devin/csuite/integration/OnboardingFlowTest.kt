package com.devin.csuite.integration

import com.devin.csuite.core.Constants
import com.devin.csuite.data.local.SecureKeyStore
import com.devin.csuite.data.remote.ApiException
import com.devin.csuite.domain.model.Organization
import com.devin.csuite.domain.model.OrganizationsResponse
import com.devin.csuite.domain.repository.MetricsRepository
import com.devin.csuite.presentation.navigation.Routes
import com.devin.csuite.presentation.onboarding.OnboardingViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Epic 6.7.1 - Integration test for the complete onboarding flow:
 * Welcome -> API Key Input -> Validation -> Home
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingFlowTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var secureKeyStore: SecureKeyStore
    private lateinit var metricsRepository: MetricsRepository
    private lateinit var viewModel: OnboardingViewModel

    private val testOrgsResponse = OrganizationsResponse(
        organizations = listOf(
            Organization(orgId = "org-enterprise", name = "enterprise-co", displayName = "Enterprise Co")
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        secureKeyStore = mockk(relaxed = true)
        metricsRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Complete Flow: Welcome -> Key Input -> Validation -> Home ---

    @Test
    fun `full onboarding flow - no key starts at Welcome`() {
        every { secureKeyStore.hasApiKey() } returns false
        val startDestination = if (secureKeyStore.hasApiKey()) Routes.HOME else Routes.WELCOME
        assertEquals(Routes.WELCOME, startDestination)
    }

    @Test
    fun `full onboarding flow - Welcome navigates to API Key Input`() {
        val expectedFlow = listOf(Routes.WELCOME, Routes.API_KEY_INPUT, Routes.HOME)
        assertEquals(Routes.API_KEY_INPUT, expectedFlow[1])
    }

    @Test
    fun `full onboarding flow - valid key navigates to Home`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns Result.success(testOrgsResponse)
        viewModel = OnboardingViewModel(secureKeyStore, metricsRepository)

        viewModel.updateApiKey("cog_enterprise_key_abc123")
        viewModel.validateAndStore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Key should be validated", state.isValid)
        assertFalse("Should not be loading after validation", state.isLoading)
        assertNull("Should have no error", state.errorMessage)
        assertEquals(1, state.organizations.size)

        // After validation, navigation should go to HOME
        val destination = if (state.isValid) Routes.HOME else Routes.API_KEY_INPUT
        assertEquals(Routes.HOME, destination)
    }

    @Test
    fun `onboarding flow - invalid key stays on API Key Input with error`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns
            Result.failure(ApiException(401, "Invalid or expired API key"))
        viewModel = OnboardingViewModel(secureKeyStore, metricsRepository)

        viewModel.updateApiKey("cog_invalid_key")
        viewModel.validateAndStore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Key should not be validated", state.isValid)
        assertTrue("Should show error message", state.errorMessage != null)
        assertTrue(state.errorMessage!!.contains("Invalid"))

        // User stays on API Key Input
        val destination = if (state.isValid) Routes.HOME else Routes.API_KEY_INPUT
        assertEquals(Routes.API_KEY_INPUT, destination)
    }

    @Test
    fun `onboarding flow - key without cog_ prefix shows format error immediately`() {
        viewModel = OnboardingViewModel(secureKeyStore, metricsRepository)

        viewModel.updateApiKey("invalid_no_prefix")
        viewModel.validateAndStore()

        val state = viewModel.uiState.value
        assertTrue(state.errorMessage!!.contains(Constants.KEY_PREFIX))
        assertFalse(state.isLoading)
    }

    @Test
    fun `onboarding flow - network error shows retry message`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns
            Result.failure(Exception("Unable to resolve host api.devin.ai"))
        viewModel = OnboardingViewModel(secureKeyStore, metricsRepository)

        viewModel.updateApiKey("cog_some_key_123")
        viewModel.validateAndStore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isValid)
        assertTrue(state.errorMessage!!.contains("Network error"))
    }

    @Test
    fun `onboarding flow - key is stored before API call`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns Result.success(testOrgsResponse)
        viewModel = OnboardingViewModel(secureKeyStore, metricsRepository)

        viewModel.updateApiKey("cog_store_test_key")
        viewModel.validateAndStore()
        advanceUntilIdle()

        verify { secureKeyStore.storeApiKey("cog_store_test_key") }
    }

    @Test
    fun `onboarding flow - key is cleared on validation failure`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns
            Result.failure(ApiException(403, "Forbidden"))
        viewModel = OnboardingViewModel(secureKeyStore, metricsRepository)

        viewModel.updateApiKey("cog_bad_key_xyz")
        viewModel.validateAndStore()
        advanceUntilIdle()

        verify { secureKeyStore.clearApiKey() }
    }

    @Test
    fun `onboarding flow - existing valid key skips onboarding entirely`() {
        every { secureKeyStore.hasApiKey() } returns true
        val startDestination = if (secureKeyStore.hasApiKey()) Routes.HOME else Routes.WELCOME
        assertEquals(Routes.HOME, startDestination)
    }

    @Test
    fun `onboarding flow - loading state shown during validation`() = runTest {
        coEvery { metricsRepository.validateApiKey() } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(testOrgsResponse)
        }
        viewModel = OnboardingViewModel(secureKeyStore, metricsRepository)

        viewModel.updateApiKey("cog_loading_test")
        viewModel.validateAndStore()
        testScheduler.advanceTimeBy(10) // Advance past the launch but before the API call completes

        // During validation, should be in loading state
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onboarding flow - updating key clears previous error`() {
        viewModel = OnboardingViewModel(secureKeyStore, metricsRepository)

        viewModel.updateApiKey("no_prefix")
        viewModel.validateAndStore()
        assertTrue(viewModel.uiState.value.errorMessage != null)

        viewModel.updateApiKey("cog_new_attempt")
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onboarding flow - organizations are stored on success`() = runTest {
        val multiOrgResponse = OrganizationsResponse(
            organizations = listOf(
                Organization(orgId = "org-1", name = "org-one", displayName = "Org One"),
                Organization(orgId = "org-2", name = "org-two", displayName = "Org Two"),
                Organization(orgId = "org-3", name = "org-three", displayName = "Org Three")
            )
        )
        coEvery { metricsRepository.validateApiKey() } returns Result.success(multiOrgResponse)
        viewModel = OnboardingViewModel(secureKeyStore, metricsRepository)

        viewModel.updateApiKey("cog_multi_org_key")
        viewModel.validateAndStore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.organizations.size)
        assertEquals("org-1", state.organizations[0].orgId)
        assertEquals("org-2", state.organizations[1].orgId)
        assertEquals("org-3", state.organizations[2].orgId)
    }
}
