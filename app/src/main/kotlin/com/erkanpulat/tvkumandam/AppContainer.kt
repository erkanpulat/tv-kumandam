package com.erkanpulat.tvkumandam

import android.content.Context
import com.erkanpulat.tvkumandam.data.ir.ConsumerIrTransmitter
import com.erkanpulat.tvkumandam.data.preferences.DataStoreRemotePreferences
import com.erkanpulat.tvkumandam.data.remote.RemoteProfileCatalog
import com.erkanpulat.tvkumandam.domain.preferences.RemotePreferences
import com.erkanpulat.tvkumandam.domain.remote.RemoteController
import com.erkanpulat.tvkumandam.domain.remote.RemoteTransmissionCoordinator

class AppContainer(context: Context) {
    val profileCatalog = RemoteProfileCatalog()
    val preferences: RemotePreferences = DataStoreRemotePreferences(context, profileCatalog)
    private val remoteController = RemoteController(ConsumerIrTransmitter(context))
    val transmissionCoordinator = RemoteTransmissionCoordinator(remoteController)
}
