package com.erkanpulat.tvkumandam.presentation.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.erkanpulat.tvkumandam.AppContainer

class RemoteViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RemoteViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }

        @Suppress("UNCHECKED_CAST")
        return RemoteViewModel(
            transmissionCoordinator = container.transmissionCoordinator,
            catalog = container.profileCatalog,
            preferences = container.preferences,
        ) as T
    }
}
