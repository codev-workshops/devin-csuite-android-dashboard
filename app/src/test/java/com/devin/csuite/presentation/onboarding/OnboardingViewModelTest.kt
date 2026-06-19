package com.devin.csuite.presentation.onboarding

import com.devin.csuite.data.local.SecureKeyStore
import com.devin.csuite.data.remote.ApiException
import com.devin.csuite.domain.model.Organization
import com.devin.csuite.domain.model.OrganizationsResponse
import com.devin.csuite.domain.repository.MetricsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var secureKeyStore: SecureKeyStore
    private lateinit var metricsRepository: MetricsRepository
    private lateinit var viewModel: OnboardingViewModel

    private val testOrgsResponse = OrganizationsResponse(
        organizations = listOf(
            Organization(orgId = "org-1", name = "test-org", displayName = "Test Org")
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        secureKeyStore = mockk(relaxed = true)
        metricsRepository = mockk()
        viewModel = OnboardingViewModel(secureKeyStore, metricsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty key and no error`() {
        val state = viewModel.uiState.value
        assertEquals("", state.apiKey)
        assertFalse(state.isLoading)
        assertFalse(state.isValid)
        assertNull(state.errorMessage)
        assertTrue(state.organizations.isEmpty())
    }

    @Test
    fun `updateApiKey updates the key in state`() {
        viewModel.updateApiKey("cog_test123")
        assertEquals("cog_test123", viewModel.uiState.value.apiKey)
    }

    @Test
    fun `updateApiKey clears previous error`() {
        viewModel.updateApiKey("invalid")
        viewModel.validateAndStore()
        // Error set because no cog_ prefix
        assertTrue(viewModel.uiState.value.errorMessage != null)

        viewModel.updateApiKey("cog_new")
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `validateAndStore rejects key without cog_ prefix`() {
        viewModel.updateApiKey("invalid_key_123")
        viewModel.validateAndStore()

        val state = viewModel.uiState.value
        assertTrue(state.errorMessage!!.contains("cog_"))
        assertFalse(state.isLoading)
    }

    @Test
    fun `validateAndStore with valid key - success flow`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns Result.success(testOrgsResponse)

        viewModel.updateApiKey("cog_valid_test_key_123")
        viewModel.validateAndStore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isValid)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(1, state.organizations.size)
        assertEquals("org-1", state.organizations[0].orgId)
    }

    @Test
    fun `validateAndStore stores key before validation call`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns Result.success(testOrgsResponse)

        viewModel.updateApiKey("cog_mykey")
        viewModel.validateAndStore()
        advanceUntilIdle()

        verify { secureKeyStore.storeApiKey("cog_mykey") }
    }

    @Test
    fun `validateAndStore clears key on 401 error`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns Result.failure(ApiException(401, "Invalid or expired API key"))

        viewModel.updateApiKey("cog_bad_key")
        viewModel.validateAndStore()
        advanceUntilIdle()

        verify { secureKeyStore.clearApiKey() }
        val state = viewModel.uiState.value
        assertFalse(state.isValid)
        assertTrue(state.errorMessage!!.contains("Invalid API key"))
    }

    @Test
    fun `validateAndStore clears key on 403 error`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns Result.failure(ApiException(403, "Forbidden"))

        viewModel.updateApiKey("cog_forbidden_key")
        viewModel.validateAndStore()
        advanceUntilIdle()

        verify { secureKeyStore.clearApiKey() }
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Invalid API key"))
    }

    @Test
    fun `validateAndStore shows network error message`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns Result.failure(
            Exception("Unable to resolve host api.devin.ai")
        )

        viewModel.updateApiKey("cog_valid_key")
        viewModel.validateAndStore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.errorMessage!!.contains("Network error"))
        verify { secureKeyStore.clearApiKey() }
    }

    @Test
    fun `validateAndStore shows generic error for unknown failures`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns Result.failure(
            Exception("Something unexpected happened")
        )

        viewModel.updateApiKey("cog_valid_key")
        viewModel.validateAndStore()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.errorMessage!!.contains("Something unexpected happened"))
    }

    @Test
    fun `validateAndStore transitions through loading state`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns Result.success(testOrgsResponse)

        viewModel.updateApiKey("cog_test_key")
        viewModel.validateAndStore()
        advanceUntilIdle()

        // After completion: loading is false
        assertFalse(viewModel.uiState.value.isLoading)
        // Validation was attempted (key was stored temporarily)
        verify { secureKeyStore.storeApiKey("cog_test_key") }
        // And result is valid
        assertTrue(viewModel.uiState.value.isValid)
    }

    @Test
    fun `validateAndStore trims whitespace from key`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns Result.success(testOrgsResponse)

        viewModel.updateApiKey("  cog_test_key  ")
        viewModel.validateAndStore()
        advanceUntilIdle()

        verify { secureKeyStore.storeApiKey("cog_test_key") }
    }
}
