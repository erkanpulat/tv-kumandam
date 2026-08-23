package com.erkanpulat.tvkumandam.presentation.devices

import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings

data class DeviceListItem(
    val remote: SavedRemote,
    val profile: RemoteProfile,
    val isSelected: Boolean,
)

enum class FinderStep { BRAND, MODEL, TEST, NAME, EXHAUSTED }

data class ProfileFinderState(
    val step: FinderStep,
    val brands: List<String>,
    val models: List<String> = emptyList(),
    val selectedBrand: String? = null,
    val selectedModel: String? = null,
    val candidateIds: List<String> = emptyList(),
    val candidateIndex: Int = 0,
    val currentProfile: RemoteProfile? = null,
    val testCommand: RemoteCommand = RemoteCommand.POWER,
    val isSending: Boolean = false,
    val awaitingResponse: Boolean = false,
    val error: String? = null,
    val tvName: String = "",
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null,
)

sealed interface DevicesUiEvent {
    data class NavigateRemote(val remoteId: String) : DevicesUiEvent
    data object OpenAddDevice : DevicesUiEvent
}

data class DevicesUiState(
    val isLoading: Boolean = true,
    val settings: RemoteSettings = RemoteSettings(),
    val devices: List<DeviceListItem> = emptyList(),
    val isIrAvailable: Boolean = false,
    val finder: ProfileFinderState? = null,
    val mutationInProgress: Boolean = false,
    val mutationError: String? = null,
    val pendingEvent: DevicesUiEvent? = null,
)
