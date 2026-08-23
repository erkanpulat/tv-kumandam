package com.erkanpulat.tvkumandam.presentation.customize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erkanpulat.tvkumandam.data.remote.RemoteProfileCatalog
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.model.SavedMacro
import com.erkanpulat.tvkumandam.domain.preferences.RemotePreferences
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CustomizeRemoteUiState(
    val isLoading: Boolean = true,
    val remote: SavedRemote? = null,
    val profile: RemoteProfile? = null,
    val settings: RemoteSettings = RemoteSettings(),
    val actions: List<RemoteAction> = emptyList(),
    val availableActions: List<RemoteAction> = emptyList(),
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val saveError: String? = null,
    val exitBlocked: Boolean = false,
    val shouldExit: Boolean = false,
) {
    val hapticsEnabled: Boolean get() = settings.hapticsEnabled
    val macros: List<SavedMacro> get() = remote?.macros.orEmpty()
}

/**
 * Owns a single saved TV's customization session. Persistence is deliberately
 * conflated: at most one snapshot is being written and one newer snapshot is pending.
 */
class CustomizeRemoteViewModel internal constructor(
    private val targetRemoteId: String,
    private val preferences: RemotePreferences,
    private val catalog: RemoteProfileCatalog,
    private val persistenceDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private data class WriteRequest(
        val generation: Long,
        val actions: List<RemoteAction>,
        val macros: List<SavedMacro>,
        val projectedSnapshot: RemoteSettings,
        val expectedProfileId: String,
    )

    private val mutableUiState = MutableStateFlow(CustomizeRemoteUiState())
    val uiState: StateFlow<CustomizeRemoteUiState> = mutableUiState.asStateFlow()

    private val writeLock = Any()
    private var writeWorker: Job? = null
    private var pendingWrite: WriteRequest? = null
    private var generation = 0L
    private var capturedProfileId: String? = null
    private var latestReceivedSettings = RemoteSettings()
    private var latestAcceptedSnapshot: RemoteSettings? = null
    private var awaitingEcho: RemoteSettings? = null
    private var exitRequested = false

    init {
        viewModelScope.launch {
            preferences.settings.collect(::reconcileIncomingSettings)
        }
    }

    fun add(action: RemoteAction): Boolean = edit { profile, actions ->
        QuickActionEditor.add(profile, actions, action, mutableUiState.value.macros)
    }

    fun replace(index: Int, action: RemoteAction): Boolean = edit { profile, actions ->
        QuickActionEditor.replace(profile, actions, index, action, mutableUiState.value.macros)
    }

    fun remove(action: RemoteAction): Boolean = edit { profile, actions ->
        QuickActionEditor.remove(profile, actions, action, mutableUiState.value.macros)
    }

    fun move(fromIndex: Int, toIndex: Int): Boolean = edit { profile, actions ->
        QuickActionEditor.move(profile, actions, fromIndex, toIndex, mutableUiState.value.macros)
    }

    fun moveLeft(action: RemoteAction): Boolean = edit { profile, actions ->
        QuickActionEditor.moveLeft(profile, actions, action, mutableUiState.value.macros)
    }

    fun moveRight(action: RemoteAction): Boolean = edit { profile, actions ->
        QuickActionEditor.moveRight(profile, actions, action, mutableUiState.value.macros)
    }

    fun moveToTop(action: RemoteAction): Boolean = edit { profile, actions ->
        QuickActionEditor.moveToTop(profile, actions, action, mutableUiState.value.macros)
    }

    fun reset(): Boolean = edit { profile, actions ->
        QuickActionEditor.reset(profile, actions)
    }

    fun saveMacro(macro: SavedMacro, pinToRemote: Boolean): Boolean {
        val current = mutableUiState.value
        val profile = current.profile ?: return false
        if (current.isLoading || current.shouldExit || profile.sequenceFor(macro) == null) return false
        val macros = current.macros.toMutableList()
        val existingIndex = macros.indexOfFirst { it.id == macro.id }
        if (existingIndex >= 0) macros[existingIndex] = macro else {
            if (macros.size >= SavedRemote.MAX_MACROS) return false
            macros += macro
        }
        val macroAction = RemoteAction.Macro(macro.id)
        val actions = current.actions.filterNot { it == macroAction }.toMutableList()
        if (pinToRemote && macroAction !in actions && actions.size < SavedRemote.MAX_QUICK_ACTIONS) {
            actions.add(0, macroAction)
        }
        return persist(actions, macros)
    }

    fun deleteMacro(macroId: String): Boolean {
        val current = mutableUiState.value
        if (current.macros.none { it.id == macroId }) return false
        return persist(
            actions = current.actions.filterNot { it == RemoteAction.Macro(macroId) },
            macros = current.macros.filterNot { it.id == macroId },
        )
    }

    fun retry() {
        val current = mutableUiState.value
        if (!current.isDirty || current.isSaving || current.shouldExit || current.profile == null) return
        val snapshot = settingsWithRemoteData(latestReceivedSettings, current.actions, current.macros) ?: return
        latestAcceptedSnapshot = snapshot
        enqueue(snapshot)
    }

    /** Returns false while an accepted edit is not safely persisted. */
    fun requestExit(): Boolean {
        val current = mutableUiState.value
        if (current.shouldExit) return false
        val safe = !current.isDirty && !current.isSaving
        if (!safe) {
            exitRequested = true
            mutableUiState.update { it.copy(exitBlocked = true) }
        }
        return safe
    }

    private fun edit(
        operation: (RemoteProfile, List<RemoteAction>) -> QuickActionEdit,
    ): Boolean {
        val current = mutableUiState.value
        val profile = current.profile ?: return false
        if (current.isLoading || current.shouldExit) return false
        val result = operation(profile, current.actions)
        if (!result.accepted || result.actions == current.actions) return false
        return persist(result.actions, current.macros)
    }

    private fun persist(actions: List<RemoteAction>, macros: List<SavedMacro>): Boolean {
        val current = mutableUiState.value
        val snapshot = settingsWithRemoteData(latestReceivedSettings, actions, macros) ?: return false
        latestAcceptedSnapshot = snapshot
        val updatedRemote = snapshot.savedRemotes.first { remote -> remote.id == targetRemoteId }
        mutableUiState.update {
            it.copy(
                remote = updatedRemote,
                settings = snapshot,
                actions = actions,
                availableActions = current.profile?.let { profile ->
                    QuickActionEditor.availableActions(profile, macros)
                }.orEmpty(),
                isDirty = true,
                isSaving = true,
                saveError = null,
                exitBlocked = false,
            )
        }
        enqueue(snapshot)
        return true
    }

    private fun enqueue(snapshot: RemoteSettings) {
        val request = WriteRequest(
            generation = ++generation,
            actions = mutableUiState.value.actions.toList(),
            macros = mutableUiState.value.macros.toList(),
            projectedSnapshot = snapshot,
            expectedProfileId = requireNotNull(capturedProfileId),
        )
        var workerToStart: Job? = null
        synchronized(writeLock) {
            pendingWrite = request
            if (writeWorker == null) {
                workerToStart = viewModelScope.launch(
                    context = persistenceDispatcher,
                    start = CoroutineStart.LAZY,
                ) { drainWrites() }
                writeWorker = workerToStart
            }
        }
        mutableUiState.update {
            it.copy(isSaving = true, isDirty = true, saveError = null, exitBlocked = false)
        }
        workerToStart?.start()
    }

    private suspend fun drainWrites() {
        while (true) {
            val request = synchronized(writeLock) {
                pendingWrite?.also { pendingWrite = null } ?: run {
                    writeWorker = null
                    null
                }
            } ?: return

            try {
                val committed = preferences.update { current ->
                    settingsWithRemoteData(current, request.actions, request.macros, request.expectedProfileId)
                }
                if (committed == null) {
                    withContext(Dispatchers.Main.immediate) { cancelForExternalDeviceChange() }
                    return
                }
                withContext(Dispatchers.Main.immediate) { publishWriteSuccess(request, committed) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                withContext(Dispatchers.Main.immediate) { publishWriteFailure(request) }
            }
        }
    }

    private fun publishWriteSuccess(request: WriteRequest, committed: RemoteSettings) {
        if (mutableUiState.value.shouldExit) return
        val hasNewer = synchronized(writeLock) { pendingWrite != null } || request.generation < generation
        if (latestReceivedSettings != committed) awaitingEcho = committed
        latestReceivedSettings = committed
        if (!hasNewer && mutableUiState.value.actions == request.actions) {
            latestAcceptedSnapshot = committed
            mutableUiState.update {
                it.copy(
                    remote = committed.savedRemotes.firstOrNull { remote -> remote.id == targetRemoteId },
                    settings = committed,
                    isSaving = false,
                    isDirty = false,
                    saveError = null,
                    exitBlocked = false,
                    shouldExit = exitRequested,
                )
            }
        }
    }

    private fun publishWriteFailure(request: WriteRequest) {
        if (mutableUiState.value.shouldExit) return
        val hasNewer = synchronized(writeLock) { pendingWrite != null } || request.generation < generation
        if (!hasNewer && latestAcceptedSnapshot == request.projectedSnapshot) {
            mutableUiState.update {
                it.copy(isSaving = false, isDirty = true, saveError = SAVE_ERROR)
            }
        }
    }

    private fun reconcileIncomingSettings(settings: RemoteSettings) {
        if (mutableUiState.value.shouldExit) return
        val remote = settings.savedRemotes.firstOrNull { it.id == targetRemoteId }
        if (remote == null || settings.selectedSavedRemoteId != targetRemoteId) {
            cancelForExternalDeviceChange()
            return
        }
        val expectedProfileId = capturedProfileId
        if (expectedProfileId != null && remote.profileId != expectedProfileId) {
            cancelForExternalDeviceChange()
            return
        }
        val profile = catalog.findOrNull(remote.profileId)
        if (profile == null) {
            cancelForExternalDeviceChange()
            return
        }

        capturedProfileId = profile.id
        latestReceivedSettings = settings
        if (awaitingEcho == settings) awaitingEcho = null

        val current = mutableUiState.value
        if (current.isLoading) {
            val normalized = QuickActionEditor.normalize(profile, remote.quickActions, remote.macros)
            mutableUiState.value = CustomizeRemoteUiState(
                isLoading = false,
                remote = remoteWithData(remote, normalized, remote.macros),
                profile = profile,
                settings = settingsWithRemoteData(settings, normalized, remote.macros) ?: settings,
                actions = normalized,
                availableActions = QuickActionEditor.availableActions(profile, remote.macros),
            )
            return
        }

        val protectsLocalOrder = current.isDirty || current.isSaving ||
            current.saveError != null || awaitingEcho != null
        val incomingActions = QuickActionEditor.normalize(profile, remote.quickActions, remote.macros)
        val effectiveActions = if (protectsLocalOrder) current.actions else incomingActions
        val effectiveMacros = if (protectsLocalOrder) current.macros else remote.macros
        val mergedSettings = settingsWithRemoteData(settings, effectiveActions, effectiveMacros) ?: settings
        mutableUiState.update {
            it.copy(
                remote = remoteWithData(remote, effectiveActions, effectiveMacros),
                profile = profile,
                settings = mergedSettings,
                actions = effectiveActions,
                availableActions = QuickActionEditor.availableActions(profile, effectiveMacros),
            )
        }
    }

    private fun cancelForExternalDeviceChange() {
        synchronized(writeLock) {
            pendingWrite = null
            writeWorker?.cancel()
            writeWorker = null
        }
        mutableUiState.update {
            it.copy(isSaving = false, isDirty = false, saveError = null, shouldExit = true)
        }
    }

    private fun settingsWithRemoteData(
        base: RemoteSettings,
        actions: List<RemoteAction>,
        macros: List<SavedMacro>,
    ): RemoteSettings? {
        val expectedProfileId = capturedProfileId ?: return null
        return settingsWithRemoteData(base, actions, macros, expectedProfileId)
    }

    private fun settingsWithRemoteData(
        base: RemoteSettings,
        actions: List<RemoteAction>,
        macros: List<SavedMacro>,
        expectedProfileId: String,
    ): RemoteSettings? {
        if (base.selectedSavedRemoteId != targetRemoteId) return null
        val target = base.savedRemotes.firstOrNull { it.id == targetRemoteId } ?: return null
        if (target.profileId != expectedProfileId) return null
        val replacement = remoteWithData(target, actions, macros)
        return base.copy(
            savedRemotes = base.savedRemotes.map { remote ->
                if (remote.id == targetRemoteId) replacement else remote
            },
        )
    }

    private fun remoteWithData(
        remote: SavedRemote,
        actions: List<RemoteAction>,
        macros: List<SavedMacro>,
    ): SavedRemote =
        SavedRemote(
            id = remote.id,
            name = remote.name,
            profileId = remote.profileId,
            quickActions = actions,
            isConfirmed = remote.isConfirmed,
            macros = macros,
        )

    private companion object {
        const val SAVE_ERROR = "Kaydedilemedi"
    }
}
