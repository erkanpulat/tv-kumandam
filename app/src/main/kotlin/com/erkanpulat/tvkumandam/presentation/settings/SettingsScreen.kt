package com.erkanpulat.tvkumandam.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erkanpulat.tvkumandam.domain.preferences.Handedness
import com.erkanpulat.tvkumandam.domain.preferences.ThemePreference

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeSelected: (ThemePreference) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onHandednessSelected: (Handedness) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controlsEnabled = !state.isLoading && !state.isSaving
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Ayarlar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("settings_loading"),
                )
            }

            state.error?.let { error ->
                ErrorCard(error = error, enabled = !state.isSaving, onRetry = onRetry)
            }
            SettingsCard(title = "Görünüm") {
                Text(
                    "Tema",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ThemePreference.entries.forEach { preference ->
                    RadioPreferenceRow(
                        label = preference.label,
                        selected = state.settings.theme == preference,
                        enabled = controlsEnabled,
                        testTag = "settings_theme_${preference.name.lowercase()}",
                        onClick = { onThemeSelected(preference) },
                    )
                }
            }

            SettingsCard(title = "Kumanda") {
                TogglePreferenceRow(
                    title = "Tuş titreşimi",
                    description = "Tuşlara dokunulduğunda hafif titreşim verir.",
                    checked = state.settings.hapticsEnabled,
                    enabled = controlsEnabled,
                    onCheckedChange = onHapticsChanged,
                )
                HorizontalDivider()
                Text(
                    "Tuş yerleşimi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Ses ve kanal tuşlarını sağa veya sola yerleştirir.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Handedness.entries.forEach { handedness ->
                    RadioPreferenceRow(
                        label = handedness.label,
                        selected = state.settings.handedness == handedness,
                        enabled = controlsEnabled,
                        testTag = "settings_handedness_${handedness.name.lowercase()}",
                        onClick = { onHandednessSelected(handedness) },
                    )
                }
            }

            HardwareCard(isIrAvailable = state.isIrAvailable)
            PrivacyCard()
            Spacer(Modifier.heightIn(min = 8.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            content()
        }
    }
}

@Composable
private fun RadioPreferenceRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag(testTag)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { contentDescription = "$label seçeneği" }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(label, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun TogglePreferenceRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag("settings_haptics")
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics { contentDescription = "Tuş titreşimi" }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun HardwareCard(isIrAvailable: Boolean) {
    SettingsCard(title = "Kızılötesi (IR)") {
        StatusMessage(
            icon = if (isIrAvailable) Icons.Rounded.PhoneAndroid else Icons.Rounded.ErrorOutline,
            message = if (isIrAvailable) {
                "Dahili IR vericisi kullanıma hazır."
            } else {
                "Bu telefonda IR vericisi bulunamadı."
            },
            contentDescription = if (isIrAvailable) "IR vericisi hazır" else "IR vericisi yok",
        )
        Text(
            "Komutlar telefonun kızılötesi vericisi üzerinden gönderilir.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrivacyCard() {
    SettingsCard(title = "Gizlilik") {
        Text(
            "Uygulama internete bağlanmaz; reklam ve analiz içermez.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "TV'ler ve uygulama ayarları yalnızca bu telefonda saklanır.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorCard(
    error: String,
    enabled: Boolean,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_error"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(error)
            Button(
                onClick = onRetry,
                enabled = enabled,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("settings_retry"),
            ) {
                Text("Tekrar dene")
            }
        }
    }
}

@Composable
private fun StatusMessage(
    icon: ImageVector,
    message: String,
    contentDescription: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(message, modifier = Modifier.weight(1f))
    }
}

private val ThemePreference.label: String
    get() = when (this) {
        ThemePreference.SYSTEM -> "Sistem ayarı"
        ThemePreference.LIGHT -> "Açık"
        ThemePreference.DARK -> "Koyu"
    }

private val Handedness.label: String
    get() = when (this) {
        Handedness.RIGHT -> "Sağ el"
        Handedness.LEFT -> "Sol el"
    }
