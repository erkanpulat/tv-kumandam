package com.erkanpulat.tvkumandam.presentation.customize

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erkanpulat.tvkumandam.domain.model.RemoteAction
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand
import com.erkanpulat.tvkumandam.domain.model.RemoteProfile
import com.erkanpulat.tvkumandam.domain.model.RemoteShortcut
import com.erkanpulat.tvkumandam.domain.model.SavedRemote
import com.erkanpulat.tvkumandam.domain.model.SavedMacro
import com.erkanpulat.tvkumandam.presentation.components.MacroLabel
import com.erkanpulat.tvkumandam.presentation.remote.RemoteActionPresentation

@Composable
fun CustomizeRemoteRoute(
    viewModel: CustomizeRemoteViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val acceptedHaptic = {
        if (state.hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val tryDone = {
        if (viewModel.requestExit()) onDone()
    }

    BackHandler(onBack = tryDone)
    LaunchedEffect(state.shouldExit) {
        if (state.shouldExit) onDone()
    }

    CustomizeRemoteScreen(
        state = state,
        onBack = tryDone,
        onDone = tryDone,
        onAdd = { action -> viewModel.add(action).also { if (it) acceptedHaptic() } },
        onReplace = { index, action ->
            viewModel.replace(index, action).also { if (it) acceptedHaptic() }
        },
        onRemove = { action -> viewModel.remove(action).also { if (it) acceptedHaptic() } },
        onMoveLeft = { action -> viewModel.moveLeft(action).also { if (it) acceptedHaptic() } },
        onMoveRight = { action -> viewModel.moveRight(action).also { if (it) acceptedHaptic() } },
        onMoveToTop = { action -> viewModel.moveToTop(action).also { if (it) acceptedHaptic() } },
        onMove = { from, to -> viewModel.move(from, to).also { if (it) acceptedHaptic() } },
        onReset = { viewModel.reset().also { if (it) acceptedHaptic() } },
        onSaveMacro = { macro, pin -> viewModel.saveMacro(macro, pin).also { if (it) acceptedHaptic() } },
        onDeleteMacro = { macroId -> viewModel.deleteMacro(macroId).also { if (it) acceptedHaptic() } },
        onRetry = viewModel::retry,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun CustomizeRemoteScreen(
    state: CustomizeRemoteUiState,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onAdd: (RemoteAction) -> Unit,
    onReplace: (Int, RemoteAction) -> Unit,
    onRemove: (RemoteAction) -> Unit,
    onMoveLeft: (RemoteAction) -> Unit,
    onMoveRight: (RemoteAction) -> Unit,
    onMoveToTop: (RemoteAction) -> Unit,
    onMove: (Int, Int) -> Unit,
    onReset: () -> Unit,
    onSaveMacro: (SavedMacro, Boolean) -> Unit,
    onDeleteMacro: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var replacementAction by remember { mutableStateOf<RemoteAction?>(null) }
    var editingMacro by remember { mutableStateOf<SavedMacro?>(null) }
    var creatingMacro by remember { mutableStateOf(false) }
    var macroPendingDelete by remember { mutableStateOf<SavedMacro?>(null) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        EditorTopBar(
            remoteName = state.remote?.name ?: "Kumandayı düzenle",
            onBack = onBack,
            onDone = onDone,
        )
        when {
            state.isLoading -> LoadingEditor()
            state.remote == null || state.profile == null -> Text("Bu TV bulunamadı.")
            else -> {
                EditorIntro(state.remote, state.profile)
                SaveStatus(state, onRetry)
                MacroLibrary(
                    macros = state.macros,
                    onCreate = { creatingMacro = true },
                    onEdit = { editingMacro = it },
                    onDelete = { macroPendingDelete = it },
                )
                QuickDeckPreview(
                    actions = state.actions,
                    macros = state.macros,
                    onRemove = onRemove,
                    onMoveLeft = onMoveLeft,
                    onMoveRight = onMoveRight,
                    onMoveToTop = onMoveToTop,
                    onMove = onMove,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Kısayol ekle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() },
                    )
                    TextButton(onClick = onReset, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Varsayılana dön")
                    }
                }
                        ActionLibrary(
                    macros = state.macros,
                    actions = state.availableActions,
                    selectedActions = state.actions,
                    onPick = { action ->
                        if (state.actions.size < SavedRemote.MAX_QUICK_ACTIONS) onAdd(action)
                        else replacementAction = action
                    },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    replacementAction?.let { replacement ->
        ReplaceActionDialog(
            replacement = replacement,
            currentActions = state.actions,
            macros = state.macros,
            onReplace = { index ->
                onReplace(index, replacement)
                replacementAction = null
            },
            onDismiss = { replacementAction = null },
        )
    }

    if ((creatingMacro || editingMacro != null) && state.profile != null) {
        MacroEditorDialog(
            profile = state.profile,
            existing = editingMacro,
            canPin = state.actions.size < SavedRemote.MAX_QUICK_ACTIONS ||
                editingMacro?.let { RemoteAction.Macro(it.id) in state.actions } == true,
            isPinned = editingMacro?.let { RemoteAction.Macro(it.id) in state.actions } == true,
            onSave = { macro, pin ->
                onSaveMacro(macro, pin)
                creatingMacro = false
                editingMacro = null
            },
            onDismiss = {
                creatingMacro = false
                editingMacro = null
            },
        )
    }

    macroPendingDelete?.let { macro ->
        AlertDialog(
            onDismissRequest = { macroPendingDelete = null },
            title = { Text("${macro.name} silinsin mi?") },
            text = { Text("Makro ve bağlı kısayol silinecek.") },
            confirmButton = {
                Button(onClick = { onDeleteMacro(macro.id); macroPendingDelete = null }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { macroPendingDelete = null }) { Text("İptal") }
            },
        )
    }
}

@Composable
private fun MacroLibrary(
    macros: List<SavedMacro>,
    onCreate: () -> Unit,
    onEdit: (SavedMacro) -> Unit,
    onDelete: (SavedMacro) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Makrolar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Birden fazla komutu tek tuşta birleştirin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onCreate,
                enabled = macros.size < SavedRemote.MAX_MACROS,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Yeni makro") }
        }
        if (macros.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Text(
                    "Komut sırasını, tekrar sayısını ve bekleme süresini belirleyin.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            macros.forEach { macro ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            MacroLabel()
                            Text(macro.name, fontWeight = FontWeight.Bold)
                            Text(
                                "${macro.logicalPressCount} komut · ${macro.steps.size} adım",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { onEdit(macro) }) { Text("Düzenle") }
                        TextButton(onClick = { onDelete(macro) }) { Text("Sil") }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorTopBar(
    remoteName: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "Kumandaya geri dön" },
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
        }
        Column(Modifier.weight(1f)) {
            Text(
                "Kumandayı düzenle",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                remoteName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Button(onClick = onDone, modifier = Modifier.heightIn(min = 48.dp)) { Text("Bitti") }
    }
}

@Composable
private fun EditorIntro(remote: SavedRemote, profile: RemoteProfile) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(remote.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                profile.displayName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Kısayolları seçin ve sıralayın.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SaveStatus(state: CustomizeRemoteUiState, onRetry: () -> Unit) {
    when {
        state.saveError != null -> Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_save_error"),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Kaydedilemedi", fontWeight = FontWeight.Bold)
                    Text("Değişiklikler silinmedi. Kaydetmek için tekrar deneyin.")
                }
                Button(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("Tekrar dene") }
            }
        }
        state.isSaving -> Text(
            if (state.exitBlocked) "Kaydediliyor… Tamamlandığında kumandaya dönülecek." else "Kaydediliyor…",
            modifier = Modifier.testTag("quick_save_progress"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.exitBlocked -> Text(
            "Değişiklikler kaydediliyor.",
            modifier = Modifier.testTag("quick_exit_blocked"),
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun QuickDeckPreview(
    actions: List<RemoteAction>,
    macros: List<SavedMacro>,
    onRemove: (RemoteAction) -> Unit,
    onMoveLeft: (RemoteAction) -> Unit,
    onMoveRight: (RemoteAction) -> Unit,
    onMoveToTop: (RemoteAction) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    // Deck order and the words left/right describe physical positions, even in an RTL locale.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Kısayol sırası",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_deck_preview"),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 16.dp),
            ) {
                itemsIndexed(actions, key = { _, action -> action.stableKey() }) { index, action ->
                    EditableActionCard(
                        action = action,
                        macros = macros,
                        index = index,
                        actionCount = actions.size,
                        onRemove = { onRemove(action) },
                        onMoveLeft = { onMoveLeft(action) },
                        onMoveRight = { onMoveRight(action) },
                        onMoveToTop = { onMoveToTop(action) },
                        onMove = onMove,
                    )
                }
                items(SavedRemote.MAX_QUICK_ACTIONS - actions.size) { emptyOffset ->
                    val index = actions.size + emptyOffset
                    Surface(
                        modifier = Modifier
                            .width(220.dp)
                            .heightIn(min = 116.dp)
                            .testTag("quick_deck_empty_$index"),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${index + 1}. boş alan",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableActionCard(
    action: RemoteAction,
    macros: List<SavedMacro>,
    index: Int,
    actionCount: Int,
    onRemove: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onMoveToTop: () -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val copy = RemoteActionPresentation.forAction(action, macros)
    val dragThreshold = with(LocalDensity.current) { 52.dp.toPx() }
    var menuExpanded by remember { mutableStateOf(false) }
    val currentIndex by rememberUpdatedState(index)
    val currentActionCount by rememberUpdatedState(actionCount)
    val currentOnMove by rememberUpdatedState(onMove)
    val accessibilityActions = buildList {
        if (index > 0) {
            add(CustomAccessibilityAction("Sola taşı") { onMoveLeft(); true })
            add(CustomAccessibilityAction("En başa taşı") { onMoveToTop(); true })
        }
        if (index < actionCount - 1) {
            add(CustomAccessibilityAction("Sağa taşı") { onMoveRight(); true })
        }
        add(CustomAccessibilityAction("Gizle") { onRemove(); true })
    }

    Card(
        modifier = Modifier
            .widthIn(min = 220.dp, max = 280.dp)
            .heightIn(min = 116.dp)
            .testTag("quick_deck_position_$index")
            .semantics {
                val type = if (action is RemoteAction.Macro) "Makro, " else ""
                contentDescription = "${index + 1}. sıra, $type${copy.label}. ${copy.description}"
                customActions = accessibilityActions
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "${copy.label} tuşunu sürükle",
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(action, dragThreshold) {
                        var dragDistance = 0f
                        var gestureIndex = currentIndex
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragDistance = 0f
                                gestureIndex = currentIndex
                            },
                            onDragEnd = { dragDistance = 0f },
                            onDragCancel = { dragDistance = 0f },
                            onDrag = { change, amount ->
                                change.consume()
                                dragDistance += amount.x
                                when {
                                    dragDistance <= -dragThreshold && gestureIndex > 0 -> {
                                        currentOnMove(gestureIndex, gestureIndex - 1)
                                        gestureIndex -= 1
                                        dragDistance = 0f
                                    }
                                    dragDistance >= dragThreshold && gestureIndex < currentActionCount - 1 -> {
                                        currentOnMove(gestureIndex, gestureIndex + 1)
                                        gestureIndex += 1
                                        dragDistance = 0f
                                    }
                                }
                            },
                        )
                    },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (action is RemoteAction.Macro) MacroLabel()
                Text(
                    copy.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "${copy.label} işlemleri" },
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (index > 0) {
                        DropdownMenuItem(text = { Text("Sola taşı") }, onClick = {
                            menuExpanded = false; onMoveLeft()
                        })
                        DropdownMenuItem(text = { Text("En başa taşı") }, onClick = {
                            menuExpanded = false; onMoveToTop()
                        })
                    }
                    if (index < actionCount - 1) {
                        DropdownMenuItem(text = { Text("Sağa taşı") }, onClick = {
                            menuExpanded = false; onMoveRight()
                        })
                    }
                    DropdownMenuItem(text = { Text("Gizle") }, onClick = {
                        menuExpanded = false; onRemove()
                    })
                }
            }
        }
    }
}

@Composable
private fun ActionLibrary(
    macros: List<SavedMacro>,
    actions: List<RemoteAction>,
    selectedActions: List<RemoteAction>,
    onPick: (RemoteAction) -> Unit,
) {
    actionGroups(actions).forEach { (title, groupActions) ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            groupActions.forEach { action ->
                val copy = RemoteActionPresentation.forAction(action, macros)
                val selected = action in selectedActions
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (action is RemoteAction.Macro) MacroLabel()
                            Text(copy.label, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onPick(action) },
                            enabled = !selected,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("quick_picker_${action.stableKey()}"),
                        ) {
                            Text(
                                when {
                                    selected -> "Seçili"
                                    selectedActions.size >= SavedRemote.MAX_QUICK_ACTIONS -> "Değiştir"
                                    else -> "Ekle"
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplaceActionDialog(
    replacement: RemoteAction,
    currentActions: List<RemoteAction>,
    macros: List<SavedMacro>,
    onReplace: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val replacementCopy = RemoteActionPresentation.forAction(replacement, macros)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${replacementCopy.label} nereye gelsin?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Değiştirilecek kısayolu seçin.")
                currentActions.forEachIndexed { index, action ->
                    OutlinedButton(
                        onClick = { onReplace(index) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("quick_replace_target_$index"),
                    ) {
                        val prefix = if (action is RemoteAction.Macro) "Makro · " else ""
                        Text("${index + 1}. $prefix${RemoteActionPresentation.forAction(action, macros).label}")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
    )
}

@Composable
private fun LoadingEditor() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Text("Kısayollar hazırlanıyor…", modifier = Modifier.padding(20.dp))
    }
}

private fun actionGroups(actions: List<RemoteAction>): List<Pair<String, List<RemoteAction>>> {
    val groups = linkedMapOf(
        "Makrolar" to mutableListOf<RemoteAction>(),
        "Girişler" to mutableListOf<RemoteAction>(),
        "Gezinme" to mutableListOf(),
        "Medya" to mutableListOf(),
        "Rakamlar" to mutableListOf(),
        "Gelişmiş" to mutableListOf(),
    )
    actions.forEach { action ->
        val command = (action as? RemoteAction.Command)?.command
        val title = when {
            action is RemoteAction.Macro -> "Makrolar"
            action is RemoteAction.Shortcut || command == RemoteCommand.SOURCE || command in HDMI_COMMANDS -> "Girişler"
            command in NAVIGATION_COMMANDS -> "Gezinme"
            command in MEDIA_COMMANDS -> "Medya"
            command in DIGIT_COMMANDS -> "Rakamlar"
            else -> "Gelişmiş"
        }
        groups.getValue(title) += action
    }
    return groups.mapNotNull { (title, values) -> values.takeIf(List<RemoteAction>::isNotEmpty)?.let { title to it } }
}

internal fun RemoteAction.stableKey(): String = when (this) {
    is RemoteAction.Command -> "command_${command.name.lowercase()}"
    is RemoteAction.Macro -> "macro_$macroId"
    is RemoteAction.Shortcut -> "shortcut_${shortcut.name.lowercase()}"
}

private val HDMI_COMMANDS = setOf(
    RemoteCommand.HDMI1,
    RemoteCommand.HDMI2,
    RemoteCommand.HDMI3,
    RemoteCommand.HDMI4,
)
private val NAVIGATION_COMMANDS = setOf(
    RemoteCommand.UP,
    RemoteCommand.DOWN,
    RemoteCommand.LEFT,
    RemoteCommand.RIGHT,
    RemoteCommand.OK,
    RemoteCommand.MENU,
    RemoteCommand.HOME,
    RemoteCommand.BACK,
    RemoteCommand.EXIT,
)
private val MEDIA_COMMANDS = setOf(
    RemoteCommand.PLAY_PAUSE,
    RemoteCommand.PLAY,
    RemoteCommand.PAUSE,
    RemoteCommand.STOP,
    RemoteCommand.PREVIOUS,
    RemoteCommand.NEXT,
    RemoteCommand.REWIND,
    RemoteCommand.FAST_FORWARD,
)
private val DIGIT_COMMANDS = setOf(
    RemoteCommand.DIGIT_0,
    RemoteCommand.DIGIT_1,
    RemoteCommand.DIGIT_2,
    RemoteCommand.DIGIT_3,
    RemoteCommand.DIGIT_4,
    RemoteCommand.DIGIT_5,
    RemoteCommand.DIGIT_6,
    RemoteCommand.DIGIT_7,
    RemoteCommand.DIGIT_8,
    RemoteCommand.DIGIT_9,
)
