package com.erkanpulat.tvkumandam.presentation.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.erkanpulat.tvkumandam.domain.model.SavedRemote

@Composable
fun DevicesScreen(
    state: DevicesUiState,
    onAddDevice: () -> Unit,
    onSelectRemote: (String) -> Unit,
    onDeleteRemote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<SavedRemote?>(null) }
    val controlsEnabled = !state.mutationInProgress && state.pendingEvent == null
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("devices_screen")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "TV'ler",
                    modifier = Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Button(
                    onClick = onAddDevice,
                    enabled = controlsEnabled,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("devices_add"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("TV ekle")
                }
            }

            state.mutationError?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Text(error, modifier = Modifier.padding(14.dp))
                }
            }

            if (!state.isLoading && state.devices.isEmpty()) {
                EmptyDevicesCard(onAddDevice)
            } else {
                state.devices.forEach { item ->
                    DeviceCard(
                        item = item,
                        enabled = controlsEnabled,
                        onSelect = { onSelectRemote(item.remote.id) },
                        onDelete = { pendingDelete = item.remote },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    pendingDelete?.let { remote ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("${remote.name} silinsin mi?") },
            text = { Text("Bu TV ve kumanda düzeni silinecek.") },
            confirmButton = {
                Button(onClick = {
                    pendingDelete = null
                    onDeleteRemote(remote.id)
                }) { Text("TV'yi sil") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("İptal") }
            },
        )
    }
}

@Composable
private fun DeviceCard(
    item: DeviceListItem,
    enabled: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("device_card_${item.remote.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.remote.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(item.profile.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                    onClick = onDelete,
                    enabled = enabled,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("device_delete_${item.remote.id}")
                        .semantics {
                            contentDescription = "${item.remote.name} kumandasını sil"
                        },
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                }
            }
            OutlinedButton(
                onClick = onSelect,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("device_select_${item.remote.id}"),
            ) {
                Text(if (item.isSelected) "Kumandaya dön" else "Bu kumandayı aç")
            }
        }
    }
}

@Composable
private fun EmptyDevicesCard(onAddDevice: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Henüz TV eklenmedi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Kumandayı kullanmak için bir TV ekleyin.")
            Button(onClick = onAddDevice, modifier = Modifier.heightIn(min = 48.dp)) { Text("TV ekle") }
        }
    }
}
