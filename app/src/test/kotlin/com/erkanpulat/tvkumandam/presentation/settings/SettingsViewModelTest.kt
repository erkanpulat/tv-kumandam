package com.erkanpulat.tvkumandam.presentation.settings

import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.preferences.Handedness
import com.erkanpulat.tvkumandam.domain.preferences.RemotePreferences
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.domain.preferences.ThemePreference
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `theme update is atomic and preserves every unrelated setting`() = runTest(dispatcher) {
        val original = richSettings()
        val preferences = FakePreferences(original)
        val viewModel = SettingsViewModel(preferences, isIrAvailable = true, workDispatcher = dispatcher)
        runCurrent()

        viewModel.selectTheme(ThemePreference.DARK)
        runCurrent()

        val saved = preferences.current.value
        assertEquals(ThemePreference.DARK, saved.theme)
        assertEquals(original.savedRemotes, saved.savedRemotes)
        assertEquals(original.selectedSavedRemoteId, saved.selectedSavedRemoteId)
        assertEquals(original.hapticsEnabled, saved.hapticsEnabled)
        assertEquals(original.handedness, saved.handedness)
        assertEquals(original.onboardingCompleted, saved.onboardingCompleted)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `concurrent taps cannot overwrite a newer settings snapshot`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val preferences = FakePreferences(richSettings(), gate)
        val viewModel = SettingsViewModel(preferences, isIrAvailable = true, workDispatcher = dispatcher)
        runCurrent()

        viewModel.selectTheme(ThemePreference.DARK)
        runCurrent()
        preferences.replace(preferences.current.value.copy(hapticsEnabled = false))
        viewModel.selectHandedness(Handedness.LEFT)
        gate.complete(Unit)
        runCurrent()

        assertEquals(ThemePreference.DARK, preferences.current.value.theme)
        assertFalse(preferences.current.value.hapticsEnabled)
        assertEquals(Handedness.RIGHT, preferences.current.value.handedness)
        assertEquals(1, preferences.updateCount)
    }

    @Test
    fun `failed mutation exposes retry and retry applies it to latest settings`() = runTest(dispatcher) {
        val preferences = FakePreferences(richSettings()).apply { failNext = true }
        val viewModel = SettingsViewModel(preferences, isIrAvailable = false, workDispatcher = dispatcher)
        runCurrent()

        viewModel.selectHandedness(Handedness.LEFT)
        runCurrent()

        assertTrue(viewModel.uiState.value.error?.startsWith("Ayar kaydedilemedi") == true)
        assertFalse(viewModel.uiState.value.isSaving)
        preferences.replace(preferences.current.value.copy(hapticsEnabled = false))

        viewModel.retry()
        runCurrent()

        assertEquals(Handedness.LEFT, preferences.current.value.handedness)
        assertFalse(preferences.current.value.hapticsEnabled)
        assertEquals(null, viewModel.uiState.value.error)
        assertEquals(2, preferences.updateCount)
    }

    private fun richSettings(): RemoteSettings {
        val remote = SavedRemote("salon", "Salon TV", ArcelikOldLcdProfile.ID, isConfirmed = true)
        return RemoteSettings(
            savedRemotes = listOf(remote),
            selectedSavedRemoteId = remote.id,
            theme = ThemePreference.LIGHT,
            hapticsEnabled = true,
            handedness = Handedness.RIGHT,
            onboardingCompleted = true,
        )
    }

    private class FakePreferences(
        initial: RemoteSettings,
        private val gate: CompletableDeferred<Unit>? = null,
    ) : RemotePreferences {
        val current = MutableStateFlow(initial)
        override val settings: Flow<RemoteSettings> = current
        var failNext = false
        var updateCount = 0

        override suspend fun save(settings: RemoteSettings) {
            current.value = settings
        }

        override suspend fun update(transform: (RemoteSettings) -> RemoteSettings?): RemoteSettings? {
            updateCount += 1
            gate?.await()
            if (failNext) {
                failNext = false
                throw IOException("disk")
            }
            val result = transform(current.value) ?: return null
            current.value = result
            return result
        }

        fun replace(settings: RemoteSettings) {
            current.value = settings
        }
    }
}
