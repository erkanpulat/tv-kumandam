package com.erkanpulat.tvkumandam.presentation.navigation

import com.erkanpulat.tvkumandam.domain.preferences.RemotePreferences
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `first launch opens Welcome before TV setup`() = runTest(dispatcher) {
        val preferences = DeferredPreferences()
        val viewModel = AppViewModel(preferences)

        preferences.emit(RemoteSettings())
        runCurrent()

        assertEquals(AppDestination.Welcome, viewModel.uiState.value.destination)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `routing waits for the first preferences emission then opens Add TV with no device`() = runTest(dispatcher) {
        val preferences = DeferredPreferences()
        val viewModel = AppViewModel(preferences)

        runCurrent()
        assertNull(viewModel.uiState.value.destination)
        assertEquals(true, viewModel.uiState.value.isLoading)

        preferences.emit(RemoteSettings(onboardingCompleted = true))
        runCurrent()

        assertEquals(AppDestination.AddDevice, viewModel.uiState.value.destination)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `an existing saved TV launches Remote and explicit destinations are retained`() = runTest(dispatcher) {
        val remote = SavedRemote("salon", "Salon", ArcelikOldLcdProfile.ID)
        val preferences = DeferredPreferences()
        val viewModel = AppViewModel(preferences)
        preferences.emit(RemoteSettings(listOf(remote), remote.id))
        runCurrent()

        assertEquals(AppDestination.Remote, viewModel.uiState.value.destination)
        viewModel.navigate(AppDestination.Devices)
        assertEquals(AppDestination.Devices, viewModel.uiState.value.destination)

        preferences.emit(RemoteSettings())
        runCurrent()
        assertEquals(AppDestination.AddDevice, viewModel.uiState.value.destination)
    }

    @Test
    fun `an unrelated settings echo cannot eject an active Add TV flow`() = runTest(dispatcher) {
        val remote = SavedRemote("salon", "Salon", ArcelikOldLcdProfile.ID)
        val preferences = DeferredPreferences()
        val viewModel = AppViewModel(preferences)
        val initial = RemoteSettings(listOf(remote), remote.id)
        preferences.emit(initial)
        runCurrent()

        viewModel.navigate(AppDestination.AddDevice)
        preferences.emit(initial.copy(hapticsEnabled = false))
        runCurrent()

        assertEquals(AppDestination.AddDevice, viewModel.uiState.value.destination)
        assertFalse(viewModel.uiState.value.settings.hapticsEnabled)
    }

    private class DeferredPreferences : RemotePreferences {
        private val flow = MutableStateFlow<RemoteSettings?>(null)
        override val settings = kotlinx.coroutines.flow.flow {
            flow.collect { value -> if (value != null) emit(value) }
        }

        suspend fun emit(settings: RemoteSettings) { flow.emit(settings) }
        override suspend fun save(settings: RemoteSettings) = Unit
        override suspend fun update(transform: (RemoteSettings) -> RemoteSettings?): RemoteSettings? = null
    }
}
