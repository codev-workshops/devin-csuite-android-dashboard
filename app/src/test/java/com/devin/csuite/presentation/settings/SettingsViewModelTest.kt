package com.devin.csuite.presentation.settings

import com.devin.csuite.data.local.PreferencesManager
import com.devin.csuite.data.local.SecureKeyStore
import com.devin.csuite.data.local.datasource.LocalMetricsDataSource
import com.devin.csuite.data.local.datasource.LocalSessionsDataSource
import com.devin.csuite.domain.model.OrganizationsResponse
import com.devin.csuite.domain.repository.MetricsRepository
import com.devin.csuite.notification.NotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var secureKeyStore: SecureKeyStore
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var metricsRepository: MetricsRepository
    private lateinit var localMetrics: LocalMetricsDataSource
    private lateinit var localSessions: LocalSessionsDataSource
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        secureKeyStore = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)
        metricsRepository = mockk()
        localMetrics = mockk(relaxed = true)
        localSessions = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)

        every { secureKeyStore.getApiKey() } returns "cog_test_key_1234567890"
        every { preferencesManager.themeMode } returns flowOf("dark")
        every { preferencesManager.refreshInterval } returns flowOf(5)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads settings and masks API key`() = runTest {
        viewModel = SettingsViewModel(secureKeyStore, preferencesManager, metricsRepository, localMetrics, localSessions, notificationHelper)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.maskedApiKey.startsWith("cog_"))
        assertTrue(state.maskedApiKey.endsWith("7890"))
        assertTrue(state.maskedApiKey.contains("*"))
        assertEquals("dark", state.themeMode)
        assertEquals(5, state.refreshInterval)
    }

    @Test
    fun `API key masking shows first 4 and last 4 chars`() = runTest {
        every { secureKeyStore.getApiKey() } returns "cog_abcdefghijklmnop"
        viewModel = SettingsViewModel(secureKeyStore, preferencesManager, metricsRepository, localMetrics, localSessions, notificationHelper)
        advanceUntilIdle()

        val masked = viewModel.uiState.value.maskedApiKey
        assertEquals("cog_", masked.take(4))
        assertEquals("mnop", masked.takeLast(4))
        assertTrue(masked.contains("*"))
    }

    @Test
    fun `short API key shows masked placeholder`() = runTest {
        every { secureKeyStore.getApiKey() } returns "cog_abc"
        viewModel = SettingsViewModel(secureKeyStore, preferencesManager, metricsRepository, localMetrics, localSessions, notificationHelper)
        advanceUntilIdle()

        assertEquals("****", viewModel.uiState.value.maskedApiKey)
    }

    @Test
    fun `no API key shows Not set`() = runTest {
        every { secureKeyStore.getApiKey() } returns null
        viewModel = SettingsViewModel(secureKeyStore, preferencesManager, metricsRepository, localMetrics, localSessions, notificationHelper)
        advanceUntilIdle()

        assertEquals("Not set", viewModel.uiState.value.maskedApiKey)
    }

    @Test
    fun `setThemeMode updates state and persists`() = runTest {
        viewModel = SettingsViewModel(secureKeyStore, preferencesManager, metricsRepository, localMetrics, localSessions, notificationHelper)
        advanceUntilIdle()

        viewModel.setThemeMode("light")
        advanceUntilIdle()

        assertEquals("light", viewModel.uiState.value.themeMode)
        coVerify { preferencesManager.setThemeMode("light") }
    }

    @Test
    fun `setRefreshInterval updates state and persists`() = runTest {
        viewModel = SettingsViewModel(secureKeyStore, preferencesManager, metricsRepository, localMetrics, localSessions, notificationHelper)
        advanceUntilIdle()

        viewModel.setRefreshInterval(15)
        advanceUntilIdle()

        assertEquals(15, viewModel.uiState.value.refreshInterval)
        coVerify { preferencesManager.setRefreshInterval(15) }
    }

    @Test
    fun `showReplaceDialog sets flag true`() = runTest {
        viewModel = SettingsViewModel(secureKeyStore, preferencesManager, metricsRepository, localMetrics, localSessions, notificationHelper)
        advanceUntilIdle()

        viewModel.showReplaceDialog()
        assertTrue(viewModel.uiState.value.showReplaceDialog)
    }

    @Test
    fun `dismissReplaceDialog sets flag false`() = runTest {
        viewModel = SettingsViewModel(secureKeyStore, preferencesManager, metricsRepository, localMetrics, localSessions, notificationHelper)
        advanceUntilIdle()

        viewModel.showReplaceDialog()
        assertTrue(viewModel.uiState.value.showReplaceDialog)

        viewModel.dismissReplaceDialog()
        assertFalse(viewModel.uiState.value.showReplaceDialog)
    }

    @Test
    fun `replaceApiKey clears key and dismisses dialog`() = runTest {
        viewModel = SettingsViewModel(secureKeyStore, preferencesManager, metricsRepository, localMetrics, localSessions, notificationHelper)
        advanceUntilIdle()

        viewModel.showReplaceDialog()
        viewModel.replaceApiKey()

        verify { secureKeyStore.clearApiKey() }
        assertFalse(viewModel.uiState.value.showReplaceDialog)
    }

    @Test
    fun `validateApiKey success sets Valid result`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns Result.success(OrganizationsResponse())
        viewModel = SettingsViewModel(secureKeyStore, preferencesManager, metricsRepository, localMetrics, localSessions, notificationHelper)
        advanceUntilIdle()

        viewModel.validateApiKey()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isValidating)
        assertEquals("Valid", state.validationResult)
    }

    @Test
    fun `validateApiKey failure sets Invalid result`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns Result.failure(Exception("Auth failed"))
        viewModel = SettingsViewModel(secureKeyStore, preferencesManager, metricsRepository, localMetrics, localSessions, notificationHelper)
        advanceUntilIdle()

        viewModel.validateApiKey()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isValidating)
        assertTrue(state.validationResult!!.contains("Invalid"))
        assertTrue(state.validationResult!!.contains("Auth failed"))
    }

    @Test
    fun `validateApiKey transitions through validating state`() = runTest {
        coEvery { metricsRepository.validateApiKey() } returns Result.success(OrganizationsResponse())
        viewModel = SettingsViewModel(secureKeyStore, preferencesManager, metricsRepository, localMetrics, localSessions, notificationHelper)
        advanceUntilIdle()

        viewModel.validateApiKey()
        advanceUntilIdle()

        // After completion: not validating and result is set
        assertFalse(viewModel.uiState.value.isValidating)
        assertEquals("Valid", viewModel.uiState.value.validationResult)
    }
}
