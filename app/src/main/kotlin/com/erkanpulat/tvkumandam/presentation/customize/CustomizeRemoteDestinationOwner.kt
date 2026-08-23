package com.erkanpulat.tvkumandam.presentation.customize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Gives the temporary full-screen editor its own lifecycle without adding a navigation library.
 * The Activity retains this owner across configuration changes, while leaving the editor clears
 * every editor-scoped ViewModel and its preferences collector immediately.
 */
class CustomizeRemoteDestinationOwner : ViewModel(), ViewModelStoreOwner {
    private var destinationStore = ViewModelStore()

    override val viewModelStore: ViewModelStore
        get() = destinationStore

    fun clearDestination() {
        destinationStore.clear()
        destinationStore = ViewModelStore()
    }

    override fun onCleared() {
        destinationStore.clear()
    }
}
