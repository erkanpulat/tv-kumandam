package com.erkanpulat.tvkumandam.presentation.remote

import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutSpec
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.preferences.Handedness
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings

sealed interface TransmissionState {
    data object Idle : TransmissionState

    data class Command(
        val savedRemoteId: String,
        val profileId: String,
        val command: RemoteCommand,
    ) : TransmissionState

    data class Macro(
        val savedRemoteId: String,
        val profileId: String,
        val macroId: String,
        val macroName: String,
        val completedSteps: Int,
        val totalSteps: Int,
    ) : TransmissionState {
        init {
            require(totalSteps > 0) { "A macro requires at least one logical step." }
            require(completedSteps in 0..totalSteps) { "Macro progress is out of range." }
        }
    }
}

data class RemoteUiState(
    val profiles: List<RemoteProfile>,
    val selectedProfileId: String,
    val selectedRemote: SavedRemote?,
    val settings: RemoteSettings,
    val isIrAvailable: Boolean,
    val isLoadingPreferences: Boolean = false,
    val transmissionState: TransmissionState = TransmissionState.Idle,
) {
    val selectedProfile: RemoteProfile?
        get() = selectedRemote?.let { remote -> profiles.firstOrNull { it.id == remote.profileId } }

    val quickActions: List<RemoteAction>
        get() = selectedRemote?.quickActions.orEmpty()

    val layout: RemoteLayoutSpec?
        get() = selectedProfile?.layout

    val hapticsEnabled: Boolean
        get() = settings.hapticsEnabled

    val handedness: Handedness
        get() = settings.handedness

    val isTransmitting: Boolean
        get() = transmissionState != TransmissionState.Idle

    fun macroNamed(id: String) = selectedRemote?.macros?.firstOrNull { it.id == id }

    companion object {
        fun fromSettings(
            profiles: List<RemoteProfile>,
            settings: RemoteSettings,
            isIrAvailable: Boolean,
            transmissionState: TransmissionState = TransmissionState.Idle,
            fallbackProfileId: String = profiles.first().id,
        ): RemoteUiState {
            val selectedRemote = settings.selectedRemote?.takeIf { remote ->
                profiles.any { profile -> profile.id == remote.profileId }
            }
            return RemoteUiState(
                profiles = profiles,
                selectedProfileId = selectedRemote?.profileId ?: fallbackProfileId,
                selectedRemote = selectedRemote,
                settings = settings,
                isIrAvailable = isIrAvailable,
                isLoadingPreferences = false,
                transmissionState = transmissionState,
            )
        }
    }
}

sealed interface RemoteUiEvent {
    data class CommandSent(val command: RemoteCommand) : RemoteUiEvent
    data class MacroSent(val name: String) : RemoteUiEvent
    data object IrUnsupported : RemoteUiEvent
    data object CommandUnavailable : RemoteUiEvent
    data class TransmissionFailed(val detail: String) : RemoteUiEvent
}
