package com.erkanpulat.tvkumandam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erkanpulat.tvkumandam.presentation.customize.CustomizeRemoteDestinationOwner
import com.erkanpulat.tvkumandam.presentation.devices.DevicesViewModel
import com.erkanpulat.tvkumandam.presentation.devices.DevicesViewModelFactory
import com.erkanpulat.tvkumandam.presentation.navigation.AppViewModel
import com.erkanpulat.tvkumandam.presentation.navigation.AppViewModelFactory
import com.erkanpulat.tvkumandam.presentation.navigation.TvKumandamApp
import com.erkanpulat.tvkumandam.presentation.remote.RemoteViewModel
import com.erkanpulat.tvkumandam.presentation.remote.RemoteViewModelFactory
import com.erkanpulat.tvkumandam.presentation.settings.SettingsViewModel
import com.erkanpulat.tvkumandam.presentation.settings.SettingsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val container = (application as TvKumandamApplication).appContainer
            val remoteFactory = remember(container) { RemoteViewModelFactory(container) }
            val devicesFactory = remember(container) { DevicesViewModelFactory(container) }
            val appFactory = remember(container) { AppViewModelFactory(container) }
            val settingsFactory = remember(container) { SettingsViewModelFactory(container) }
            val remoteViewModel: RemoteViewModel = viewModel(factory = remoteFactory)
            val devicesViewModel: DevicesViewModel = viewModel(factory = devicesFactory)
            val appViewModel: AppViewModel = viewModel(factory = appFactory)
            val settingsViewModel: SettingsViewModel = viewModel(factory = settingsFactory)
            val customizeOwner: CustomizeRemoteDestinationOwner = viewModel()
            TvKumandamApp(
                container,
                appViewModel,
                remoteViewModel,
                devicesViewModel,
                settingsViewModel,
                customizeOwner,
            )
        }
    }
}
