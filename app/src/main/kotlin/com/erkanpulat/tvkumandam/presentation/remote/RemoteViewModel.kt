package com.erkanpulat.tvkumandam.presentation.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erkanpulat.tvkumandam.data.remote.RemoteProfileCatalog
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteShortcut
import com.erkanpulat.tvkumandam.domain.model.SavedMacro
import com.erkanpulat.tvkumandam.domain.model.TransmissionResult
import com.erkanpulat.tvkumandam.domain.preferences.RemotePreferences
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.domain.remote.RemoteTransmissionCoordinator
import com.erkanpulat.tvkumandam.domain.remote.SequenceProgress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal sealed interface TransmissionPublication {
    data class CommandResult(
        val savedRemoteId: String,
        val profileId: String,
        val command: RemoteCommand,
    ) : TransmissionPublication

    data class ShortcutProgress(
        val savedRemoteId: String,
        val profileId: String,
        val shortcut: RemoteShortcut,
        val progress: SequenceProgress,
    ) : TransmissionPublication

    data class ShortcutResult(
        val savedRemoteId: String,
        val profileId: String,
        val shortcut: RemoteShortcut,
    ) : TransmissionPublication

    data class MacroProgress(
        val savedRemoteId: String,
        val profileId: String,
        val macroId: String,
        val progress: SequenceProgress,
    ) : TransmissionPublication

    data class MacroResult(
        val savedRemoteId: String,
        val profileId: String,
        val macroId: String,
    ) : TransmissionPublication
}

/** A no-op production gate; deterministic tests can pause exactly at publication boundaries. */
internal fun interface TransmissionPublicationGate {
    suspend fun await(publication: TransmissionPublication)

    companion object {
        val None = TransmissionPublicationGate { }
    }
}

