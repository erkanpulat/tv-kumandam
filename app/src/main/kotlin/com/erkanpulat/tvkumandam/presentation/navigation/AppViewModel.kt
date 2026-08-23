package com.erkanpulat.tvkumandam.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erkanpulat.tvkumandam.domain.preferences.RemotePreferences
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val isLoading: Boolean = true,
    val destination: AppDestination? = null,
    val settings: RemoteSettings = RemoteSettings(),
)

/** Owns app-level destinations while finder steps remain inside the Devices feature. */
class AppViewModel internal constructor(
    preferences: RemotePreferences,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.settings.collect { settings ->
                mutableUiState.update { current ->
                    val destination = when {
                        settings.savedRemotes.isEmpty() && current.destination == null -> {
                            if (settings.onboardingCompleted) {
                                AppDestination.AddDevice
                            } else {
                                AppDestination.Welcome
                            }
                        }
                        settings.savedRemotes.isEmpty() &&
                            current.destination != AppDestination.Welcome &&
                            current.destination != AppDestination.AddDevice -> AppDestination.AddDevice
                        current.destination == null -> AppDestination.Remote
                        else -> current.destination
                    }
                    current.copy(
                        isLoading = false,
                        destination = destination,
                        settings = settings,
                    )
                }
            }
        }
    }

    fun navigate(destination: AppDestination) {
        if (mutableUiState.value.isLoading) return
        mutableUiState.update { it.copy(destination = destination) }
    }
}
