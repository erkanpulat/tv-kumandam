package com.erkanpulat.tvkumandam.presentation.devices

import com.erkanpulat.tvkumandam.data.remote.ArcelikOldLcdProfile
import com.erkanpulat.tvkumandam.data.remote.BekoCompatibleProfile
import com.erkanpulat.tvkumandam.data.remote.RemoteProfileCatalog
import com.erkanpulat.tvkumandam.domain.model.IrSignal
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteSequence
import com.erkanpulat.tvkumandam.domain.model.RemoteSequenceStep
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.model.TransmissionResult
import com.erkanpulat.tvkumandam.domain.preferences.Handedness
import com.erkanpulat.tvkumandam.domain.preferences.RemotePreferences
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.domain.preferences.ThemePreference
import com.erkanpulat.tvkumandam.domain.remote.IrTransmitter
import com.erkanpulat.tvkumandam.domain.remote.RemoteController
import com.erkanpulat.tvkumandam.domain.remote.RemoteTransmissionCoordinator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DevicesViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `one explicit tap emits one Power signal and awaits truthful human confirmation`() =
        runTest(dispatcher) {
            val transmitter = RecordingTransmitter()
            val viewModel = viewModel(transmitter = transmitter)
            runCurrent()
            openArcelikCandidate(viewModel)

            assertTrue(viewModel.sendCurrentTest())
            assertFalse(viewModel.sendCurrentTest())
            runCurrent()

            assertEquals(1, transmitter.transmissions)
            assertEquals(RemoteCommand.POWER, viewModel.uiState.value.finder?.testCommand)
            assertTrue(viewModel.uiState.value.finder?.awaitingResponse == true)
            assertFalse(viewModel.uiState.value.finder?.isSending == true)
        }

    @Test
    fun `Power rejection advances without persistence and exhausts the exact candidate list`() =
        runTest(dispatcher) {
            val preferences = AtomicPreferences(RemoteSettings())
            val viewModel = viewModel(preferences = preferences)
            runCurrent()
            openArcelikCandidate(viewModel)
            viewModel.sendCurrentTest()
            runCurrent()

            viewModel.respondToCurrentTest(didRespond = false)

            assertEquals(FinderStep.EXHAUSTED, viewModel.uiState.value.finder?.step)
            assertTrue(preferences.writes.isEmpty())

            assertTrue(viewModel.goBackInFinder())
            assertEquals(FinderStep.MODEL, viewModel.uiState.value.finder?.step)
            viewModel.selectModel("82-507 B")
            assertEquals(FinderStep.TEST, viewModel.uiState.value.finder?.step)
            assertEquals(0, viewModel.uiState.value.finder?.candidateIndex)
        }

    @Test
    fun `confirmed Power and Volume checks reach naming without opening Source`() =
        runTest(dispatcher) {
            val transmitter = RecordingTransmitter()
            val viewModel = viewModel(transmitter = transmitter)
            runCurrent()
            openArcelikCandidate(viewModel)

            sendAndConfirm(viewModel)
            assertEquals(RemoteCommand.VOLUME_UP, viewModel.uiState.value.finder?.testCommand)
            assertFalse(viewModel.uiState.value.finder?.awaitingResponse == true)

            sendAndConfirm(viewModel)
            assertEquals(FinderStep.NAME, viewModel.uiState.value.finder?.step)
            assertEquals(2, transmitter.transmissions)
        }

    @Test
    fun `unknown model tries every brand candidate and exhausts only after the final rejection`() =
        runTest(dispatcher) {
            val preferences = AtomicPreferences(RemoteSettings())
            val viewModel = viewModel(preferences = preferences)
            runCurrent()
            viewModel.startAddDevice()
            viewModel.selectBrand("LG")
            viewModel.selectModel(null)

            val candidateIds = requireNotNull(viewModel.uiState.value.finder).candidateIds
            assertTrue(candidateIds.size > 1)
            assertNull(viewModel.uiState.value.finder?.selectedModel)
            assertEquals(candidateIds.first(), viewModel.uiState.value.finder?.currentProfile?.id)

            sendAndConfirm(viewModel)
            assertEquals(RemoteCommand.VOLUME_UP, viewModel.uiState.value.finder?.testCommand)
            assertTrue(viewModel.sendCurrentTest())
            runCurrent()
            viewModel.respondToCurrentTest(didRespond = false)

            assertEquals(1, viewModel.uiState.value.finder?.candidateIndex)
            assertEquals(candidateIds[1], viewModel.uiState.value.finder?.currentProfile?.id)
            assertEquals(RemoteCommand.POWER, viewModel.uiState.value.finder?.testCommand)

            for (index in 1 until candidateIds.size) {
                assertEquals(candidateIds[index], viewModel.uiState.value.finder?.currentProfile?.id)
                assertTrue(viewModel.sendCurrentTest())
                runCurrent()
                viewModel.respondToCurrentTest(didRespond = false)
            }

            assertEquals(FinderStep.EXHAUSTED, viewModel.uiState.value.finder?.step)
            assertEquals(candidateIds.size, viewModel.uiState.value.finder?.candidateIndex)
            assertNull(viewModel.uiState.value.finder?.currentProfile)
            assertTrue(preferences.writes.isEmpty())
        }

    @Test
    fun `IR unavailable permits browsing but rejects tests without transmitter calls`() =
        runTest(dispatcher) {
            val transmitter = RecordingTransmitter(isAvailable = false)
            val viewModel = viewModel(transmitter = transmitter)
            runCurrent()
            openArcelikCandidate(viewModel)

            assertFalse(viewModel.sendCurrentTest())
            runCurrent()

            assertEquals(0, transmitter.transmissions)
            assertEquals("Bu telefonda IR vericisi bulunamadı.", viewModel.uiState.value.finder?.error)
        }

    @Test
    fun `successful save appends exact confirmed profile defaults and preserves unrelated settings`() =
        runTest(dispatcher) {
            val original = richSettings()
            val preferences = AtomicPreferences(original)
            val ids = ArrayDeque(listOf(TV_A, "new-tv"))
            val viewModel = viewModel(preferences = preferences, idGenerator = { ids.removeFirst() })
            val bundledEvidence = ArcelikOldLcdProfile.profile.defaultEvidence
            runCurrent()
            reachNaming(viewModel)
            viewModel.updateTvName("  Yatak Odası  ")

            viewModel.saveCurrentCandidate()
            runCurrent()

            val saved = preferences.current
            val added = saved.savedRemotes.last()
            assertEquals("new-tv", added.id)
            assertEquals("Yatak Odası", added.name)
            assertEquals(ArcelikOldLcdProfile.ID, added.profileId)
            assertEquals(ArcelikOldLcdProfile.profile.layout.defaultQuickActions, added.quickActions)
            assertTrue(added.isConfirmed)
            assertEquals("new-tv", saved.selectedSavedRemoteId)
            assertEquals(original.savedRemotes, saved.savedRemotes.dropLast(1))
            assertEquals(ThemePreference.DARK, saved.theme)
            assertFalse(saved.hapticsEnabled)
            assertEquals(Handedness.LEFT, saved.handedness)
            assertTrue(saved.onboardingCompleted)
            assertEquals(bundledEvidence, ArcelikOldLcdProfile.profile.defaultEvidence)
            assertNull(viewModel.uiState.value.finder)
            assertEquals(DevicesUiEvent.NavigateRemote("new-tv"), viewModel.uiState.value.pendingEvent)
        }

    @Test
    fun `leaving finder cancels its queued send and a late result cannot publish`() =
        runTest(dispatcher) {
            val sequencePaused = CompletableDeferred<Unit>()
            val releaseSequence = CompletableDeferred<Unit>()
            val transmitter = RecordingTransmitter()
            val coordinator = RemoteTransmissionCoordinator(
                RemoteController(transmitter) {
                    sequencePaused.complete(Unit)
                    releaseSequence.await()
                },
            )
            val viewModel = viewModel(coordinator = coordinator)
            runCurrent()
            openArcelikCandidate(viewModel)
            val sequence = RemoteSequence(
                listOf(RemoteSequenceStep(RemoteCommand.POWER, delayAfterMillis = 1)),
            )
            val remoteOwner = launch {
                coordinator.send("remote", ArcelikOldLcdProfile.profile, sequence)
            }
            sequencePaused.await()

            assertTrue(viewModel.sendCurrentTest())
            runCurrent()
            assertTrue(viewModel.uiState.value.finder?.isSending == true)

            viewModel.cancelFinderAndJoin()
            releaseSequence.complete(Unit)
            remoteOwner.join()
            runCurrent()

            assertNull(viewModel.uiState.value.finder)
            assertEquals(1, transmitter.transmissions)
        }

    @Test
    fun `back then immediate exit still joins a non cooperative finder transmission`() =
        runTest(dispatcher) {
            val transmitter = BlockingTransmitter()
            val viewModel = viewModel(
                transmitter = transmitter,
                workDispatcher = Dispatchers.Default,
            )
            runCurrent()
            openArcelikCandidate(viewModel)

            assertTrue(viewModel.sendCurrentTest())
            transmitter.entered.await()
            assertTrue(viewModel.goBackInFinder())

            val leaving = async { viewModel.cancelFinderAndJoin() }
            runCurrent()
            assertFalse(leaving.isCompleted)

            transmitter.release.complete(Unit)
            leaving.await()

            assertNull(viewModel.uiState.value.finder)
            assertNull(viewModel.uiState.value.pendingEvent)
            assertEquals(1, transmitter.transmissions)
        }

    @Test
    fun `save failure retains wizard and Retry is idempotent`() = runTest(dispatcher) {
        val preferences = FailOncePreferences(RemoteSettings())
        val viewModel = viewModel(preferences = preferences, idGenerator = { "stable-id" })
        runCurrent()
        reachNaming(viewModel)
        viewModel.updateTvName("Salon")

        viewModel.saveCurrentCandidate()
        runCurrent()

        assertEquals(FinderStep.NAME, viewModel.uiState.value.finder?.step)
        assertEquals("Kaydedilemedi", viewModel.uiState.value.finder?.saveError)
        assertNull(viewModel.uiState.value.pendingEvent)

        viewModel.saveCurrentCandidate()
        runCurrent()

        assertEquals(1, preferences.current.savedRemotes.size)
        assertEquals("stable-id", preferences.current.savedRemotes.single().id)
        assertEquals(DevicesUiEvent.NavigateRemote("stable-id"), viewModel.uiState.value.pendingEvent)
    }

    @Test
    fun `blank TV name is rejected without persistence`() = runTest(dispatcher) {
        val preferences = AtomicPreferences(RemoteSettings())
        val viewModel = viewModel(preferences = preferences)
        runCurrent()
        reachNaming(viewModel)
        viewModel.updateTvName("   ")

        viewModel.saveCurrentCandidate()
        runCurrent()

        assertEquals("TV adı boş olamaz.", viewModel.uiState.value.finder?.nameError)
        assertTrue(preferences.writes.isEmpty())
        assertNull(viewModel.uiState.value.pendingEvent)
    }

    @Test
    fun `back and edits are blocked during save and leaving joins any late commit`() =
        runTest(dispatcher) {
            val preferences = NonCancellableUpdatePreferences(RemoteSettings())
            val viewModel = viewModel(preferences = preferences, idGenerator = { "saved-tv" })
            runCurrent()
            reachNaming(viewModel)
            viewModel.updateTvName("Salon")

            viewModel.saveCurrentCandidate()
            runCurrent()
            preferences.updateEntered.await()
            assertTrue(viewModel.uiState.value.finder?.isSaving == true)

            assertTrue(viewModel.goBackInFinder())
            viewModel.updateTvName("Late edit")
            assertEquals(FinderStep.NAME, viewModel.uiState.value.finder?.step)
            assertEquals("Salon", viewModel.uiState.value.finder?.tvName)

            val leaving = async { viewModel.cancelFinderAndJoin() }
            runCurrent()
            assertFalse(leaving.isCompleted)
            assertNull(viewModel.uiState.value.finder)

            preferences.releaseUpdate.complete(Unit)
            leaving.await()
            viewModel.startAddDevice()
            runCurrent()

            assertEquals(FinderStep.BRAND, viewModel.uiState.value.finder?.step)
            assertNull(viewModel.uiState.value.pendingEvent)
            assertEquals("saved-tv", preferences.current.savedRemotes.single().id)
        }

    @Test
    fun `select and delete rebase atomically and follow deterministic selection rules`() =
        runTest(dispatcher) {
            val preferences = AtomicPreferences(richSettings())
            val viewModel = viewModel(preferences = preferences)
            runCurrent()

            viewModel.selectRemote(TV_B)
            runCurrent()
            assertEquals(TV_B, preferences.current.selectedSavedRemoteId)
            assertEquals(DevicesUiEvent.NavigateRemote(TV_B), viewModel.uiState.value.pendingEvent)
            viewModel.consumeEvent()

            viewModel.deleteRemote(TV_A)
            runCurrent()
            assertEquals(listOf(TV_B), preferences.current.savedRemotes.map(SavedRemote::id))
            assertEquals(TV_B, preferences.current.selectedSavedRemoteId)

            viewModel.deleteRemote(TV_B)
            runCurrent()
            assertTrue(preferences.current.savedRemotes.isEmpty())
            assertNull(preferences.current.selectedSavedRemoteId)
            assertEquals(DevicesUiEvent.OpenAddDevice, viewModel.uiState.value.pendingEvent)
        }

    @Test
    fun `deleting selected TV selects the first remaining TV`() = runTest(dispatcher) {
        val preferences = AtomicPreferences(richSettings())
        val viewModel = viewModel(preferences = preferences)
        runCurrent()

        viewModel.deleteRemote(TV_A)
        runCurrent()

        assertEquals(listOf(TV_B), preferences.current.savedRemotes.map(SavedRemote::id))
        assertEquals(TV_B, preferences.current.selectedSavedRemoteId)
        assertNull(viewModel.uiState.value.pendingEvent)
    }

    @Test
    fun `selection aborted by a concurrent removal clears progress and never navigates`() =
        runTest(dispatcher) {
            val preferences = AbortUpdatePreferences(richSettings())
            val viewModel = viewModel(preferences = preferences)
            runCurrent()

            viewModel.selectRemote(TV_B)
            runCurrent()

            assertFalse(viewModel.uiState.value.mutationInProgress)
            assertEquals("Değişiklik artık geçerli değil.", viewModel.uiState.value.mutationError)
            assertNull(viewModel.uiState.value.pendingEvent)
        }

    @Test
    fun `platform failure remains on candidate and Retry sends only when tapped again`() =
        runTest(dispatcher) {
            val transmitter = RecordingTransmitter(result = TransmissionResult.PlatformFailure("blocked"))
            val viewModel = viewModel(transmitter = transmitter)
            runCurrent()
            openArcelikCandidate(viewModel)

            viewModel.sendCurrentTest()
            runCurrent()
            assertEquals(1, transmitter.transmissions)
            assertEquals("IR sinyali gönderilemedi. Tekrar deneyin.", viewModel.uiState.value.finder?.error)
            assertFalse(viewModel.uiState.value.finder?.awaitingResponse == true)

            viewModel.sendCurrentTest()
            runCurrent()
            assertEquals(2, transmitter.transmissions)
        }

    private fun openArcelikCandidate(viewModel: DevicesViewModel) {
        viewModel.startAddDevice()
        viewModel.selectBrand("Arçelik")
        viewModel.selectModel("82-507 B")
        assertEquals(FinderStep.TEST, viewModel.uiState.value.finder?.step)
    }

    private fun TestScope.sendAndConfirm(viewModel: DevicesViewModel) {
        assertTrue(viewModel.sendCurrentTest())
        runCurrent()
        assertTrue(viewModel.uiState.value.finder?.awaitingResponse == true)
        viewModel.respondToCurrentTest(didRespond = true)
    }

    private fun TestScope.reachNaming(viewModel: DevicesViewModel) {
        openArcelikCandidate(viewModel)
        sendAndConfirm(viewModel)
        sendAndConfirm(viewModel)
        assertEquals(FinderStep.NAME, viewModel.uiState.value.finder?.step)
    }

    private fun viewModel(
        preferences: RemotePreferences = AtomicPreferences(RemoteSettings()),
        transmitter: RecordingTransmitter = RecordingTransmitter(),
        coordinator: RemoteTransmissionCoordinator = RemoteTransmissionCoordinator(RemoteController(transmitter)),
        idGenerator: () -> String = { "generated-tv" },
        workDispatcher: CoroutineDispatcher = dispatcher,
    ) = DevicesViewModel(
        coordinator = coordinator,
        catalog = RemoteProfileCatalog(),
        preferences = preferences,
        idGenerator = idGenerator,
        workDispatcher = workDispatcher,
    )

    private fun richSettings(): RemoteSettings {
        val first = SavedRemote(
            TV_A,
            "Salon",
            ArcelikOldLcdProfile.ID,
            listOf(RemoteAction.Command(RemoteCommand.SOURCE)),
            true,
        )
        val second = SavedRemote(TV_B, "Mutfak", BekoCompatibleProfile.ID)
        return RemoteSettings(
            savedRemotes = listOf(first, second),
            selectedSavedRemoteId = TV_A,
            theme = ThemePreference.DARK,
            hapticsEnabled = false,
            handedness = Handedness.LEFT,
            onboardingCompleted = true,
        )
    }

    private open class AtomicPreferences(initial: RemoteSettings) : RemotePreferences {
        protected val mutableSettings = MutableStateFlow(initial)
        override val settings: Flow<RemoteSettings> = mutableSettings
        val writes = mutableListOf<RemoteSettings>()
        val current get() = mutableSettings.value

        override suspend fun save(settings: RemoteSettings) {
            writes += settings
            mutableSettings.value = settings
        }

        override suspend fun update(transform: (RemoteSettings) -> RemoteSettings?): RemoteSettings? {
            val updated = transform(mutableSettings.value) ?: return null
            save(updated)
            return updated
        }
    }

    private class FailOncePreferences(initial: RemoteSettings) : AtomicPreferences(initial) {
        private var failed = false
        override suspend fun update(transform: (RemoteSettings) -> RemoteSettings?): RemoteSettings? {
            if (!failed) {
                failed = true
                error("disk full")
            }
            return super.update(transform)
        }
    }

    private class AbortUpdatePreferences(initial: RemoteSettings) : AtomicPreferences(initial) {
        override suspend fun update(transform: (RemoteSettings) -> RemoteSettings?): RemoteSettings? = null
    }

    private class NonCancellableUpdatePreferences(initial: RemoteSettings) : AtomicPreferences(initial) {
        val updateEntered = CompletableDeferred<Unit>()
        val releaseUpdate = CompletableDeferred<Unit>()

        override suspend fun update(transform: (RemoteSettings) -> RemoteSettings?): RemoteSettings? =
            withContext(NonCancellable) {
                updateEntered.complete(Unit)
                releaseUpdate.await()
                val updated = transform(current) ?: return@withContext null
                save(updated)
                updated
            }
    }

    private open class RecordingTransmitter(
        override val isAvailable: Boolean = true,
        var result: TransmissionResult = TransmissionResult.Success,
    ) : IrTransmitter {
        var transmissions = 0
        override open fun transmit(signal: IrSignal): TransmissionResult {
            transmissions += 1
            return result
        }
    }

    private class BlockingTransmitter : RecordingTransmitter() {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        override fun transmit(signal: IrSignal): TransmissionResult {
            transmissions += 1
            entered.complete(Unit)
            kotlinx.coroutines.runBlocking { release.await() }
            return TransmissionResult.Success
        }
    }

    private companion object {
        const val TV_A = "tv-a"
        const val TV_B = "tv-b"
    }
}
