package com.erkanpulat.tvkumandam.presentation.settings

import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings

data class SettingsUiState(
    val settings: RemoteSettings = RemoteSettings(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isIrAvailable: Boolean = false,
    val error: String? = null,
)
