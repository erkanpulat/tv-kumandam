package com.erkanpulat.tvkumandam.presentation.customize

import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import com.erkanpulat.tvkumandam.data.remote.BekoCompatibleProfile
import com.erkanpulat.tvkumandam.data.remote.RemoteProfileCatalog
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.IrSignal
import com.erkanpulat.tvkumandam.domain.model.SavedMacro
import com.erkanpulat.tvkumandam.domain.model.SavedMacroStep
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.model.TransmissionResult
import com.erkanpulat.tvkumandam.domain.preferences.Handedness
import com.erkanpulat.tvkumandam.domain.preferences.RemotePreferences
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.domain.preferences.ThemePreference
import com.erkanpulat.tvkumandam.domain.remote.IrTransmitter
import com.erkanpulat.tvkumandam.domain.remote.RemoteController
import com.erkanpulat.tvkumandam.domain.remote.RemoteTransmissionCoordinator
import com.erkanpulat.tvkumandam.presentation.remote.RemoteViewModel
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
class CustomizeRemoteViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val hdmiMacro = SavedMacro(
        id = "hdmi1",
        name = "HDMI 1",
        steps = listOf(SavedMacroStep(RemoteCommand.SOURCE)),
    )
    private val hdmi = RemoteAction.Macro(hdmiMacro.id)
    private val source = RemoteAction.Command(RemoteCommand.SOURCE)
    private val menu = RemoteAction.Command(RemoteCommand.MENU)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `one TV edit preserves every other setting and exact typed action`() = runTest(dispatcher) {
        val initial = settings(actions = listOf(source, hdmi))
        val preferences = RecordingPreferences(initial)
        val viewModel = createViewModel(preferences)
        runCurrent()

        assertTrue(viewModel.moveToTop(hdmi))
        runCurrent()

        val saved = preferences.writes.single()
        assertEquals(listOf(hdmi, source), saved.savedRemotes.first { it.id == TV_A }.quickActions)
        assertEquals(initial.savedRemotes.first { it.id == TV_B }, saved.savedRemotes.first { it.id == TV_B })
        assertEquals(ThemePreference.DARK, saved.theme)
        assertFalse(saved.hapticsEnabled)
        assertEquals(Handedness.LEFT, saved.handedness)
        assertTrue(saved.onboardingCompleted)
    }

    @Test
    fun `committed editor order becomes the main remote projection`() = runTest(dispatcher) {
        val preferences = RecordingPreferences(settings(actions = listOf(source, hdmi)))
        val catalog = RemoteProfileCatalog()
        val remoteViewModel = RemoteViewModel(
            transmissionCoordinator = RemoteTransmissionCoordinator(RemoteController(NoOpIrTransmitter)),
            catalog = catalog,
            preferences = preferences,
            transmissionDispatcher = dispatcher,
        )
        val customizeViewModel = CustomizeRemoteViewModel(
            targetRemoteId = TV_A,
            preferences = preferences,
            catalog = catalog,
            persistenceDispatcher = dispatcher,
        )
        runCurrent()

        assertTrue(customizeViewModel.moveToTop(hdmi))
        runCurrent()

        assertEquals(listOf(hdmi, source), remoteViewModel.uiState.value.quickActions)
    }

    @Test
    fun `rapid edits keep one active and one latest write and ignore stale echoes`() = runTest(dispatcher) {
        val initial = settings(actions = listOf(hdmi, source, menu))
        val preferences = BlockingFirstSavePreferences(initial)
        val viewModel = createViewModel(preferences)
        runCurrent()

        assertTrue(viewModel.moveRight(hdmi))
        runCurrent()
        preferences.firstSaveStarted.await()
        assertTrue(viewModel.moveRight(hdmi))
        assertTrue(viewModel.moveToTop(menu))
        preferences.emit(initial)
        runCurrent()

        assertEquals(listOf(menu, source, hdmi), viewModel.uiState.value.actions)
        assertEquals(1, preferences.writes.size)

        preferences.releaseFirstSave.complete(Unit)
        runCurrent()

        assertEquals(2, preferences.writes.size)
        assertEquals(
            listOf(menu, source, hdmi),
            preferences.writes.last().savedRemotes.first { it.id == TV_A }.quickActions,
        )
        assertEquals(listOf(menu, source, hdmi), viewModel.uiState.value.actions)
        assertFalse(viewModel.uiState.value.isDirty)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `failed save retains dirty order and retry clears the error`() = runTest(dispatcher) {
        val preferences = FailOncePreferences(settings(actions = listOf(source, hdmi)))
        val viewModel = createViewModel(preferences)
        runCurrent()

        assertTrue(viewModel.moveToTop(hdmi))
        runCurrent()

        assertEquals(listOf(hdmi, source), viewModel.uiState.value.actions)
        assertEquals("Kaydedilemedi", viewModel.uiState.value.saveError)
        assertTrue(viewModel.uiState.value.isDirty)
        assertFalse(viewModel.requestExit())

        viewModel.retry()
        runCurrent()

        assertEquals(2, preferences.writes.size)
        assertNull(viewModel.uiState.value.saveError)
        assertFalse(viewModel.uiState.value.isDirty)
        assertTrue(viewModel.uiState.value.shouldExit)
    }

    @Test
    fun `exit requested while saving happens once after the latest committed write`() = runTest(dispatcher) {
        val preferences = BlockingFirstSavePreferences(settings(actions = listOf(source, hdmi)))
        val viewModel = createViewModel(preferences)
        runCurrent()

        assertTrue(viewModel.moveToTop(hdmi))
        runCurrent()
        preferences.firstSaveStarted.await()

        assertFalse(viewModel.requestExit())
        assertFalse(viewModel.requestExit())
        assertFalse(viewModel.uiState.value.shouldExit)

        preferences.releaseFirstSave.complete(Unit)
        runCurrent()

        assertTrue(viewModel.uiState.value.shouldExit)
        assertEquals(1, preferences.writes.size)
    }

    @Test
    fun `atomic update preserves concurrent rename theme and other TV order`() = runTest(dispatcher) {
        val initial = settings(actions = listOf(source, hdmi))
        val preferences = AtomicGatePreferences(initial)
        val viewModel = createViewModel(preferences)
        runCurrent()

        assertTrue(viewModel.moveToTop(hdmi))
        runCurrent()
        preferences.updateStarted.await()

        val renamedA = SavedRemote(
            id = TV_A,
            name = "Yeni Salon",
            profileId = ArcelikOldLcdProfile.ID,
            quickActions = listOf(source, hdmi),
            isConfirmed = true,
            macros = listOf(hdmiMacro),
        )
        val movedB = initial.savedRemotes.first { it.id == TV_B }
        preferences.emit(
            initial.copy(
                savedRemotes = listOf(movedB, renamedA),
                selectedSavedRemoteId = TV_A,
                theme = ThemePreference.LIGHT,
            ),
        )
        preferences.releaseUpdate.complete(Unit)
        runCurrent()

        val committed = preferences.current
        assertEquals(listOf(TV_B, TV_A), committed.savedRemotes.map(SavedRemote::id))
        assertEquals("Yeni Salon", committed.savedRemotes.first { it.id == TV_A }.name)
        assertEquals(listOf(hdmi, source), committed.savedRemotes.first { it.id == TV_A }.quickActions)
        assertEquals(ThemePreference.LIGHT, committed.theme)
    }

    @Test
    fun `atomic update aborts when selection changed before its flow echo`() = runTest(dispatcher) {
        val initial = settings(actions = listOf(source, hdmi))
        val preferences = AtomicGatePreferences(initial)
        val viewModel = createViewModel(preferences)
        runCurrent()

        assertTrue(viewModel.moveToTop(hdmi))
        runCurrent()
        preferences.updateStarted.await()
        preferences.replaceWithoutEmission(initial.copy(selectedSavedRemoteId = TV_B))
        preferences.releaseUpdate.complete(Unit)
        runCurrent()

        assertTrue(viewModel.uiState.value.shouldExit)
        assertTrue(preferences.writes.isEmpty())
        assertEquals(listOf(source, hdmi), preferences.current.savedRemotes.first { it.id == TV_A }.quickActions)
    }

    @Test
    fun `selected TV removal exits and cancels an active save without touching another TV`() =
        runTest(dispatcher) {
            val initial = settings(actions = listOf(source, hdmi))
            val preferences = BlockingFirstSavePreferences(initial)
            val viewModel = createViewModel(preferences)
            runCurrent()

            assertTrue(viewModel.moveToTop(hdmi))
            runCurrent()
            preferences.firstSaveStarted.await()

            preferences.emit(
                initial.copy(
                    savedRemotes = listOf(initial.savedRemotes.first { it.id == TV_B }),
                    selectedSavedRemoteId = TV_B,
                ),
            )
            runCurrent()

            assertTrue(viewModel.uiState.value.shouldExit)
            assertFalse(viewModel.uiState.value.isSaving)
            assertEquals(listOf(source), preferences.current.savedRemotes.single().quickActions)
            assertTrue(preferences.firstSaveCancelled)
        }

    @Test
    fun `profile change exits and cancels an active save without restoring the old profile`() =
        runTest(dispatcher) {
            val initial = settings(actions = listOf(source, hdmi))
            val preferences = BlockingFirstSavePreferences(initial)
            val viewModel = createViewModel(preferences)
            runCurrent()

            assertTrue(viewModel.moveToTop(hdmi))
            runCurrent()
            preferences.firstSaveStarted.await()

            val changedTarget = SavedRemote(
                id = TV_A,
                name = "Salon TV",
                profileId = BekoCompatibleProfile.ID,
                quickActions = listOf(source),
                isConfirmed = true,
            )
            preferences.emit(
                initial.copy(
                    savedRemotes = initial.savedRemotes.map { remote ->
                        if (remote.id == TV_A) changedTarget else remote
                    },
                ),
            )
            runCurrent()

            assertTrue(viewModel.uiState.value.shouldExit)
            assertTrue(preferences.firstSaveCancelled)
            assertEquals(BekoCompatibleProfile.ID, preferences.current.selectedRemote?.profileId)
            assertEquals(listOf(source), preferences.current.selectedRemote?.quickActions)
        }

    @Test
    fun `explicit empty deck stays valid until reset restores normalized defaults`() = runTest(dispatcher) {
        val preferences = RecordingPreferences(settings(actions = listOf(hdmi)))
        val viewModel = createViewModel(preferences)
        runCurrent()

        assertTrue(viewModel.remove(hdmi))
        runCurrent()
        assertTrue(viewModel.uiState.value.actions.isEmpty())

        assertTrue(viewModel.reset())
        runCurrent()
        assertEquals(ArcelikOldLcdProfile.profile.layout.defaultQuickActions, viewModel.uiState.value.actions)
    }

    @Test
    fun `macro can be saved unpinned and later moved to the first quick slot`() = runTest(dispatcher) {
        val preferences = RecordingPreferences(settings(actions = listOf(source, hdmi)))
        val viewModel = createViewModel(preferences)
        runCurrent()

        assertTrue(viewModel.saveMacro(hdmiMacro, pinToRemote = false))
        runCurrent()
        assertEquals(listOf(source), viewModel.uiState.value.actions)
        assertEquals(listOf(hdmiMacro), viewModel.uiState.value.macros)

        assertTrue(viewModel.saveMacro(hdmiMacro, pinToRemote = true))
        runCurrent()
        assertEquals(listOf(hdmi, source), viewModel.uiState.value.actions)
    }

    private fun createViewModel(preferences: RemotePreferences) = CustomizeRemoteViewModel(
        targetRemoteId = TV_A,
        preferences = preferences,
        catalog = RemoteProfileCatalog(),
        persistenceDispatcher = dispatcher,
    )

    private fun settings(actions: List<RemoteAction>): RemoteSettings = RemoteSettings(
        savedRemotes = listOf(
            SavedRemote(
                TV_A,
                "Salon TV",
                ArcelikOldLcdProfile.ID,
                actions,
                isConfirmed = true,
                macros = listOf(hdmiMacro),
            ),
            SavedRemote(TV_B, "Mutfak TV", ArcelikOldLcdProfile.ID, listOf(source)),
        ),
        selectedSavedRemoteId = TV_A,
        theme = ThemePreference.DARK,
        hapticsEnabled = false,
        handedness = Handedness.LEFT,
        onboardingCompleted = true,
    )

    private open class RecordingPreferences(initial: RemoteSettings) : RemotePreferences {
        protected val mutableSettings = MutableStateFlow(initial)
        override val settings: Flow<RemoteSettings> = mutableSettings
        val writes = mutableListOf<RemoteSettings>()
        open val current: RemoteSettings get() = mutableSettings.value

        override suspend fun save(settings: RemoteSettings) {
            writes += settings
            mutableSettings.value = settings
        }

        override suspend fun update(
            transform: (RemoteSettings) -> RemoteSettings?,
        ): RemoteSettings? {
            val updated = transform(mutableSettings.value) ?: return null
            save(updated)
            return updated
        }

        open fun emit(settings: RemoteSettings) {
            mutableSettings.value = settings
        }
    }

    private class BlockingFirstSavePreferences(initial: RemoteSettings) : RecordingPreferences(initial) {
        val firstSaveStarted = CompletableDeferred<Unit>()
        val releaseFirstSave = CompletableDeferred<Unit>()
        var firstSaveCancelled = false
            private set

        override suspend fun save(settings: RemoteSettings) {
            writes += settings
            if (writes.size == 1) {
                firstSaveStarted.complete(Unit)
                try {
                    releaseFirstSave.await()
                } finally {
                    firstSaveCancelled = !releaseFirstSave.isCompleted
                }
            }
            mutableSettings.value = settings
        }
    }

    private class FailOncePreferences(initial: RemoteSettings) : RecordingPreferences(initial) {
        override suspend fun save(settings: RemoteSettings) {
            writes += settings
            if (writes.size == 1) error("disk full")
            mutableSettings.value = settings
        }
    }

    private class AtomicGatePreferences(initial: RemoteSettings) : RecordingPreferences(initial) {
        val updateStarted = CompletableDeferred<Unit>()
        val releaseUpdate = CompletableDeferred<Unit>()
        private var transactionalCurrent = initial
        override val current: RemoteSettings get() = transactionalCurrent

        override fun emit(settings: RemoteSettings) {
            transactionalCurrent = settings
            super.emit(settings)
        }

        override suspend fun update(
            transform: (RemoteSettings) -> RemoteSettings?,
        ): RemoteSettings? {
            updateStarted.complete(Unit)
            releaseUpdate.await()
            val committed = transform(transactionalCurrent) ?: return null
            writes += committed
            transactionalCurrent = committed
            mutableSettings.value = committed
            return committed
        }

        fun replaceWithoutEmission(settings: RemoteSettings) {
            transactionalCurrent = settings
        }
    }

    private companion object {
        const val TV_A = "tv-a"
        const val TV_B = "tv-b"
    }

    private object NoOpIrTransmitter : IrTransmitter {
        override val isAvailable: Boolean = true

        override fun transmit(signal: IrSignal): TransmissionResult = TransmissionResult.Success
    }
}
