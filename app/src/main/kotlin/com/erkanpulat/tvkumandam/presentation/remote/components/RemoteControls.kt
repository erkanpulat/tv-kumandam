package com.erkanpulat.tvkumandam.presentation.remote.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.SavedMacro
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.presentation.components.MacroLabel
import com.erkanpulat.tvkumandam.presentation.remote.RemoteActionPresentation
import com.erkanpulat.tvkumandam.presentation.remote.TransmissionState

@Composable
fun KumandamTopBar(
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "TV Kumandam",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "Uygulama ayarlarını aç" },
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = null)
        }
    }
}

@Composable
fun SelectedDeviceCard(
    remote: SavedRemote,
    profile: RemoteProfile,
    irAvailable: Boolean,
    enabled: Boolean,
    onPower: () -> Unit,
    onAcceptedPressHaptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("selected_device_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = remote.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        append(profile.displayName)
                        profile.remoteModel?.let { append(" · ").append(it) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IrStatus(irAvailable)
            }
            if (RemoteCommand.POWER in profile.supportedCommands) {
                FilledIconButton(
                    onClick = {
                        onAcceptedPressHaptic()
                        onPower()
                    },
                    enabled = enabled,
                    modifier = Modifier
                        .size(68.dp)
                        .semantics { contentDescription = "Televizyonu aç veya kapat" },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    shape = CircleShape,
                ) {
                    Icon(
                        Icons.Rounded.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun IrStatus(isAvailable: Boolean, modifier: Modifier = Modifier) {
    val color = if (isAvailable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    Row(
        modifier = modifier
            .testTag("ir_status")
            .semantics {
                contentDescription = if (isAvailable) "IR vericisi hazır" else "IR vericisi bulunamadı"
            },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = if (isAvailable) "IR hazır" else "IR kullanılamıyor",
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
fun QuickDeck(
    actions: List<RemoteAction>,
    macros: List<SavedMacro>,
    enabled: Boolean,
    onAction: (RemoteAction) -> Unit,
    onEdit: () -> Unit,
    onAcceptedPressHaptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader("Kısayollar") {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = "Kısayolları düzenle" },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Düzenle")
            }
        }
        if (actions.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    text = "Sık kullandığınız tuşları ve makroları buraya ekleyin.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (actions.first() is RemoteAction.Macro) {
            QuickActionTile(
                action = actions.first(),
                macros = macros,
                enabled = enabled,
                onClick = {
                    onAcceptedPressHaptic()
                    onAction(actions.first())
                },
                modifier = Modifier.fillMaxWidth().testTag("quick_action_0"),
                emphasized = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                actions.drop(1).forEachIndexed { offset, action ->
                    QuickActionTile(
                        action = action,
                        macros = macros,
                        enabled = enabled,
                        onClick = {
                            onAcceptedPressHaptic()
                            onAction(action)
                        },
                        modifier = Modifier.weight(1f).testTag("quick_action_${offset + 1}"),
                        emphasized = false,
                    )
                }
            }
        } else {
            actions.chunked(2).forEachIndexed { rowIndex, rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowActions.forEachIndexed { columnIndex, action ->
                        val index = rowIndex * 2 + columnIndex
                        QuickActionTile(
                            action = action,
                            macros = macros,
                            enabled = enabled,
                            onClick = {
                                onAcceptedPressHaptic()
                                onAction(action)
                            },
                            modifier = Modifier.weight(1f).testTag("quick_action_$index"),
                            emphasized = false,
                        )
                    }
                    if (rowActions.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    action: RemoteAction,
    macros: List<SavedMacro>,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    emphasized: Boolean,
) {
    val copy = RemoteActionPresentation.forAction(action, macros)
    val isMacro = action is RemoteAction.Macro
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(
                minHeight = when {
                    emphasized && isMacro -> 90.dp
                    emphasized -> 82.dp
                    isMacro -> 78.dp
                    else -> 68.dp
                },
            )
            .semantics {
                contentDescription = if (isMacro) "Makro. ${copy.description}" else copy.description
            },
        shape = RoundedCornerShape(if (emphasized) 24.dp else 20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (emphasized) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (emphasized) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        if (emphasized) Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                if (isMacro) MacroLabel()
                Text(
                    copy.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(action.glyph(), style = MaterialTheme.typography.titleLarge)
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isMacro) MacroLabel()
                Text(copy.label, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DirectionPad(
    profile: RemoteProfile,
    enabled: Boolean,
    onAction: (RemoteAction) -> Unit,
    onAcceptedPressHaptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier.widthIn(min = 152.dp, max = 210.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DirectionKey(RemoteCommand.UP, "▲", profile, enabled, onAction, onAcceptedPressHaptic)
            Row(verticalAlignment = Alignment.CenterVertically) {
                DirectionKey(
                    RemoteCommand.LEFT,
                    "◀",
                    profile,
                    enabled,
                    onAction,
                    onAcceptedPressHaptic,
                    Modifier.testTag("dpad_left"),
                )
                DirectionKey(
                    RemoteCommand.OK,
                    "OK",
                    profile,
                    enabled,
                    onAction,
                    onAcceptedPressHaptic,
                    Modifier.testTag("dpad_ok"),
                    isOk = true,
                )
                DirectionKey(
                    RemoteCommand.RIGHT,
                    "▶",
                    profile,
                    enabled,
                    onAction,
                    onAcceptedPressHaptic,
                    Modifier.testTag("dpad_right"),
                )
            }
            DirectionKey(RemoteCommand.DOWN, "▼", profile, enabled, onAction, onAcceptedPressHaptic)
        }
    }
}

@Composable
private fun DirectionKey(
    command: RemoteCommand,
    glyph: String,
    profile: RemoteProfile,
    enabled: Boolean,
    onAction: (RemoteAction) -> Unit,
    onAcceptedPressHaptic: () -> Unit,
    modifier: Modifier = Modifier,
    isOk: Boolean = false,
) {
    if (command !in profile.supportedCommands) return
    val action = RemoteAction.Command(command)
    val copy = RemoteActionPresentation.forCommand(command)
    Button(
        onClick = {
            onAcceptedPressHaptic()
            onAction(action)
        },
        enabled = enabled,
        modifier = modifier
            .size(if (isOk) 68.dp else 56.dp)
            .semantics { contentDescription = copy.description },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (isOk) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(glyph, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun Rocker(
    title: String,
    increase: RemoteCommand,
    decrease: RemoteCommand,
    profile: RemoteProfile,
    enabled: Boolean,
    onAction: (RemoteAction) -> Unit,
    onAcceptedPressHaptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (increase !in profile.supportedCommands && decrease !in profile.supportedCommands) return
    Surface(
        modifier = modifier.widthIn(min = 52.dp, max = 68.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompactCommand(increase, "+", profile, enabled, onAction, onAcceptedPressHaptic)
            Text(
                title,
                modifier = Modifier.padding(vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            CompactCommand(decrease, "−", profile, enabled, onAction, onAcceptedPressHaptic)
        }
    }
}

@Composable
private fun CompactCommand(
    command: RemoteCommand,
    glyph: String,
    profile: RemoteProfile,
    enabled: Boolean,
    onAction: (RemoteAction) -> Unit,
    onAcceptedPressHaptic: () -> Unit,
) {
    if (command !in profile.supportedCommands) return
    val copy = RemoteActionPresentation.forCommand(command)
    Button(
        onClick = {
            onAcceptedPressHaptic()
            onAction(RemoteAction.Command(command))
        },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .semantics { contentDescription = copy.description },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(glyph, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CommandGrid(
    commands: List<RemoteCommand>,
    profile: RemoteProfile,
    enabled: Boolean,
    onAction: (RemoteAction) -> Unit,
    onAcceptedPressHaptic: () -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "command",
    columns: Int = 3,
) {
    val supported = commands.filter { it in profile.supportedCommands }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        supported.chunked(columns).forEach { rowCommands ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCommands.forEach { command ->
                    CommandTile(
                        command = command,
                        enabled = enabled,
                        onAction = onAction,
                        onAcceptedPressHaptic = onAcceptedPressHaptic,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${testTagPrefix}_${command.name}"),
                    )
                }
                repeat(columns - rowCommands.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun CommandTile(
    command: RemoteCommand,
    enabled: Boolean,
    onAction: (RemoteAction) -> Unit,
    onAcceptedPressHaptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = RemoteActionPresentation.forCommand(command)
    OutlinedButton(
        onClick = {
            onAcceptedPressHaptic()
            onAction(RemoteAction.Command(command))
        },
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 56.dp)
            .semantics { contentDescription = copy.description },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Text(copy.label, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AdvancedPanel(
    expanded: Boolean,
    commands: List<RemoteCommand>,
    profile: RemoteProfile,
    enabled: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAction: (RemoteAction) -> Unit,
    onAcceptedPressHaptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .semantics {
                    stateDescription = if (expanded) "Açık" else "Kapalı"
                    contentDescription = if (expanded) {
                        "Daha fazla kumanda tuşunu kapat"
                    } else {
                        "Daha fazla kumanda tuşunu aç"
                    }
                },
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(if (expanded) "Daha az" else "Daha fazla")
        }
        AnimatedVisibility(visible = expanded) {
            CommandGrid(
                commands = commands,
                profile = profile,
                enabled = enabled,
                onAction = onAction,
                onAcceptedPressHaptic = onAcceptedPressHaptic,
                columns = 3,
            )
        }
    }
}

@Composable
fun TransmissionBanner(
    state: TransmissionState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = when (state) {
        is TransmissionState.Macro -> Triple(state.macroName, state.completedSteps, state.totalSteps)
        else -> return
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("${progress.first} gönderiliyor", fontWeight = FontWeight.Bold)
            Text("${progress.second} / ${progress.third} komut")
            Text("Telefonu TV'ye doğrultun", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Durdur")
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        trailing?.invoke()
    }
}

private fun RemoteAction.glyph(): String = when (this) {
    is RemoteAction.Macro -> "▶"
    is RemoteAction.Command -> when (command) {
        RemoteCommand.MUTE -> "◉"
        RemoteCommand.SOURCE -> "↳"
        RemoteCommand.MENU -> "☰"
        else -> "•"
    }
}

private val HDMI_COMMANDS = setOf(
    RemoteCommand.HDMI1,
    RemoteCommand.HDMI2,
    RemoteCommand.HDMI3,
    RemoteCommand.HDMI4,
)
