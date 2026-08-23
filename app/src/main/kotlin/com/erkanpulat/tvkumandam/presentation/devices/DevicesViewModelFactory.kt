package com.erkanpulat.tvkumandam.presentation.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.erkanpulat.tvkumandam.AppContainer

class DevicesViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DevicesViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return DevicesViewModel(
            coordinator = container.transmissionCoordinator,
            catalog = container.profileCatalog,
            preferences = container.preferences,
        ) as T
    }
}
