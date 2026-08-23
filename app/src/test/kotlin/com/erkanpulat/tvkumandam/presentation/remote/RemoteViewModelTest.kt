package com.erkanpulat.tvkumandam.presentation.remote

import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import com.erkanpulat.tvkumandam.data.remote.BekoCompatibleProfile
import com.erkanpulat.tvkumandam.data.remote.RemoteProfileCatalog
import com.erkanpulat.tvkumandam.domain.model.IrSignal
import com.erkanpulat.tvkumandam.domain.model.CommandBinding
import com.erkanpulat.tvkumandam.domain.model.EvidenceTier
import com.erkanpulat.tvkumandam.domain.model.InputCapability
import com.erkanpulat.tvkumandam.domain.model.IrCommand
import com.erkanpulat.tvkumandam.domain.model.ProfileEvidence
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutSpec
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutTemplate
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.RemoteShortcut
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.model.SavedMacro
import com.erkanpulat.tvkumandam.domain.model.SavedMacroStep
import com.erkanpulat.tvkumandam.domain.model.TransmissionResult
import com.erkanpulat.tvkumandam.domain.preferences.RemotePreferences
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.domain.remote.IrTransmitter
import com.erkanpulat.tvkumandam.domain.remote.RemoteController
import com.erkanpulat.tvkumandam.domain.remote.RemoteTransmissionCoordinator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `stored profile and quick actions are restored`() = runTest(dispatcher) {
        val preferences = FakeRemotePreferences(
            savedSettings(BekoCompatibleProfile.ID, listOf(RemoteCommand.SOURCE)),
        )
        val viewModel = createViewModel(preferences)

        runCurrent()

        assertEquals(BekoCompatibleProfile.ID, viewModel.uiState.value.selectedProfileId)
        assertEquals(
            listOf(RemoteAction.Command(RemoteCommand.SOURCE)),
            viewModel.uiState.value.quickActions,
        )
        assertTrue(viewModel.uiState.value.isIrAvailable)
    }

    @Test
    fun `rapid Power taps send once and never replay later`() = runTest(dispatcher) {
        val transmitter = RecordingIrTransmitter()
        val viewModel = createViewModel(
            preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
            transmitter = transmitter,
        )
        runCurrent()
        val event = async { viewModel.events.first() }

        viewModel.sendCommand(RemoteCommand.POWER)
        viewModel.sendCommand(RemoteCommand.POWER)

        assertEquals(0, transmitter.transmissionCount)
        runCurrent()

        assertEquals(1, transmitter.transmissionCount)
        assertEquals(RemoteUiEvent.CommandSent(RemoteCommand.POWER), event.await())
        advanceTimeBy(10_000L)
        runCurrent()
        assertEquals(1, transmitter.transmissionCount)
    }

    @Test
    fun `saved user macro runs as one exclusive transmission and reports its name`() = runTest(dispatcher) {
        val macro = SavedMacro(
            "movie",
            "Film modu",
            listOf(
                SavedMacroStep(RemoteCommand.SOURCE, delayAfterMillis = 0),
                SavedMacroStep(RemoteCommand.DOWN, repeatCount = 2, delayAfterMillis = 0),
                SavedMacroStep(RemoteCommand.OK, delayAfterMillis = 0),
            ),
        )
        val remote = SavedRemote(
            "salon",
            "Salon",
            ArcelikOldLcdProfile.ID,
            quickActions = listOf(RemoteAction.Macro(macro.id)),
            macros = listOf(macro),
        )
        val transmitter = RecordingIrTransmitter()
        val viewModel = createViewModel(
            FakeRemotePreferences(RemoteSettings(listOf(remote), remote.id)),
            transmitter,
        )
        runCurrent()
        val event = async { viewModel.events.first() }

        assertTrue(viewModel.sendMacro(macro.id))
        assertFalse(viewModel.sendCommand(RemoteCommand.VOLUME_UP))
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(8, transmitter.transmissionCount)
        assertEquals(RemoteUiEvent.MacroSent("Film modu"), event.await())
        assertEquals(TransmissionState.Idle, viewModel.uiState.value.transmissionState)
    }

    @Test
    fun `HDMI shortcut locks controls and reports success after the complete sequence`() =
        runTest(dispatcher) {
            val transmitter = RecordingIrTransmitter()
            val viewModel = createViewModel(
                preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
                transmitter = transmitter,
            )
            runCurrent()
            val event = async { viewModel.events.first() }

            viewModel.sendShortcut(RemoteShortcut.HDMI1)

            assertTrue(viewModel.uiState.value.isTransmitting)
            assertEquals(RemoteShortcut.HDMI1, viewModel.uiState.value.activeShortcut)
            assertEquals(
                TransmissionState.Shortcut(
                    savedRemoteId = "test-${ArcelikOldLcdProfile.ID}",
                    profileId = ArcelikOldLcdProfile.ID,
                    shortcut = RemoteShortcut.HDMI1,
                    completedSteps = 0,
                    totalSteps = 9,
                ),
                viewModel.uiState.value.transmissionState,
            )
            runCurrent()
            assertEquals(1, transmitter.transmissionCount)

            advanceTimeBy(89L)
            runCurrent()
            assertEquals(1, viewModel.uiState.value.shortcutProgress?.completedSteps)

            advanceTimeBy(4_012L)
            runCurrent()

            assertEquals(18, transmitter.transmissionCount)
            assertFalse(viewModel.uiState.value.isTransmitting)
            assertEquals(null, viewModel.uiState.value.activeShortcut)
            assertEquals(RemoteUiEvent.ShortcutSent(RemoteShortcut.HDMI1), event.await())
        }

    @Test
    fun `selecting another saved remote with the same profile cancels macro ownership`() =
        runTest(dispatcher) {
            val salon = SavedRemote(
                id = "salon-tv",
                name = "Salon TV",
                profileId = ArcelikOldLcdProfile.ID,
            )
            val yatak = SavedRemote(
                id = "yatak-tv",
                name = "Yatak TV",
                profileId = ArcelikOldLcdProfile.ID,
            )
            val salonSettings = RemoteSettings(
                savedRemotes = listOf(salon, yatak),
                selectedSavedRemoteId = salon.id,
            )
            val preferences = FakeRemotePreferences(salonSettings)
            val transmitter = RecordingIrTransmitter()
            val viewModel = createViewModel(preferences, transmitter)
            val events = mutableListOf<RemoteUiEvent>()
            backgroundScope.launch { viewModel.events.toList(events) }
            runCurrent()

            assertTrue(viewModel.sendShortcut(RemoteShortcut.HDMI1))
            runCurrent()
            val macroFramesBeforeSwitch = transmitter.transmissionCount
            assertTrue(macroFramesBeforeSwitch > 0)

            preferences.emit(salonSettings.copy(selectedSavedRemoteId = yatak.id))
            runCurrent()
            advanceTimeBy(10_000L)
            runCurrent()

            assertEquals(yatak.id, viewModel.uiState.value.selectedRemote?.id)
            assertEquals(macroFramesBeforeSwitch, transmitter.transmissionCount)
            assertEquals(TransmissionState.Idle, viewModel.uiState.value.transmissionState)
            assertFalse(events.any { it is RemoteUiEvent.ShortcutSent })

            assertTrue(viewModel.sendCommand(RemoteCommand.VOLUME_UP))
            runCurrent()
            assertEquals(macroFramesBeforeSwitch + 1, transmitter.transmissionCount)
            assertEquals(
                listOf(RemoteUiEvent.CommandSent(RemoteCommand.VOLUME_UP)),
                events.filterIsInstance<RemoteUiEvent.CommandSent>(),
            )
        }

    @Test
    fun `no selected saved remote cannot admit a transmission`() = runTest(dispatcher) {
        val transmitter = RecordingIrTransmitter()
        val viewModel = createViewModel(
            preferences = FakeRemotePreferences(RemoteSettings()),
            transmitter = transmitter,
        )
        runCurrent()

        assertFalse(viewModel.sendCommand(RemoteCommand.POWER))
        assertFalse(viewModel.sendShortcut(RemoteShortcut.HDMI1))
        runCurrent()

        assertEquals(0, transmitter.transmissionCount)
        assertEquals(TransmissionState.Idle, viewModel.uiState.value.transmissionState)
    }

    @Test
    fun `RC-YC1 profiles expose only the verified HDMI1 shortcut`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)))
        runCurrent()

        assertEquals(setOf(RemoteShortcut.HDMI1), RemoteShortcut.entries.toSet())
        assertEquals(
            setOf(RemoteShortcut.HDMI1),
            ArcelikOldLcdProfile.profile.supportedShortcuts,
        )
        assertEquals(
            setOf(RemoteShortcut.HDMI1),
            viewModel.uiState.value.profiles
                .single { it.id == ArcelikOldLcdProfile.ID }
                .supportedShortcuts,
        )
    }

    @Test
    fun `commands submitted during an HDMI shortcut are ignored`() = runTest(dispatcher) {
        val transmitter = RecordingIrTransmitter()
        val viewModel = createViewModel(
            preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
            transmitter = transmitter,
        )
        runCurrent()

        viewModel.sendShortcut(RemoteShortcut.HDMI1)
        viewModel.sendCommand(RemoteCommand.SOURCE)
        viewModel.sendCommand(RemoteCommand.DOWN)
        viewModel.sendShortcut(RemoteShortcut.HDMI1)
        advanceTimeBy(4_101L)
        runCurrent()

        assertEquals(18, transmitter.transmissionCount)
    }

    @Test
    fun `cancelling HDMI during a delay stops remaining steps clears progress and emits no success`() =
        runTest(dispatcher) {
            val transmitter = RecordingIrTransmitter()
            val viewModel = createViewModel(
                preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
                transmitter = transmitter,
            )
            runCurrent()
            val events = mutableListOf<RemoteUiEvent>()
            val collector = backgroundScope.launch { viewModel.events.toList(events) }

            viewModel.sendShortcut(RemoteShortcut.HDMI1)
            runCurrent()
            assertEquals(1, transmitter.transmissionCount)

            viewModel.cancelTransmission()
            runCurrent()
            advanceTimeBy(10_000L)
            runCurrent()

            assertEquals(1, transmitter.transmissionCount)
            assertEquals(TransmissionState.Idle, viewModel.uiState.value.transmissionState)
            assertEquals(null, viewModel.uiState.value.shortcutProgress)
            assertFalse(events.any { it is RemoteUiEvent.ShortcutSent })
            collector.cancel()
        }

    @Test
    fun `navigation closes admission before waiting for the old owner to cancel`() =
        runTest(dispatcher) {
            val enteredPublication = CompletableDeferred<Unit>()
            val releasePublication = CompletableDeferred<Unit>()
            val transmitter = RecordingIrTransmitter()
            val viewModel = createViewModel(
                preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
                transmitter = transmitter,
                publicationGate = TransmissionPublicationGate { publication ->
                    if (
                        publication is TransmissionPublication.CommandResult &&
                        publication.command == RemoteCommand.POWER
                    ) {
                        enteredPublication.complete(Unit)
                        withContext(NonCancellable) { releasePublication.await() }
                    }
                },
            )
            runCurrent()

            assertTrue(viewModel.sendCommand(RemoteCommand.POWER))
            runCurrent()
            enteredPublication.await()

            val navigation = async { viewModel.suspendTransmissionAdmissionAndCancel() }
            runCurrent()

            assertFalse(navigation.isCompleted)
            assertEquals(TransmissionState.Idle, viewModel.uiState.value.transmissionState)
            assertFalse(viewModel.sendCommand(RemoteCommand.VOLUME_UP))
            assertEquals(1, transmitter.transmissionCount)

            releasePublication.complete(Unit)
            navigation.await()
            viewModel.resumeTransmissionAdmission()
            assertTrue(viewModel.sendCommand(RemoteCommand.VOLUME_UP))
            runCurrent()
            assertEquals(2, transmitter.transmissionCount)
        }

    @Test
    fun `cancel at command publication boundary suppresses stale success and preserves next owner`() =
        runTest(dispatcher) {
            val enteredPublication = CompletableDeferred<Unit>()
            val enteredReplacementPublication = CompletableDeferred<Unit>()
            val releaseFirstPublication = CompletableDeferred<Unit>()
            val releaseReplacementPublication = CompletableDeferred<Unit>()
            val events = mutableListOf<RemoteUiEvent>()
            val transmitter = RecordingIrTransmitter()
            val viewModel = createViewModel(
                preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
                transmitter = transmitter,
                publicationGate = TransmissionPublicationGate { publication ->
                    if (
                        publication is TransmissionPublication.CommandResult &&
                        publication.profileId == ArcelikOldLcdProfile.ID &&
                        publication.command == RemoteCommand.POWER
                    ) {
                        enteredPublication.complete(Unit)
                        withContext(NonCancellable) { releaseFirstPublication.await() }
                    } else if (
                        publication is TransmissionPublication.CommandResult &&
                        publication.profileId == ArcelikOldLcdProfile.ID &&
                        publication.command == RemoteCommand.VOLUME_UP
                    ) {
                        enteredReplacementPublication.complete(Unit)
                        withContext(NonCancellable) { releaseReplacementPublication.await() }
                    }
                },
            )
            runCurrent()
            val collector = backgroundScope.launch { viewModel.events.toList(events) }
            runCurrent()

            viewModel.sendCommand(RemoteCommand.POWER)
            runCurrent()
            enteredPublication.await()

            viewModel.cancelTransmission()
            viewModel.sendCommand(RemoteCommand.VOLUME_UP)
            runCurrent()

            try {
                assertEquals(2, transmitter.transmissionCount)
                enteredReplacementPublication.await()
                assertEquals(
                    TransmissionState.Command("test-${ArcelikOldLcdProfile.ID}", ArcelikOldLcdProfile.ID, RemoteCommand.VOLUME_UP),
                    viewModel.uiState.value.transmissionState,
                )

                releaseFirstPublication.complete(Unit)
                runCurrent()

                assertFalse(events.any { it == RemoteUiEvent.CommandSent(RemoteCommand.POWER) })
                assertEquals(
                    TransmissionState.Command("test-${ArcelikOldLcdProfile.ID}", ArcelikOldLcdProfile.ID, RemoteCommand.VOLUME_UP),
                    viewModel.uiState.value.transmissionState,
                )
            } finally {
                releaseFirstPublication.complete(Unit)
                releaseReplacementPublication.complete(Unit)
            }
            runCurrent()

            assertEquals(
                listOf(RemoteUiEvent.CommandSent(RemoteCommand.VOLUME_UP)),
                events.filterIsInstance<RemoteUiEvent.CommandSent>(),
            )
            assertEquals(TransmissionState.Idle, viewModel.uiState.value.transmissionState)
            collector.cancel()
        }

    @Test
    fun `cancel at progress publication boundary suppresses stale shortcut progress and success`() =
        runTest(dispatcher) {
            val enteredPublication = CompletableDeferred<Unit>()
            val enteredReplacementPublication = CompletableDeferred<Unit>()
            val releaseProgressPublication = CompletableDeferred<Unit>()
            val releaseReplacementPublication = CompletableDeferred<Unit>()
            val events = mutableListOf<RemoteUiEvent>()
            val transmitter = RecordingIrTransmitter()
            val viewModel = createViewModel(
                preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
                transmitter = transmitter,
                publicationGate = TransmissionPublicationGate { publication ->
                    if (
                        publication is TransmissionPublication.ShortcutProgress &&
                        publication.progress.completedSteps == 1
                    ) {
                        enteredPublication.complete(Unit)
                        withContext(NonCancellable) { releaseProgressPublication.await() }
                    } else if (
                        publication is TransmissionPublication.CommandResult &&
                        publication.command == RemoteCommand.VOLUME_UP
                    ) {
                        enteredReplacementPublication.complete(Unit)
                        withContext(NonCancellable) { releaseReplacementPublication.await() }
                    }
                },
            )
            runCurrent()
            val collector = backgroundScope.launch { viewModel.events.toList(events) }
            runCurrent()

            viewModel.sendShortcut(RemoteShortcut.HDMI1)
            runCurrent()
            advanceTimeBy(89L)
            runCurrent()
            enteredPublication.await()

            viewModel.cancelTransmission()
            viewModel.sendCommand(RemoteCommand.VOLUME_UP)
            runCurrent()

            try {
                // The old sequence keeps the app-scoped emitter lease until its
                // cancellation reaches the suspended progress callback.
                assertEquals(2, transmitter.transmissionCount)
                assertFalse(enteredReplacementPublication.isCompleted)

                releaseProgressPublication.complete(Unit)
                runCurrent()

                assertEquals(3, transmitter.transmissionCount)
                enteredReplacementPublication.await()
                assertEquals(
                    TransmissionState.Command("test-${ArcelikOldLcdProfile.ID}", ArcelikOldLcdProfile.ID, RemoteCommand.VOLUME_UP),
                    viewModel.uiState.value.transmissionState,
                )

                assertFalse(events.any { it is RemoteUiEvent.ShortcutSent })
                assertEquals(
                    TransmissionState.Command("test-${ArcelikOldLcdProfile.ID}", ArcelikOldLcdProfile.ID, RemoteCommand.VOLUME_UP),
                    viewModel.uiState.value.transmissionState,
                )
            } finally {
                releaseProgressPublication.complete(Unit)
                releaseReplacementPublication.complete(Unit)
            }
            runCurrent()

            assertEquals(TransmissionState.Idle, viewModel.uiState.value.transmissionState)
            assertEquals(
                listOf(RemoteUiEvent.CommandSent(RemoteCommand.VOLUME_UP)),
                events.filterIsInstance<RemoteUiEvent.CommandSent>(),
            )
            assertFalse(events.any { it is RemoteUiEvent.ShortcutSent })
            collector.cancel()
        }

    @Test
    fun `cancel at shortcut result boundary cannot publish into a replacement owner`() =
        runTest(dispatcher) {
            val enteredShortcutResult = CompletableDeferred<Unit>()
            val enteredReplacementResult = CompletableDeferred<Unit>()
            val releaseShortcutResult = CompletableDeferred<Unit>()
            val releaseReplacementResult = CompletableDeferred<Unit>()
            val events = mutableListOf<RemoteUiEvent>()
            val transmitter = RecordingIrTransmitter()
            val viewModel = createViewModel(
                preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
                transmitter = transmitter,
                publicationGate = TransmissionPublicationGate { publication ->
                    if (publication is TransmissionPublication.ShortcutResult) {
                        enteredShortcutResult.complete(Unit)
                        withContext(NonCancellable) { releaseShortcutResult.await() }
                    } else if (
                        publication is TransmissionPublication.CommandResult &&
                        publication.command == RemoteCommand.VOLUME_UP
                    ) {
                        enteredReplacementResult.complete(Unit)
                        withContext(NonCancellable) { releaseReplacementResult.await() }
                    }
                },
            )
            runCurrent()
            val collector = backgroundScope.launch { viewModel.events.toList(events) }
            runCurrent()

            viewModel.sendShortcut(RemoteShortcut.HDMI1)
            runCurrent()
            advanceTimeBy(4_101L)
            runCurrent()
            enteredShortcutResult.await()

            viewModel.cancelTransmission()
            viewModel.sendCommand(RemoteCommand.VOLUME_UP)
            runCurrent()

            try {
                assertEquals(19, transmitter.transmissionCount)
                enteredReplacementResult.await()
                assertEquals(
                    TransmissionState.Command("test-${ArcelikOldLcdProfile.ID}", ArcelikOldLcdProfile.ID, RemoteCommand.VOLUME_UP),
                    viewModel.uiState.value.transmissionState,
                )

                releaseShortcutResult.complete(Unit)
                runCurrent()

                assertFalse(events.any { it is RemoteUiEvent.ShortcutSent })
                assertEquals(
                    TransmissionState.Command("test-${ArcelikOldLcdProfile.ID}", ArcelikOldLcdProfile.ID, RemoteCommand.VOLUME_UP),
                    viewModel.uiState.value.transmissionState,
                )
            } finally {
                releaseShortcutResult.complete(Unit)
                releaseReplacementResult.complete(Unit)
            }
            runCurrent()

            assertEquals(TransmissionState.Idle, viewModel.uiState.value.transmissionState)
            assertEquals(
                listOf(RemoteUiEvent.CommandSent(RemoteCommand.VOLUME_UP)),
                events.filterIsInstance<RemoteUiEvent.CommandSent>(),
            )
            assertFalse(events.any { it is RemoteUiEvent.ShortcutSent })
            collector.cancel()
        }

    @Test
    fun `rapid identical volume repeats coalesce once without crossing command boundaries`() =
        runTest(dispatcher) {
            val transmitter = RecordingIrTransmitter()
            val viewModel = createViewModel(
                preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
                transmitter = transmitter,
            )
            runCurrent()

            viewModel.sendCommand(RemoteCommand.VOLUME_UP)
            viewModel.sendCommand(RemoteCommand.VOLUME_UP)
            viewModel.sendCommand(RemoteCommand.VOLUME_UP)
            viewModel.sendCommand(RemoteCommand.VOLUME_DOWN)
            viewModel.sendCommand(RemoteCommand.CHANNEL_UP)
            runCurrent()

            assertEquals(2, transmitter.transmissionCount)
            assertEquals(TransmissionState.Idle, viewModel.uiState.value.transmissionState)
        }

    @Test
    fun `external device selection cancels captured old profile work before it can continue`() =
        runTest(dispatcher) {
            val transmitter = RecordingIrTransmitter()
            val preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID))
            val viewModel = createViewModel(
                preferences = preferences,
                transmitter = transmitter,
            )
            runCurrent()

            viewModel.sendShortcut(RemoteShortcut.HDMI1)
            runCurrent()
            assertEquals(1, transmitter.transmissionCount)

            preferences.emit(savedSettings(BekoCompatibleProfile.ID))
            runCurrent()
            advanceTimeBy(10_000L)
            runCurrent()

            assertEquals(1, transmitter.transmissionCount)
            assertEquals(BekoCompatibleProfile.ID, viewModel.uiState.value.selectedProfileId)
            assertEquals(TransmissionState.Idle, viewModel.uiState.value.transmissionState)
        }

    @Test
    fun `clearing the ViewModel cancels captured shortcut work`() = runTest(dispatcher) {
        val transmitter = RecordingIrTransmitter()
        val store = ViewModelStore()
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                createViewModel(
                    preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
                    transmitter = transmitter,
                ) as T
        }
        val viewModel = ViewModelProvider(owner, factory)[RemoteViewModel::class.java]
        runCurrent()

        viewModel.sendShortcut(RemoteShortcut.HDMI1)
        runCurrent()
        assertEquals(1, transmitter.transmissionCount)

        store.clear()
        runCurrent()
        advanceTimeBy(10_000L)
        runCurrent()

        assertEquals(1, transmitter.transmissionCount)
    }

    @Test
    fun `unsupported command and missing IR never enter busy state`() = runTest(dispatcher) {
        val availableViewModel = createViewModel(
            preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
        )
        runCurrent()

        availableViewModel.sendCommand(RemoteCommand.MUTE)
        assertEquals(TransmissionState.Idle, availableViewModel.uiState.value.transmissionState)

        val unavailableTransmitter = RecordingIrTransmitter(isAvailable = false)
        val viewModel = createViewModel(
            preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
            transmitter = unavailableTransmitter,
        )
        runCurrent()

        viewModel.sendCommand(RemoteCommand.POWER)
        assertEquals(TransmissionState.Idle, viewModel.uiState.value.transmissionState)
        runCurrent()

        assertEquals(0, unavailableTransmitter.transmissionCount)
    }

    @Test
    fun `transmission intents report admission for haptic feedback without accepting rejected taps`() =
        runTest(dispatcher) {
            val viewModel = createViewModel(
                preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
            )
            runCurrent()

            assertFalse(viewModel.sendCommand(RemoteCommand.MUTE))
            assertTrue(viewModel.sendCommand(RemoteCommand.POWER))
            assertFalse(viewModel.sendCommand(RemoteCommand.POWER))
            assertFalse(viewModel.sendShortcut(RemoteShortcut.HDMI1))
        }

    @Test
    fun `encoding and platform failures clear busy state without replay`() = runTest(dispatcher) {
        val encodingProfile = profileWithPower(IrCommand { error("bad code") })
        val encodingTransmitter = RecordingIrTransmitter()
        val encodingViewModel = createViewModel(
            preferences = FakeRemotePreferences(savedSettings(encodingProfile.id)),
            transmitter = encodingTransmitter,
            catalog = RemoteProfileCatalog(listOf(encodingProfile)),
        )
        runCurrent()

        encodingViewModel.sendCommand(RemoteCommand.POWER)
        encodingViewModel.sendCommand(RemoteCommand.POWER)
        runCurrent()

        assertEquals(0, encodingTransmitter.transmissionCount)
        assertEquals(TransmissionState.Idle, encodingViewModel.uiState.value.transmissionState)

        val platformTransmitter = RecordingIrTransmitter(
            result = TransmissionResult.PlatformFailure("emitter rejected signal"),
        )
        val platformViewModel = createViewModel(
            preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
            transmitter = platformTransmitter,
        )
        runCurrent()

        platformViewModel.sendCommand(RemoteCommand.VOLUME_UP)
        platformViewModel.sendCommand(RemoteCommand.VOLUME_UP)
        runCurrent()
        advanceTimeBy(10_000L)
        runCurrent()

        assertEquals(1, platformTransmitter.transmissionCount)
        assertEquals(TransmissionState.Idle, platformViewModel.uiState.value.transmissionState)
    }

    @Test
    fun `shortcut platform failure clears progress and never replays competing input`() =
        runTest(dispatcher) {
            val transmitter = RecordingIrTransmitter(
                result = TransmissionResult.PlatformFailure("emitter rejected signal"),
            )
            val viewModel = createViewModel(
                preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID)),
                transmitter = transmitter,
            )
            runCurrent()

            viewModel.sendShortcut(RemoteShortcut.HDMI1)
            viewModel.sendCommand(RemoteCommand.SOURCE)
            runCurrent()
            advanceTimeBy(10_000L)
            runCurrent()

            assertEquals(1, transmitter.transmissionCount)
            assertEquals(TransmissionState.Idle, viewModel.uiState.value.transmissionState)
            assertEquals(null, viewModel.uiState.value.shortcutProgress)
        }

    @Test
    fun `pending volume repeat is discarded when profile changes`() = runTest(dispatcher) {
        val transmitter = RecordingIrTransmitter()
        val preferences = FakeRemotePreferences(savedSettings(ArcelikOldLcdProfile.ID))
        val firstPublicationReached = CompletableDeferred<Unit>()
        val releaseFirstPublication = CompletableDeferred<Unit>()
        val viewModel = createViewModel(
            preferences = preferences,
            transmitter = transmitter,
            publicationGate = TransmissionPublicationGate { publication ->
                if (publication is TransmissionPublication.CommandResult) {
                    firstPublicationReached.complete(Unit)
                    releaseFirstPublication.await()
                }
            },
        )
        runCurrent()

        viewModel.sendCommand(RemoteCommand.VOLUME_UP)
        runCurrent()
        firstPublicationReached.await()
        viewModel.sendCommand(RemoteCommand.VOLUME_UP)
        preferences.emit(savedSettings(BekoCompatibleProfile.ID))
        runCurrent()

        // The first press reached the platform, but its result was not yet
        // published. Switching the device revokes that owner and discards the
        // queued coalesced repeat.
        assertEquals(1, transmitter.transmissionCount)
        assertEquals(BekoCompatibleProfile.ID, viewModel.uiState.value.selectedProfileId)

        releaseFirstPublication.complete(Unit)
        viewModel.sendCommand(RemoteCommand.VOLUME_UP)
        runCurrent()
        assertEquals(2, transmitter.transmissionCount)
    }

    private fun createViewModel(
        preferences: RemotePreferences,
        transmitter: IrTransmitter = RecordingIrTransmitter(),
        catalog: RemoteProfileCatalog = RemoteProfileCatalog(),
        publicationGate: TransmissionPublicationGate = TransmissionPublicationGate.None,
    ): RemoteViewModel =
        RemoteViewModel(
            transmissionCoordinator = RemoteTransmissionCoordinator(RemoteController(transmitter)),
            catalog = catalog,
            preferences = preferences,
            transmissionDispatcher = dispatcher,
            publicationGate = publicationGate,
        )

    private fun savedSettings(
        profileId: String,
        commands: List<RemoteCommand> = emptyList(),
    ): RemoteSettings = RemoteSettings(
        savedRemotes = listOf(
            SavedRemote(
                id = "test-$profileId",
                name = "Test TV",
                profileId = profileId,
                quickActions = commands.map(RemoteAction::Command),
            ),
        ),
        selectedSavedRemoteId = "test-$profileId",
    )

    private class FakeRemotePreferences(
        initial: RemoteSettings,
    ) : RemotePreferences {
        private val mutableSettings = MutableStateFlow(initial)
        override val settings: Flow<RemoteSettings> = mutableSettings

        override suspend fun save(settings: RemoteSettings) {
            mutableSettings.value = settings
        }

        override suspend fun update(
            transform: (RemoteSettings) -> RemoteSettings?,
        ): RemoteSettings? {
            val updated = transform(mutableSettings.value) ?: return null
            save(updated)
            return updated
        }

        fun emit(settings: RemoteSettings) {
            mutableSettings.value = settings
        }
    }

    private class RecordingIrTransmitter(
        override val isAvailable: Boolean = true,
        private val result: TransmissionResult = TransmissionResult.Success,
    ) : IrTransmitter {
        var transmissionCount = 0
            private set

        override fun transmit(signal: IrSignal): TransmissionResult {
            transmissionCount += 1
            return result
        }
    }

    private fun profileWithPower(powerCommand: IrCommand): RemoteProfile {
        val evidence = ProfileEvidence(
            tier = EvidenceTier.EXPERIMENTAL,
            sourceReference = "RemoteViewModel test fixture",
        )
        return RemoteProfile(
            id = "encoding-test",
            brand = "Test",
            displayName = "Encoding test",
            modelAliases = listOf("Test TV"),
            remoteModel = null,
            defaultEvidence = evidence,
            commands = linkedMapOf(
                RemoteCommand.SOURCE to CommandBinding(
                    IrCommand { IrSignal(36_000, intArrayOf(100, 100)) },
                    evidence,
                ),
                RemoteCommand.POWER to CommandBinding(powerCommand, evidence),
            ),
            layout = RemoteLayoutSpec.defaultFor(RemoteLayoutTemplate.CLASSIC_DPAD),
            inputCapability = InputCapability.sourceOnly(),
        )
    }
}
