package com.erkanpulat.tvkumandam.presentation.customize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.SavedMacro
import com.erkanpulat.tvkumandam.domain.model.SavedMacroStep
import com.erkanpulat.tvkumandam.presentation.remote.RemoteActionPresentation
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MacroEditorDialog(
    profile: RemoteProfile,
    existing: SavedMacro?,
    canPin: Boolean,
    isPinned: Boolean,
    onSave: (SavedMacro, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var pinToRemote by remember(existing, canPin, isPinned) {
        mutableStateOf(if (existing == null) canPin else isPinned)
    }
    val steps = remember(existing) { mutableStateListOf<SavedMacroStep>().apply { addAll(existing?.steps.orEmpty()) } }
    var pickerOpen by remember { mutableStateOf(false) }
    val availableCommands = remember(profile) {
        profile.supportedCommands
            .filterNot { it == RemoteCommand.POWER }
            .sortedBy { RemoteActionPresentation.forCommand(it).label }
    }
    val logicalPresses = steps.sumOf(SavedMacroStep::repeatCount)
    val isValid = name.trim().isNotEmpty() && name.trim().length <= SavedMacro.MAX_NAME_LENGTH &&
        steps.isNotEmpty() && logicalPresses <= SavedMacro.MAX_LOGICAL_PRESSES

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (existing == null) "Yeni makro" else "Makroyu düzenle") },
                        navigationIcon = {
                            TextButton(onClick = onDismiss) { Text("İptal") }
                        },
                        actions = {
                            TextButton(
                                enabled = isValid,
                                onClick = {
                                    val macro = SavedMacro(
                                        id = existing?.id ?: "macro_${UUID.randomUUID()}",
                                        name = name.trim(),
                                        steps = steps,
                                    )
                                    onSave(macro, pinToRemote)
                                },
                            ) { Text("Kaydet", fontWeight = FontWeight.Bold) }
                        },
                    )
                },
            ) { padding ->
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            "Komutları çalışacakları sırayla ekleyin.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= SavedMacro.MAX_NAME_LENGTH) name = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Makro adı") },
                            placeholder = { Text("Örn. HDMI 1") },
                            supportingText = { Text("${name.length}/${SavedMacro.MAX_NAME_LENGTH}") },
                            singleLine = true,
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Kısayollara ekle", fontWeight = FontWeight.Bold)
                                Text(
                                    if (canPin) "Kumandanın ilk kısayolu olarak gösterilir."
                                    else "Kısayollar dolu. Kaydettikten sonra düzenleyebilirsiniz.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = pinToRemote,
                                onCheckedChange = { pinToRemote = it },
                                enabled = canPin,
                                modifier = Modifier.semantics { contentDescription = "Kısayollara ekle" },
                            )
                        }
                    }
                    item {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("Komutlar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    "$logicalPresses/${SavedMacro.MAX_LOGICAL_PRESSES} komut",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Box {
                                Button(
                                    onClick = { pickerOpen = true },
                                    enabled = steps.size < SavedMacro.MAX_STEPS,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                    Text("Komut ekle", modifier = Modifier.padding(start = 8.dp))
                                }
                                DropdownMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
                                    availableCommands.forEach { command ->
                                        DropdownMenuItem(
                                            text = { Text(RemoteActionPresentation.forCommand(command).label) },
                                            onClick = {
                                                steps += SavedMacroStep(command)
                                                pickerOpen = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (steps.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                            ) {
                                Text(
                                    "Başlamak için bir komut ekleyin.",
                                    modifier = Modifier.padding(18.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    itemsIndexed(steps, key = { index, step -> "$index-${step.command}" }) { index, step ->
                        MacroStepCard(
                            index = index,
                            step = step,
                            canIncrease = logicalPresses < SavedMacro.MAX_LOGICAL_PRESSES,
                            onChange = { steps[index] = it },
                            onMoveUp = if (index > 0) ({ steps.add(index - 1, steps.removeAt(index)) }) else null,
                            onMoveDown = if (index < steps.lastIndex) ({ steps.add(index + 1, steps.removeAt(index)) }) else null,
                            onDelete = { steps.removeAt(index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroStepCard(
    index: Int,
    step: SavedMacroStep,
    canIncrease: Boolean,
    onChange: (SavedMacroStep) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    var delayMenuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}", style = MaterialTheme.typography.labelLarge)
                Text(
                    RemoteActionPresentation.forCommand(step.command).label,
                    modifier = Modifier.padding(start = 12.dp).weight(1f),
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onMoveUp ?: {}, enabled = onMoveUp != null) {
                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Yukarı taşı")
                }
                IconButton(onClick = onMoveDown ?: {}, enabled = onMoveDown != null) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Aşağı taşı")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Komutu sil")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tekrar")
                    TextButton(
                        onClick = { onChange(step.copy(repeatCount = step.repeatCount - 1)) },
                        enabled = step.repeatCount > 1,
                    ) { Text("−") }
                    Text("${step.repeatCount}×", fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = { onChange(step.copy(repeatCount = step.repeatCount + 1)) },
                        enabled = step.repeatCount < SavedMacroStep.MAX_REPEAT_COUNT && canIncrease,
                    ) { Text("+") }
                }
                Box {
                    OutlinedButton(onClick = { delayMenuOpen = true }) {
                        Text(if (step.delayAfterMillis == 0L) "Bekleme yok" else "${step.delayAfterMillis} ms")
                    }
                    DropdownMenu(expanded = delayMenuOpen, onDismissRequest = { delayMenuOpen = false }) {
                        DELAY_OPTIONS.forEach { delay ->
                            DropdownMenuItem(
                                text = { Text(if (delay == 0L) "Bekleme yok" else "$delay ms bekle") },
                                onClick = {
                                    onChange(step.copy(delayAfterMillis = delay))
                                    delayMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private val DELAY_OPTIONS = listOf(0L, 150L, 300L, 500L, 750L, 1_000L, 1_500L, 2_000L)
