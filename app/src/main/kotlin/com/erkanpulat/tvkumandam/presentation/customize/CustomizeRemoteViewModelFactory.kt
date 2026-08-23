package com.erkanpulat.tvkumandam.presentation.customize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.erkanpulat.tvkumandam.AppContainer

class CustomizeRemoteViewModelFactory(
    private val targetRemoteId: String,
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CustomizeRemoteViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return CustomizeRemoteViewModel(
            targetRemoteId = targetRemoteId,
            preferences = container.preferences,
            catalog = container.profileCatalog,
        ) as T
    }
}
