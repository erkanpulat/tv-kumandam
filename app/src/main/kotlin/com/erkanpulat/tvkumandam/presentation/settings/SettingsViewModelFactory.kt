package com.erkanpulat.tvkumandam.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.erkanpulat.tvkumandam.AppContainer

class SettingsViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(
            preferences = container.preferences,
            isIrAvailable = container.transmissionCoordinator.isAvailable,
        ) as T
    }
}
