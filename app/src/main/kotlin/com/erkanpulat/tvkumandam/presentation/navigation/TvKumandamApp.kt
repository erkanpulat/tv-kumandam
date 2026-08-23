package com.erkanpulat.tvkumandam.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erkanpulat.tvkumandam.AppContainer
import com.erkanpulat.tvkumandam.domain.preferences.ThemePreference
import com.erkanpulat.tvkumandam.presentation.customize.CustomizeRemoteDestinationOwner
import com.erkanpulat.tvkumandam.presentation.customize.CustomizeRemoteRoute
import com.erkanpulat.tvkumandam.presentation.customize.CustomizeRemoteViewModel
import com.erkanpulat.tvkumandam.presentation.customize.CustomizeRemoteViewModelFactory
import com.erkanpulat.tvkumandam.presentation.devices.DevicesScreen
import com.erkanpulat.tvkumandam.presentation.devices.DevicesUiEvent
import com.erkanpulat.tvkumandam.presentation.devices.DevicesViewModel
import com.erkanpulat.tvkumandam.presentation.devices.FinderStep
import com.erkanpulat.tvkumandam.presentation.devices.ProfileFinderScreen
import com.erkanpulat.tvkumandam.presentation.remote.RemoteRoute
import com.erkanpulat.tvkumandam.presentation.remote.RemoteViewModel
import com.erkanpulat.tvkumandam.presentation.settings.SettingsScreen
import com.erkanpulat.tvkumandam.presentation.settings.SettingsViewModel
import com.erkanpulat.tvkumandam.ui.theme.TvKumandamTheme
import kotlinx.coroutines.flow.first