class RemoteViewModel internal constructor(
    private val transmissionCoordinator: RemoteTransmissionCoordinator,
    private val catalog: RemoteProfileCatalog,
    private val preferences: RemotePreferences,
    private val transmissionDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val publicationGate: TransmissionPublicationGate = TransmissionPublicationGate.None,
) : ViewModel() {
    private val defaultProfile = catalog.find(null)
    private val mutableUiState = MutableStateFlow(
        RemoteUiState.fromSettings(
            profiles = catalog.profiles,
            settings = RemoteSettings(),
            isIrAvailable = transmissionCoordinator.isAvailable,
            fallbackProfileId = defaultProfile.id,
        ).copy(isLoadingPreferences = true),
    )
    val uiState: StateFlow<RemoteUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<RemoteUiEvent>(Channel.BUFFERED)
    val events: Flow<RemoteUiEvent> = eventChannel.receiveAsFlow()

    private val transmissionLock = Any()
    private var activeTransmissionJob: Job? = null
    private var activeTransmissionId = 0L
    private var pendingRepeat: CommandRequest? = null
    private var transmissionAdmissionOpen = true

    init {
        viewModelScope.launch {
            preferences.settings.collect { settings ->
                val effectiveRemoteId = settings.selectedRemote?.id
                val effectiveProfileId = catalog.find(settings.selectedRemote?.profileId).id
                val current = mutableUiState.value
                if (
                    current.selectedRemote?.id != effectiveRemoteId ||
                    current.selectedProfileId != effectiveProfileId
                ) {
                    cancelTransmission()
                }
                mutableUiState.update { current ->
                    RemoteUiState.fromSettings(
                        profiles = catalog.profiles,
                        settings = settings,
                        isIrAvailable = transmissionCoordinator.isAvailable,
                        transmissionState = current.transmissionState,
                        fallbackProfileId = effectiveProfileId,
                    )
                }
            }
        }
    }

    fun sendCommand(command: RemoteCommand): Boolean {
        val currentState = uiState.value
        val selectedRemote = currentState.selectedRemote
        val profile = currentState.selectedProfile
        if (selectedRemote == null || profile == null) {
            eventChannel.trySend(RemoteUiEvent.CommandUnavailable)
            return false
        }
        if (!transmissionCoordinator.isAvailable) {
            eventChannel.trySend(RemoteUiEvent.IrUnsupported)
            return false
        }
        if (profile.commandFor(command) == null) {
            eventChannel.trySend(RemoteUiEvent.CommandUnavailable)
            return false
        }

        val request = CommandRequest(selectedRemote.id, profile.id, command)
        var admittedJob: Job? = null
        var accepted = false
        synchronized(transmissionLock) {
            if (!transmissionAdmissionOpen) return@synchronized
            if (activeTransmissionJob != null) {
                val activeCommand = uiState.value.transmissionState as? TransmissionState.Command
                if (
                    command.isCoalescible &&
                    activeCommand != null &&
                    activeCommand.savedRemoteId == request.savedRemoteId &&
                    activeCommand.profileId == request.profileId &&
                    activeCommand.command == request.command &&
                    pendingRepeat == null
                ) {
                    pendingRepeat = request
                    accepted = true
                }
                return@synchronized
            }
            if (
                uiState.value.selectedRemote?.id != request.savedRemoteId ||
                uiState.value.selectedProfileId != request.profileId
            ) return@synchronized

            val transmissionId = ++activeTransmissionId
            val job = viewModelScope.launch(
                context = transmissionDispatcher,
                start = CoroutineStart.LAZY,
            ) {
                executeCommand(transmissionId, request)
            }
            activeTransmissionJob = job
            job.invokeOnCompletion { finishTransmission(transmissionId) }
            mutableUiState.update {
                it.copy(
                    transmissionState = TransmissionState.Command(
                        savedRemoteId = request.savedRemoteId,
                        profileId = request.profileId,
                        command = request.command,
                    ),
                )
            }
            admittedJob = job
            accepted = true
        }
        admittedJob?.start()
        return accepted
    }

    fun sendShortcut(shortcut: RemoteShortcut): Boolean {
        val currentState = uiState.value
        val selectedRemote = currentState.selectedRemote
        val profile = currentState.selectedProfile
        if (selectedRemote == null || profile == null) {
            eventChannel.trySend(RemoteUiEvent.CommandUnavailable)
            return false
        }
        if (!transmissionCoordinator.isAvailable) {
            eventChannel.trySend(RemoteUiEvent.IrUnsupported)
            return false
        }
        val sequence = profile.shortcutFor(shortcut)
        if (sequence == null) {
            eventChannel.trySend(RemoteUiEvent.CommandUnavailable)
            return false
        }

        var admittedJob: Job? = null
        var accepted = false
        synchronized(transmissionLock) {
            if (!transmissionAdmissionOpen) return@synchronized
            if (activeTransmissionJob != null) return@synchronized
            if (
                uiState.value.selectedRemote?.id != selectedRemote.id ||
                uiState.value.selectedProfileId != profile.id
            ) return@synchronized

            val transmissionId = ++activeTransmissionId
            val job = viewModelScope.launch(
                context = transmissionDispatcher,
                start = CoroutineStart.LAZY,
            ) {
                executeShortcut(transmissionId, selectedRemote.id, profile.id, shortcut)
            }
            activeTransmissionJob = job
            job.invokeOnCompletion { finishTransmission(transmissionId) }
            mutableUiState.update {
                it.copy(
                    transmissionState = TransmissionState.Shortcut(
                        savedRemoteId = selectedRemote.id,
                        profileId = profile.id,
                        shortcut = shortcut,
                        completedSteps = 0,
                        totalSteps = sequence.steps.size,
                    ),
                )
            }
            admittedJob = job
            accepted = true
        }
        admittedJob?.start()
        return accepted
    }

    fun sendMacro(macroId: String): Boolean {
        val currentState = uiState.value
        val selectedRemote = currentState.selectedRemote
        val profile = currentState.selectedProfile
        val macro = selectedRemote?.macros?.firstOrNull { it.id == macroId }
        if (selectedRemote == null || profile == null || macro == null) {
            eventChannel.trySend(RemoteUiEvent.CommandUnavailable)
            return false
        }
        if (!transmissionCoordinator.isAvailable) {
            eventChannel.trySend(RemoteUiEvent.IrUnsupported)
            return false
        }
        val sequence = profile.sequenceFor(macro)
        if (sequence == null) {
            eventChannel.trySend(RemoteUiEvent.CommandUnavailable)
            return false
        }

        var admittedJob: Job? = null
        var accepted = false
        synchronized(transmissionLock) {
            if (!transmissionAdmissionOpen || activeTransmissionJob != null) return@synchronized
            if (uiState.value.selectedRemote?.id != selectedRemote.id || uiState.value.selectedProfileId != profile.id) {
                return@synchronized
            }
            val transmissionId = ++activeTransmissionId
            val job = viewModelScope.launch(context = transmissionDispatcher, start = CoroutineStart.LAZY) {
                executeMacro(transmissionId, selectedRemote.id, profile.id, macro)
            }
            activeTransmissionJob = job
            job.invokeOnCompletion { finishTransmission(transmissionId) }
            mutableUiState.update {
                it.copy(
                    transmissionState = TransmissionState.Macro(
                        savedRemoteId = selectedRemote.id,
                        profileId = profile.id,
                        macroId = macro.id,
                        macroName = macro.name,
                        completedSteps = 0,
                        totalSteps = sequence.steps.size,
                    ),
                )
            }
            admittedJob = job
            accepted = true
        }
        admittedJob?.start()
        return accepted
    }

    /** Stops only the remaining work; cancellation is deliberately not a success event. */
    fun cancelTransmission() {
        revokeTransmission()?.cancel()
    }

    /**
     * Closes admission before revoking the owner, so the still-visible Remote
     * frame cannot admit another press while navigation waits for cancellation.
     */
    suspend fun suspendTransmissionAdmissionAndCancel() {
        val job = synchronized(transmissionLock) {
            transmissionAdmissionOpen = false
            revokeTransmissionLocked()
        }
        job?.cancelAndJoin()
    }

    /** Re-opens command admission immediately before the Remote destination is shown. */
    fun resumeTransmissionAdmission() {
        synchronized(transmissionLock) {
            transmissionAdmissionOpen = true
        }
    }

    private fun revokeTransmission(): Job? {
        return synchronized(transmissionLock) {
            revokeTransmissionLocked()
        }
    }

    private fun revokeTransmissionLocked(): Job? {
        pendingRepeat = null
        val activeJob = activeTransmissionJob ?: return null

        // Revocation is linearized with publication. Drop the owner before
        // cancelling so its captured id can no longer publish or clean up state.
        activeTransmissionId += 1
        activeTransmissionJob = null
        mutableUiState.update { it.copy(transmissionState = TransmissionState.Idle) }
        return activeJob
    }

    override fun onCleared() {
        cancelTransmission()
        eventChannel.close()
    }

    private suspend fun executeCommand(
        transmissionId: Long,
        initialRequest: CommandRequest,
    ) {
        var request = initialRequest
        while (true) {
            val profile = catalog.find(request.profileId)
            val result = transmissionCoordinator.send(request.savedRemoteId, profile, request.command)
            currentCoroutineContext().ensureActive()
            publicationGate.await(
                TransmissionPublication.CommandResult(
                    savedRemoteId = request.savedRemoteId,
                    profileId = request.profileId,
                    command = request.command,
                ),
            )
            val published = publishIfCurrent(
                transmissionId,
                request.savedRemoteId,
                request.profileId,
            ) {
                eventChannel.trySend(result.toUiEvent(request.command))
            }

            val nextRequest = synchronized(transmissionLock) {
                if (
                    published &&
                    result == TransmissionResult.Success &&
                    activeTransmissionId == transmissionId &&
                    uiState.value.selectedRemote?.id == request.savedRemoteId &&
                    uiState.value.selectedProfileId == request.profileId
                ) {
                    pendingRepeat.also { pendingRepeat = null }
                } else {
                    pendingRepeat = null
                    null
                }
            } ?: break
            currentCoroutineContext().ensureActive()
            request = nextRequest
        }
    }

    private suspend fun executeShortcut(
        transmissionId: Long,
        savedRemoteId: String,
        profileId: String,
        shortcut: RemoteShortcut,
    ) {
        val profile = catalog.find(profileId)
        val sequence = profile.shortcutFor(shortcut)
        val result = if (sequence == null) {
            TransmissionResult.CommandUnavailable
        } else {
            transmissionCoordinator.send(savedRemoteId, profile, sequence) { progress ->
                publicationGate.await(
                    TransmissionPublication.ShortcutProgress(
                        savedRemoteId = savedRemoteId,
                        profileId = profileId,
                        shortcut = shortcut,
                        progress = progress,
                    ),
                )
                publishIfCurrent(transmissionId, savedRemoteId, profileId) {
                    mutableUiState.update { current ->
                        val active = current.transmissionState
                            as? TransmissionState.Shortcut
                        if (
                            active != null &&
                            active.savedRemoteId == savedRemoteId &&
                            active.profileId == profileId &&
                            active.shortcut == shortcut
                        ) {
                            current.copy(
                                transmissionState = active.copy(
                                    completedSteps = progress.completedSteps,
                                    totalSteps = progress.totalSteps,
                                ),
                            )
                        } else {
                            current
                        }
                    }
                }
            }
        }
        currentCoroutineContext().ensureActive()
        publicationGate.await(
            TransmissionPublication.ShortcutResult(
                savedRemoteId = savedRemoteId,
                profileId = profileId,
                shortcut = shortcut,
            ),
        )
        publishIfCurrent(transmissionId, savedRemoteId, profileId) {
            eventChannel.trySend(result.toUiEvent(shortcut))
        }
    }

    private suspend fun executeMacro(
        transmissionId: Long,
        savedRemoteId: String,
        profileId: String,
        macro: SavedMacro,
    ) {
        val profile = catalog.find(profileId)
        val sequence = profile.sequenceFor(macro)
        val result = if (sequence == null) TransmissionResult.CommandUnavailable else {
            transmissionCoordinator.send(savedRemoteId, profile, sequence) { progress ->
                publicationGate.await(
                    TransmissionPublication.MacroProgress(savedRemoteId, profileId, macro.id, progress),
                )
                publishIfCurrent(transmissionId, savedRemoteId, profileId) {
                    mutableUiState.update { current ->
                        val active = current.transmissionState as? TransmissionState.Macro
                        if (active?.macroId == macro.id) {
                            current.copy(
                                transmissionState = active.copy(
                                    completedSteps = progress.completedSteps,
                                    totalSteps = progress.totalSteps,
                                ),
                            )
                        } else current
                    }
                }
            }
        }
        currentCoroutineContext().ensureActive()
        publicationGate.await(TransmissionPublication.MacroResult(savedRemoteId, profileId, macro.id))
        publishIfCurrent(transmissionId, savedRemoteId, profileId) {
            eventChannel.trySend(result.toUiEvent(macro))
        }
    }

    private fun finishTransmission(transmissionId: Long) {
        synchronized(transmissionLock) {
            if (activeTransmissionId != transmissionId) return
            activeTransmissionJob = null
            pendingRepeat = null
            mutableUiState.update {
                it.copy(transmissionState = TransmissionState.Idle)
            }
        }
    }

    private inline fun publishIfCurrent(
        transmissionId: Long,
        savedRemoteId: String,
        profileId: String,
        publication: () -> Unit,
    ): Boolean = synchronized(transmissionLock) {
        val canPublish =
            activeTransmissionJob != null &&
                activeTransmissionId == transmissionId &&
                uiState.value.selectedRemote?.id == savedRemoteId &&
                uiState.value.selectedProfileId == profileId
        if (canPublish) publication()
        canPublish
    }

    private fun TransmissionResult.toUiEvent(command: RemoteCommand): RemoteUiEvent = when (this) {
        TransmissionResult.Success -> RemoteUiEvent.CommandSent(command)
        TransmissionResult.UnsupportedDevice -> RemoteUiEvent.IrUnsupported
        is TransmissionResult.UnsupportedCarrier -> RemoteUiEvent.TransmissionFailed(
            "Telefon bu IR frekansını desteklemiyor.",
        )
        TransmissionResult.CommandUnavailable -> RemoteUiEvent.CommandUnavailable
        is TransmissionResult.EncodingFailure -> RemoteUiEvent.TransmissionFailed(message)
        is TransmissionResult.PlatformFailure -> RemoteUiEvent.TransmissionFailed(message)
    }

    private fun TransmissionResult.toUiEvent(shortcut: RemoteShortcut): RemoteUiEvent = when (this) {
        TransmissionResult.Success -> RemoteUiEvent.ShortcutSent(shortcut)
        TransmissionResult.UnsupportedDevice -> RemoteUiEvent.IrUnsupported
        is TransmissionResult.UnsupportedCarrier -> RemoteUiEvent.TransmissionFailed(
            "Telefon bu IR frekansını desteklemiyor.",
        )
        TransmissionResult.CommandUnavailable -> RemoteUiEvent.CommandUnavailable
        is TransmissionResult.EncodingFailure -> RemoteUiEvent.TransmissionFailed(message)
        is TransmissionResult.PlatformFailure -> RemoteUiEvent.TransmissionFailed(message)
    }

    private fun TransmissionResult.toUiEvent(macro: SavedMacro): RemoteUiEvent = when (this) {
        TransmissionResult.Success -> RemoteUiEvent.MacroSent(macro.name)
        TransmissionResult.UnsupportedDevice -> RemoteUiEvent.IrUnsupported
        is TransmissionResult.UnsupportedCarrier -> RemoteUiEvent.TransmissionFailed(
            "Telefon bu IR frekansını desteklemiyor.",
        )
        TransmissionResult.CommandUnavailable -> RemoteUiEvent.CommandUnavailable
        is TransmissionResult.EncodingFailure -> RemoteUiEvent.TransmissionFailed(message)
        is TransmissionResult.PlatformFailure -> RemoteUiEvent.TransmissionFailed(message)
    }

    private data class CommandRequest(
        val savedRemoteId: String,
        val profileId: String,
        val command: RemoteCommand,
    )

    private val RemoteCommand.isCoalescible: Boolean
        get() = when (this) {
            RemoteCommand.VOLUME_UP,
            RemoteCommand.VOLUME_DOWN,
            RemoteCommand.CHANNEL_UP,
            RemoteCommand.CHANNEL_DOWN,
            -> true
            else -> false
        }
}
