package com.erkanpulat.tvkumandam.presentation.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteLayoutTemplate
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.RemoteSection
import com.erkanpulat.tvkumandam.domain.preferences.Handedness
import com.erkanpulat.tvkumandam.presentation.remote.components.AdvancedPanel
import com.erkanpulat.tvkumandam.presentation.remote.components.CommandGrid
import com.erkanpulat.tvkumandam.presentation.remote.components.DirectionPad
import com.erkanpulat.tvkumandam.presentation.remote.components.KumandamTopBar
import com.erkanpulat.tvkumandam.presentation.remote.components.QuickDeck
import com.erkanpulat.tvkumandam.presentation.remote.components.Rocker
import com.erkanpulat.tvkumandam.presentation.remote.components.SectionHeader
import com.erkanpulat.tvkumandam.presentation.remote.components.SelectedDeviceCard
import com.erkanpulat.tvkumandam.presentation.remote.components.TransmissionBanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun RemoteRoute(
    viewModel: RemoteViewModel,
    modifier: Modifier = Modifier,
    onEditQuickActions: () -> Unit = {},
    onTroubleshooting: () -> Unit = {},
    onAddTv: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(viewModel, state.hapticsEnabled) {
        var snackbarJob: Job? = null
        viewModel.events.collect { event ->
            if (state.hapticsEnabled && (event is RemoteUiEvent.ShortcutSent || event is RemoteUiEvent.MacroSent)) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            val message = event.message() ?: return@collect
            snackbarJob?.cancel()
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarJob = launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        RemoteScreen(
            state = state,
            onAction = { action ->
                val accepted = when (action) {
                    is RemoteAction.Command -> viewModel.sendCommand(action.command)
                    is RemoteAction.Macro -> viewModel.sendMacro(action.macroId)
                    is RemoteAction.Shortcut -> viewModel.sendShortcut(action.shortcut)
                }
                if (accepted && state.hapticsEnabled) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
            onCancelTransmission = viewModel::cancelTransmission,
            onEditQuickActions = onEditQuickActions,
            onTroubleshooting = onTroubleshooting,
            onAddTv = onAddTv,
            onSettings = onSettings,
            onAcceptedPressHaptic = {},
            modifier = Modifier.fillMaxSize(),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
fun RemoteScreen(
    state: RemoteUiState,
    onAction: (RemoteAction) -> Unit,
    modifier: Modifier = Modifier,
    onCancelTransmission: () -> Unit = {},
    onEditQuickActions: () -> Unit = {},
    onTroubleshooting: () -> Unit = {},
    onAddTv: () -> Unit = {},
    onSettings: () -> Unit = {},
    onAcceptedPressHaptic: () -> Unit = {},
) {
    val remote = state.selectedRemote
    val profile = state.selectedProfile

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        val horizontalPadding = if (maxWidth <= 340.dp) 12.dp else 18.dp
        val layoutTag = if (wide) "remote_layout_wide" else "remote_layout_compact"
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(layoutTag)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 1040.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                KumandamTopBar(onSettings = onSettings)
                if (state.isLoadingPreferences) {
                    LoadingState()
                } else if (remote == null || profile == null) {
                    NoDeviceState(
                        isIrAvailable = state.isIrAvailable,
                        onAddTv = onAddTv,
                        onTroubleshooting = onTroubleshooting,
                    )
                } else {
                    val controlsEnabled = state.isIrAvailable && !state.isTransmitting
                    SelectedDeviceCard(
                        remote = remote,
                        profile = profile,
                        irAvailable = state.isIrAvailable,
                        enabled = controlsEnabled,
                        onPower = { onAction(RemoteAction.Command(RemoteCommand.POWER)) },
                        onAcceptedPressHaptic = onAcceptedPressHaptic,
                    )
                    if (!state.isIrAvailable) {
                        IrUnavailableCard(onTroubleshooting)
                    }
                    TransmissionBanner(
                        state = state.transmissionState,
                        onCancel = onCancelTransmission,
                    )
                    RemoteSectionList(
                        state = state,
                        profile = profile,
                        wide = wide,
                        enabled = controlsEnabled,
                        onAction = onAction,
                        onEditQuickActions = onEditQuickActions,
                        onAcceptedPressHaptic = onAcceptedPressHaptic,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun RemoteSectionList(
    state: RemoteUiState,
    profile: RemoteProfile,
    wide: Boolean,
    enabled: Boolean,
    onAction: (RemoteAction) -> Unit,
    onEditQuickActions: () -> Unit,
    onAcceptedPressHaptic: () -> Unit,
) {
    var advancedExpanded by remember(profile.id) { mutableStateOf(false) }
    val sections = RemoteActionPresentation.visibleSections(profile)
    if (wide) {
        sections.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                pair.forEach { section ->
                    SectionBlock(
                        section = section,
                        state = state,
                        profile = profile,
                        enabled = enabled,
                        advancedExpanded = advancedExpanded,
                        onAdvancedExpandedChange = { advancedExpanded = it },
                        onAction = onAction,
                        onEditQuickActions = onEditQuickActions,
                        onAcceptedPressHaptic = onAcceptedPressHaptic,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    } else {
        var index = 0
        while (index < sections.size) {
            val section = sections[index]
            val combinesRockers = section == RemoteSection.NAVIGATION &&
                sections.getOrNull(index + 1) == RemoteSection.VOLUME_AND_CHANNEL
            if (combinesRockers) {
                NavigationWithRockers(
                    state = state,
                    profile = profile,
                    enabled = enabled,
                    onAction = onAction,
                    onAcceptedPressHaptic = onAcceptedPressHaptic,
                )
                index += 2
            } else {
                SectionBlock(
                    section = section,
                    state = state,
                    profile = profile,
                    enabled = enabled,
                    advancedExpanded = advancedExpanded,
                    onAdvancedExpandedChange = { advancedExpanded = it },
                    onAction = onAction,
                    onEditQuickActions = onEditQuickActions,
                    onAcceptedPressHaptic = onAcceptedPressHaptic,
                )
                index += 1
            }
        }
    }
}

@Composable
private fun NavigationWithRockers(
    state: RemoteUiState,
    profile: RemoteProfile,
    enabled: Boolean,
    onAction: (RemoteAction) -> Unit,
    onAcceptedPressHaptic: () -> Unit,
) {
    val leftIsVolume = state.handedness == Handedness.RIGHT
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.testTag("remote_section_VOLUME_AND_CHANNEL")) {
                RockerForSide(
                    isVolume = leftIsVolume,
                    profile = profile,
                    enabled = enabled,
                    onAction = onAction,
                    onAcceptedPressHaptic = onAcceptedPressHaptic,
                    modifier = Modifier.testTag("rocker_left"),
                )
            }
            DirectionPad(
                profile = profile,
                enabled = enabled,
                onAction = onAction,
                onAcceptedPressHaptic = onAcceptedPressHaptic,
                modifier = Modifier.testTag("remote_section_NAVIGATION"),
            )
            RockerForSide(
                isVolume = !leftIsVolume,
                profile = profile,
                enabled = enabled,
                onAction = onAction,
                onAcceptedPressHaptic = onAcceptedPressHaptic,
                modifier = Modifier.testTag("rocker_right"),
            )
        }
    }
}

@Composable
private fun RockerForSide(
    isVolume: Boolean,
    profile: RemoteProfile,
    enabled: Boolean,
    onAction: (RemoteAction) -> Unit,
    onAcceptedPressHaptic: () -> Unit,
    modifier: Modifier,
) {
    Rocker(
        title = if (isVolume) "SES" else "KANAL",
        increase = if (isVolume) RemoteCommand.VOLUME_UP else RemoteCommand.CHANNEL_UP,
        decrease = if (isVolume) RemoteCommand.VOLUME_DOWN else RemoteCommand.CHANNEL_DOWN,
        profile = profile,
        enabled = enabled,
        onAction = onAction,
        onAcceptedPressHaptic = onAcceptedPressHaptic,
        modifier = modifier,
    )
}

@Composable
private fun SectionBlock(
    section: RemoteSection,
    state: RemoteUiState,
    profile: RemoteProfile,
    enabled: Boolean,
    advancedExpanded: Boolean,
    onAdvancedExpandedChange: (Boolean) -> Unit,
    onAction: (RemoteAction) -> Unit,
    onEditQuickActions: () -> Unit,
    onAcceptedPressHaptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("remote_section_${section.name}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (section) {
            RemoteSection.QUICK_ACTIONS -> QuickDeck(
                actions = RemoteActionPresentation.quickActions(profile, state.quickActions),
                macros = state.selectedRemote?.macros.orEmpty(),
                enabled = enabled,
                onAction = onAction,
                onEdit = onEditQuickActions,
                onAcceptedPressHaptic = onAcceptedPressHaptic,
            )
            RemoteSection.NAVIGATION -> {
                SectionHeader("Yönler")
                DirectionPad(
                    profile = profile,
                    enabled = enabled,
                    onAction = onAction,
                    onAcceptedPressHaptic = onAcceptedPressHaptic,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            RemoteSection.VOLUME_AND_CHANNEL -> {
                SectionHeader("Ses ve kanal")
                val leftIsVolume = state.handedness == Handedness.RIGHT
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        RockerForSide(
                            leftIsVolume,
                            profile,
                            enabled,
                            onAction,
                            onAcceptedPressHaptic,
                            Modifier.testTag("rocker_left"),
                        )
                        RockerForSide(
                            !leftIsVolume,
                            profile,
                            enabled,
                            onAction,
                            onAcceptedPressHaptic,
                            Modifier.testTag("rocker_right"),
                        )
                    }
                }
            }
            RemoteSection.PRIMARY_CONTROLS -> {
                SectionHeader("Temel tuşlar")
                val commands = if (profile.layout.template == RemoteLayoutTemplate.SMART_MEDIA) {
                    listOf(RemoteCommand.HOME, RemoteCommand.BACK, RemoteCommand.GUIDE, RemoteCommand.MENU)
                } else {
                    listOf(RemoteCommand.BACK, RemoteCommand.MENU, RemoteCommand.HOME, RemoteCommand.GUIDE)
                }
                CommandGrid(commands, profile, enabled, onAction, onAcceptedPressHaptic, columns = 2)
            }
            RemoteSection.NUMERIC_KEYPAD -> {
                SectionHeader("Rakamlar")
                CommandGrid(
                    RemoteActionPresentation.DIGITS,
                    profile,
                    enabled,
                    onAction,
                    onAcceptedPressHaptic,
                    columns = 3,
                )
            }
            RemoteSection.COLOR_AND_TELETEXT -> {
                SectionHeader("Renkler ve teletekst")
                CommandGrid(
                    RemoteActionPresentation.COLOR_AND_TEXT,
                    profile,
                    enabled,
                    onAction,
                    onAcceptedPressHaptic,
                    columns = 4,
                )
            }
            RemoteSection.MEDIA -> {
                SectionHeader("Medya")
                CommandGrid(
                    RemoteActionPresentation.MEDIA,
                    profile,
                    enabled,
                    onAction,
                    onAcceptedPressHaptic,
                    columns = 4,
                )
            }
            RemoteSection.DIRECT_INPUTS -> {
                SectionHeader("Doğrudan girişler")
                CommandGrid(
                    commands = RemoteActionPresentation.verifiedDiscreteCommands(profile),
                    profile = profile,
                    enabled = enabled,
                    onAction = onAction,
                    onAcceptedPressHaptic = onAcceptedPressHaptic,
                    testTagPrefix = "direct_input",
                    columns = 2,
                )
            }
            RemoteSection.ADVANCED -> {
                AdvancedPanel(
                    expanded = advancedExpanded,
                    commands = RemoteActionPresentation.effectiveAdvancedCommands(profile),
                    profile = profile,
                    enabled = enabled,
                    onExpandedChange = onAdvancedExpandedChange,
                    onAction = onAction,
                    onAcceptedPressHaptic = onAcceptedPressHaptic,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Text(
            text = "Kumanda hazırlanıyor…",
            modifier = Modifier.padding(22.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun NoDeviceState(
    isIrAvailable: Boolean,
    onAddTv: () -> Unit,
    onTroubleshooting: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("TV ekleyin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Kumandayı kullanmak için marka ve model seçin.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAddTv, modifier = Modifier.height(52.dp)) { Text("TV ekle") }
            if (!isIrAvailable) {
                Button(onClick = onTroubleshooting, modifier = Modifier.height(52.dp)) {
                    Text("Yardımı aç")
                }
            }
        }
    }
}

@Composable
private fun IrUnavailableCard(onTroubleshooting: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("IR vericisi bulunamadı", fontWeight = FontWeight.Bold)
            Text("Bu uygulama, dahili kızılötesi vericisi olan telefonlarda çalışır.")
            Button(onClick = onTroubleshooting, modifier = Modifier.height(52.dp)) {
                Text("Yardımı aç")
            }
        }
    }
}

private fun RemoteUiEvent.message(): String? = when (this) {
    is RemoteUiEvent.CommandSent, is RemoteUiEvent.ShortcutSent -> null
    is RemoteUiEvent.MacroSent -> "$name tamamlandı."
    RemoteUiEvent.IrUnsupported -> "Bu telefonda IR vericisi bulunamadı."
    RemoteUiEvent.CommandUnavailable -> "Bu komut seçili TV tarafından desteklenmiyor."
    is RemoteUiEvent.TransmissionFailed -> "IR sinyali gönderilemedi. Tekrar deneyin."
}