@Composable
fun TvKumandamApp(
    container: AppContainer,
    appViewModel: AppViewModel,
    remoteViewModel: RemoteViewModel,
    devicesViewModel: DevicesViewModel,
    settingsViewModel: SettingsViewModel,
    customizeOwner: CustomizeRemoteDestinationOwner,
) {
    val appState by appViewModel.uiState.collectAsStateWithLifecycle()
    val devicesState by devicesViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val darkTheme = when (appState.settings.theme) {
        ThemePreference.SYSTEM -> systemDark
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val destination = appState.destination
    val navigator = remember(scope, appViewModel, remoteViewModel, devicesViewModel) {
        AppNavigationCoordinator(
            scope = scope,
            currentDestination = { appViewModel.uiState.value.destination },
            beforeLeave = { source ->
                when (source) {
                    AppDestination.Remote -> remoteViewModel.suspendTransmissionAdmissionAndCancel()
                    AppDestination.AddDevice -> devicesViewModel.cancelFinderAndJoin()
                    AppDestination.Devices -> devicesViewModel.consumeEvent()
                    else -> Unit
                }
            },
            beforeEnter = { target ->
                if (target == AppDestination.Remote) remoteViewModel.resumeTransmissionAdmission()
            },
            commit = appViewModel::navigate,
        )
    }
    val navigateSafely: (AppDestination) -> Unit = { target -> navigator.navigate(target) }

    LaunchedEffect(destination) {
        if (destination == AppDestination.AddDevice && devicesState.finder == null) {
            devicesViewModel.startAddDevice()
        }
    }
    LaunchedEffect(devicesState.pendingEvent) {
        when (val event = devicesState.pendingEvent) {
            is DevicesUiEvent.NavigateRemote -> {
                remoteViewModel.uiState.first { remoteState ->
                    !remoteState.isLoadingPreferences &&
                        remoteState.selectedRemote?.id == event.remoteId
                }
                devicesViewModel.consumeEvent()
                navigateSafely(AppDestination.Remote)
            }
            DevicesUiEvent.OpenAddDevice -> {
                devicesViewModel.consumeEvent()
                navigateSafely(AppDestination.AddDevice)
            }
            null -> Unit
        }
    }

    TvKumandamTheme(darkTheme = darkTheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (destination.isTopLevel()) {
                    AppBottomBar(
                        selected = requireNotNull(destination),
                        onSelect = navigateSafely,
                    )
                }
            },
        ) { contentPadding ->
            when (destination) {
                null -> LoadingApp(Modifier.padding(contentPadding))
                AppDestination.Welcome -> WelcomeScreen(
                    isIrAvailable = devicesState.isIrAvailable,
                    onContinue = { navigateSafely(AppDestination.AddDevice) },
                    modifier = Modifier.padding(contentPadding),
                )
                AppDestination.Remote -> RemoteRoute(
                    viewModel = remoteViewModel,
                    onEditQuickActions = {
                        remoteViewModel.uiState.value.selectedRemote?.id?.let { remoteId ->
                            navigateSafely(AppDestination.Customize(remoteId))
                        }
                    },
                    onTroubleshooting = { navigateSafely(AppDestination.Settings) },
                    onAddTv = { navigateSafely(AppDestination.AddDevice) },
                    onSettings = { navigateSafely(AppDestination.Settings) },
                    modifier = Modifier.padding(contentPadding),
                )
                AppDestination.Devices -> DevicesScreen(
                    state = devicesState,
                    onAddDevice = { navigateSafely(AppDestination.AddDevice) },
                    onSelectRemote = devicesViewModel::selectRemote,
                    onDeleteRemote = devicesViewModel::deleteRemote,
                    modifier = Modifier.padding(contentPadding),
                )
                AppDestination.Settings -> SettingsScreen(
                    state = settingsState,
                    onThemeSelected = settingsViewModel::selectTheme,
                    onHapticsChanged = settingsViewModel::setHapticsEnabled,
                    onHandednessSelected = settingsViewModel::selectHandedness,
                    onRetry = settingsViewModel::retry,
                    modifier = Modifier.padding(contentPadding),
                )
                AppDestination.AddDevice -> AddDeviceDestination(
                    state = devicesState,
                    viewModel = devicesViewModel,
                    hasSavedDevices = appState.settings.savedRemotes.isNotEmpty(),
                    onLeave = { navigateSafely(AppDestination.Devices) },
                    modifier = Modifier.padding(contentPadding),
                )
                is AppDestination.Customize -> {
                    val targetRemoteId = destination.remoteId
                    val factory = remember(container, targetRemoteId) {
                        CustomizeRemoteViewModelFactory(targetRemoteId, container)
                    }
                    val customizeViewModel: CustomizeRemoteViewModel = viewModel(
                        viewModelStoreOwner = customizeOwner,
                        factory = factory,
                    )
                    CustomizeRemoteRoute(
                        viewModel = customizeViewModel,
                        onDone = {
                            customizeOwner.clearDestination()
                            navigateSafely(AppDestination.Remote)
                        },
                        modifier = Modifier.padding(contentPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddDeviceDestination(
    state: com.erkanpulat.tvkumandam.presentation.devices.DevicesUiState,
    viewModel: DevicesViewModel,
    hasSavedDevices: Boolean,
    onLeave: () -> Unit,
    modifier: Modifier,
) {
    val finder = state.finder
    val canHandleBack = finder?.step != FinderStep.BRAND || hasSavedDevices
    BackHandler(enabled = canHandleBack) {
        if (!viewModel.goBackInFinder()) onLeave()
    }
    ProfileFinderScreen(
        state = state,
        onBack = {
            if (!viewModel.goBackInFinder() && hasSavedDevices) onLeave()
        },
        onBrand = viewModel::selectBrand,
        onModel = viewModel::selectModel,
        onUnknownModel = { viewModel.selectModel(null) },
        onSendTest = { viewModel.sendCurrentTest() },
        onResponse = viewModel::respondToCurrentTest,
        onNameChange = viewModel::updateTvName,
        onSave = viewModel::saveCurrentCandidate,
        modifier = modifier,
    )
}

@Composable
fun AppBottomBar(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier.fillMaxWidth()) {
        TOP_LEVEL_ITEMS.forEach { item ->
            NavigationBarItem(
                selected = selected == item.destination,
                onClick = { onSelect(item.destination) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.label) },
                modifier = Modifier
                    .testTag(item.testTag)
                    .heightIn(min = 64.dp)
                    .semantics { contentDescription = item.label },
            )
        }
    }
}

@Composable
private fun LoadingApp(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.testTag("app_loading"))
    }
}

private fun AppDestination?.isTopLevel(): Boolean =
    this == AppDestination.Remote || this == AppDestination.Devices || this == AppDestination.Settings

private data class TopLevelItem(
    val destination: AppDestination,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
)

private val TOP_LEVEL_ITEMS = listOf(
    TopLevelItem(AppDestination.Remote, "Kumanda", Icons.Rounded.Tv, "bottom_remote"),
    TopLevelItem(AppDestination.Devices, "TV'ler", Icons.Rounded.Devices, "bottom_devices"),
    TopLevelItem(AppDestination.Settings, "Ayarlar", Icons.Rounded.Settings, "bottom_settings"),
)
