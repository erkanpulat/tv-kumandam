package com.erkanpulat.tvkumandam.presentation.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erkanpulat.tvkumandam.data.remote.RemoteProfileCatalog
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.model.TransmissionResult
import com.erkanpulat.tvkumandam.domain.preferences.RemotePreferences
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.domain.remote.RemoteTransmissionCoordinator
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Device list mutations and one explicit, human-confirmed profile-finder session. */
class DevicesViewModel internal constructor(
    private val coordinator: RemoteTransmissionCoordinator,
    private val catalog: RemoteProfileCatalog,
    private val preferences: RemotePreferences,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val finderIndex = ProfileFinderIndex(catalog)
    private val mutableUiState = MutableStateFlow(
        DevicesUiState(isIrAvailable = coordinator.isAvailable),
    )
    val uiState: StateFlow<DevicesUiState> = mutableUiState.asStateFlow()

    private var sessionId = 0L
    private var testJob: Job? = null
    private var mutationJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.settings.collect { settings ->
                mutableUiState.update { current ->
                    current.copy(
                        isLoading = false,
                        settings = settings,
                        devices = settings.savedRemotes.mapNotNull { remote ->
                            catalog.findOrNull(remote.profileId)?.let { profile ->
                                DeviceListItem(
                                    remote = remote,
                                    profile = profile,
                                    isSelected = remote.id == settings.selectedSavedRemoteId,
                                )
                            }
                        },
                        isIrAvailable = coordinator.isAvailable,
                    )
                }
            }
        }
    }

    fun startAddDevice() {
        cancelTest()
        sessionId += 1
        mutableUiState.update {
            it.copy(
                finder = ProfileFinderState(
                    step = FinderStep.BRAND,
                    brands = finderIndex.brands,
                ),
                pendingEvent = null,
                mutationError = null,
            )
        }
    }

    fun selectBrand(brand: String) {
        if (brand !in finderIndex.brands) return
        mutableUiState.update { current ->
            val finder = current.finder ?: return@update current
            current.copy(
                finder = finder.copy(
                    step = FinderStep.MODEL,
                    selectedBrand = brand,
                    models = finderIndex.modelsFor(brand),
                    selectedModel = null,
                    candidateIds = emptyList(),
                    currentProfile = null,
                    error = null,
                ),
            )
        }
    }

    /** null represents the explicit “Modelimi bilmiyorum” choice. */
    fun selectModel(modelAlias: String?) {
        val current = mutableUiState.value
        val finder = current.finder ?: return
        val brand = finder.selectedBrand ?: return
        if (modelAlias != null && modelAlias !in finder.models) return
        val candidates = finderIndex.candidates(brand, modelAlias)
        val first = candidates.firstOrNull()
        mutableUiState.update {
            it.copy(
                finder = finder.copy(
                    step = if (first == null) FinderStep.EXHAUSTED else FinderStep.TEST,
                    selectedModel = modelAlias,
                    candidateIds = candidates.map(RemoteProfile::id),
                    candidateIndex = 0,
                    currentProfile = first,
                    testCommand = RemoteCommand.POWER,
                    isSending = false,
                    awaitingResponse = false,
                    error = null,
                ),
            )
        }
    }

    fun sendCurrentTest(): Boolean {
        val state = mutableUiState.value
        val finder = state.finder ?: return false
        val profile = finder.currentProfile ?: return false
        if (finder.step != FinderStep.TEST || finder.isSending || finder.awaitingResponse) return false
        if (!coordinator.isAvailable) {
            mutableUiState.update { current ->
                current.copy(finder = current.finder?.copy(error = IR_UNAVAILABLE))
            }
            return false
        }
        if (profile.commandFor(finder.testCommand) == null || testJob != null) return false

        val capturedSession = sessionId
        val command = finder.testCommand
        val ownerId = "finder-$capturedSession-${profile.id}"
        mutableUiState.update { current ->
            current.copy(
                finder = current.finder?.copy(
                    isSending = true,
                    awaitingResponse = false,
                    error = null,
                ),
            )
        }
        val job = viewModelScope.launch(
            context = workDispatcher,
            start = CoroutineStart.LAZY,
        ) {
            val result = coordinator.send(ownerId, profile, command)
            withContext(Dispatchers.Main.immediate) {
                if (sessionId != capturedSession || mutableUiState.value.finder?.currentProfile?.id != profile.id) {
                    return@withContext
                }
                mutableUiState.update { current ->
                    val active = current.finder ?: return@update current
                    current.copy(
                        finder = active.copy(
                            isSending = false,
                            awaitingResponse = result == TransmissionResult.Success,
                            error = result.errorMessage(),
                        ),
                    )
                }
            }
        }
        testJob = job
        job.invokeOnCompletion { if (testJob === job) testJob = null }
        job.start()
        return true
    }

    fun respondToCurrentTest(didRespond: Boolean) {
        val state = mutableUiState.value
        val finder = state.finder ?: return
        if (finder.step != FinderStep.TEST || !finder.awaitingResponse) return
        if (!didRespond) {
            advanceCandidate(finder)
            return
        }

        val profile = finder.currentProfile ?: return
        val checks = ESSENTIAL_CHECKS.filter { it in profile.supportedCommands }
        val next = checks.getOrNull(checks.indexOf(finder.testCommand) + 1)
        mutableUiState.update { current ->
            current.copy(
                finder = finder.copy(
                    step = if (next == null) FinderStep.NAME else FinderStep.TEST,
                    testCommand = next ?: finder.testCommand,
                    awaitingResponse = false,
                    error = null,
                    tvName = if (next == null && finder.tvName.isBlank()) profile.brand + " TV" else finder.tvName,
                ),
            )
        }
    }

    fun updateTvName(name: String) {
        if (mutableUiState.value.finder?.isSaving == true) return
        mutableUiState.update { current ->
            current.copy(
                finder = current.finder?.copy(
                    tvName = name.take(MAX_TV_NAME_LENGTH),
                    nameError = null,
                    saveError = null,
                ),
            )
        }
    }

    fun saveCurrentCandidate() {
        val finder = mutableUiState.value.finder ?: return
        val profile = finder.currentProfile ?: return
        val name = finder.tvName.trim()
        if (finder.step != FinderStep.NAME || finder.isSaving || mutationJob?.isActive == true) return
        if (name.isBlank()) {
            mutableUiState.update { current ->
                current.copy(finder = current.finder?.copy(nameError = "TV adı boş olamaz."))
            }
            return
        }
        mutableUiState.update { current ->
            current.copy(finder = current.finder?.copy(isSaving = true, nameError = null, saveError = null))
        }
        startMutation(
            transform = { current ->
                val id = generateUniqueId(current) ?: error("Benzersiz TV kimliği üretilemedi.")
                val quickActions = profile.layout.defaultQuickActions
                    .filter { action -> action != RemoteAction.Command(RemoteCommand.POWER) && action in profile.supportedActions }
                    .distinct()
                    .take(SavedRemote.MAX_QUICK_ACTIONS)
                val remote = SavedRemote(id, name, profile.id, quickActions, isConfirmed = true)
                current.copy(
                    savedRemotes = current.savedRemotes + remote,
                    selectedSavedRemoteId = id,
                    onboardingCompleted = true,
                )
            },
            onSuccess = { committed ->
                mutableUiState.update { current ->
                    current.copy(
                        finder = null,
                        pendingEvent = DevicesUiEvent.NavigateRemote(
                            requireNotNull(committed.selectedSavedRemoteId),
                        ),
                    )
                }
            },
            onFailure = {
                mutableUiState.update { current ->
                    current.copy(
                        finder = current.finder?.copy(isSaving = false, saveError = "Kaydedilemedi"),
                    )
                }
            },
        )
    }

    fun selectRemote(remoteId: String) {
        if (mutationJob?.isActive == true) return
        startMutation(
            transform = { current ->
                if (current.savedRemotes.none { it.id == remoteId }) null
                else current.copy(selectedSavedRemoteId = remoteId)
            },
            onSuccess = {
                mutableUiState.update {
                    it.copy(pendingEvent = DevicesUiEvent.NavigateRemote(remoteId))
                }
            },
        )
    }

    fun deleteRemote(remoteId: String) {
        if (mutationJob?.isActive == true) return
        startMutation(
            transform = { current ->
                if (current.savedRemotes.none { it.id == remoteId }) return@startMutation null
                val remaining = current.savedRemotes.filterNot { it.id == remoteId }
                val selection = if (current.selectedSavedRemoteId == remoteId) {
                    remaining.firstOrNull()?.id
                } else {
                    current.selectedSavedRemoteId
                }
                current.copy(savedRemotes = remaining, selectedSavedRemoteId = selection)
            },
            onSuccess = { committed ->
                if (committed.savedRemotes.isEmpty()) {
                    mutableUiState.update { it.copy(pendingEvent = DevicesUiEvent.OpenAddDevice) }
                }
            },
        )
    }

    fun consumeEvent() {
        mutableUiState.update { it.copy(pendingEvent = null) }
    }

    /** Returns false only at the first finder step. */
    fun goBackInFinder(): Boolean {
        val finder = mutableUiState.value.finder ?: return false
        if (finder.isSaving) return true
        cancelTest()
        val previous = when (finder.step) {
            FinderStep.BRAND -> return false
            FinderStep.MODEL -> finder.copy(
                step = FinderStep.BRAND,
                selectedBrand = null,
                models = emptyList(),
                selectedModel = null,
            )
            FinderStep.TEST, FinderStep.EXHAUSTED -> finder.copy(
                step = FinderStep.MODEL,
                candidateIds = emptyList(),
                candidateIndex = 0,
                currentProfile = null,
                testCommand = RemoteCommand.POWER,
                isSending = false,
                awaitingResponse = false,
                error = null,
            )
            FinderStep.NAME -> finder.copy(
                step = FinderStep.TEST,
                testCommand = RemoteCommand.POWER,
                isSending = false,
                awaitingResponse = false,
                error = null,
                saveError = null,
            )
        }
        mutableUiState.update { it.copy(finder = previous) }
        return true
    }

    suspend fun cancelFinderAndJoin() {
        sessionId += 1
        val activeTest = testJob
        val activeMutation = mutationJob
        testJob = null
        mutationJob = null
        mutableUiState.update {
            it.copy(
                finder = null,
                mutationInProgress = false,
                pendingEvent = null,
            )
        }
        activeTest?.cancelAndJoin()
        activeMutation?.cancelAndJoin()
        // A non-cancellable persistence boundary may have completed while we
        // joined it; revoke any publication it produced before exposing a new finder.
        mutableUiState.update {
            it.copy(
                finder = null,
                mutationInProgress = false,
                pendingEvent = null,
            )
        }
    }

    override fun onCleared() {
        cancelTest()
        mutationJob?.cancel()
    }

    private fun advanceCandidate(finder: ProfileFinderState) {
        val nextIndex = finder.candidateIndex + 1
        val nextProfile = finder.candidateIds.getOrNull(nextIndex)?.let(catalog::findOrNull)
        mutableUiState.update { current ->
            current.copy(
                finder = finder.copy(
                    step = if (nextProfile == null) FinderStep.EXHAUSTED else FinderStep.TEST,
                    candidateIndex = nextIndex,
                    currentProfile = nextProfile,
                    testCommand = RemoteCommand.POWER,
                    isSending = false,
                    awaitingResponse = false,
                    error = null,
                ),
            )
        }
    }

    private fun startMutation(
        transform: (RemoteSettings) -> RemoteSettings?,
        onSuccess: (RemoteSettings) -> Unit,
        onFailure: () -> Unit = {
            mutableUiState.update { it.copy(mutationError = "Değişiklik kaydedilemedi.") }
        },
    ) {
        mutableUiState.update {
            it.copy(
                mutationInProgress = true,
                mutationError = null,
                pendingEvent = null,
            )
        }
        val job = viewModelScope.launch(
            context = workDispatcher,
            start = CoroutineStart.LAZY,
        ) {
            try {
                val committed = preferences.update(transform)
                if (committed == null) {
                    withContext(Dispatchers.Main.immediate) {
                        mutableUiState.update {
                            it.copy(
                                mutationInProgress = false,
                                mutationError = "Değişiklik artık geçerli değil.",
                            )
                        }
                    }
                    return@launch
                }
                withContext(Dispatchers.Main.immediate) {
                    mutableUiState.update { it.copy(mutationInProgress = false, settings = committed) }
                    onSuccess(committed)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                withContext(Dispatchers.Main.immediate) {
                    mutableUiState.update { it.copy(mutationInProgress = false) }
                    onFailure()
                }
            }
        }
        mutationJob = job
        job.invokeOnCompletion {
            if (mutationJob === job) mutationJob = null
        }
        job.start()
    }

    private fun generateUniqueId(settings: RemoteSettings): String? {
        val used = settings.savedRemotes.mapTo(HashSet(), SavedRemote::id)
        repeat(MAX_ID_ATTEMPTS) {
            val candidate = idGenerator().trim()
            if (candidate.isNotEmpty() && candidate !in used) return candidate
        }
        return null
    }

    private fun cancelTest() {
        testJob?.cancel()
    }

    private fun TransmissionResult.errorMessage(): String? = when (this) {
        TransmissionResult.Success -> null
        TransmissionResult.UnsupportedDevice -> IR_UNAVAILABLE
        is TransmissionResult.UnsupportedCarrier -> "Telefon bu IR frekansını desteklemiyor."
        TransmissionResult.CommandUnavailable -> "Bu komut kullanılamıyor."
        is TransmissionResult.EncodingFailure -> "IR komutu hazırlanamadı."
        is TransmissionResult.PlatformFailure -> "IR sinyali gönderilemedi. Tekrar deneyin."
    }

    private companion object {
        const val IR_UNAVAILABLE = "Bu telefonda IR vericisi bulunamadı."
        const val MAX_TV_NAME_LENGTH = 40
        const val MAX_ID_ATTEMPTS = 8
        val ESSENTIAL_CHECKS = listOf(
            RemoteCommand.POWER,
            RemoteCommand.VOLUME_UP,
        )
    }
}
