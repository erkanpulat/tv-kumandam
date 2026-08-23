package com.erkanpulat.tvkumandam.presentation.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erkanpulat.tvkumandam.domain.model.RemoteCommand

@Composable
fun ProfileFinderScreen(
    state: DevicesUiState,
    onBack: () -> Unit,
    onBrand: (String) -> Unit,
    onModel: (String) -> Unit,
    onUnknownModel: () -> Unit,
    onSendTest: () -> Unit,
    onResponse: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val finder = state.finder
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_finder")
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FinderHeader(onBack)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!state.isIrAvailable) IrUnavailableCard()
                if (finder == null) {
                    Text("Seçenekler hazırlanıyor…")
                } else {
                    when (finder.step) {
                        FinderStep.BRAND -> BrandStep(finder, onBrand)
                        FinderStep.MODEL -> ModelStep(finder, onModel)
                        FinderStep.TEST -> TestStep(
                            finder = finder,
                            irAvailable = state.isIrAvailable,
                            onSendTest = onSendTest,
                            onResponse = onResponse,
                        )
                        FinderStep.NAME -> NameStep(finder, onNameChange, onSave)
                        FinderStep.EXHAUSTED -> ExhaustedStep(finder, onBack)
                    }
                }
            }
            if (finder?.step == FinderStep.MODEL) {
                Button(
                    onClick = onUnknownModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("finder_unknown_model"),
                ) { Text("Modelimi bilmiyorum") }
            }
        }
    }
}

@Composable
private fun FinderHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("finder_header"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = "Önceki adıma dön" },
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
        }
        Column {
            Text(
                "TV ekle",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Text("Marka ve modeli seçin.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun IrUnavailableCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            "Bu telefonda IR vericisi bulunamadı.",
            modifier = Modifier.padding(14.dp),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BrandStep(finder: ProfileFinderState, onBrand: (String) -> Unit) {
    StepCard("Marka") {
        finder.brands.forEach { brand ->
            OutlinedButton(
                onClick = { onBrand(brand) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("finder_brand_$brand"),
            ) { Text(brand) }
        }
    }
}

@Composable
private fun ModelStep(
    finder: ProfileFinderState,
    onModel: (String) -> Unit,
) {
    var query by rememberSaveable(finder.selectedBrand) { mutableStateOf("") }
    val matchingModels = finder.models.filter { model ->
        model.contains(query.trim(), ignoreCase = true)
    }
    StepCard("Model") {
        Text("${finder.selectedBrand} modelleri", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().testTag("finder_model_search"),
            label = { Text("Model adı ara") },
            placeholder = { Text("Ör. 82-507 B") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { query = "" },
                        modifier = Modifier.semantics { contentDescription = "Model aramasını temizle" },
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null)
                    }
                }
            } else {
                null
            },
            singleLine = true,
        )
        if (matchingModels.isEmpty()) {
            Text(
                "Bu aramayla eşleşen model bulunamadı.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        matchingModels.forEach { model ->
            OutlinedButton(
                onClick = { onModel(model) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("finder_model_$model"),
            ) { Text(model) }
        }
    }
}

@Composable
private fun TestStep(
    finder: ProfileFinderState,
    irAvailable: Boolean,
    onSendTest: () -> Unit,
    onResponse: (Boolean) -> Unit,
) {
    val profile = finder.currentProfile ?: return
    StepCard("Kumandayı test et") {
        Text(
            "${testProgress(finder.testCommand)} · ${testTitle(finder.testCommand)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(profile.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Kumanda seçeneği ${finder.candidateIndex + 1}/${finder.candidateIds.size}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Telefonu TV'ye doğrultun ve komutu gönderin.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (finder.isSending) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("IR sinyali gönderiliyor…")
        }
        finder.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onSendTest,
            enabled = irAvailable && !finder.isSending && !finder.awaitingResponse,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("finder_send_${finder.testCommand.name}"),
        ) {
            Text(if (finder.error == null) "${testTitle(finder.testCommand)} komutunu gönder" else "Tekrar dene")
        }
        if (finder.awaitingResponse) {
            Text("TV tepki verdi mi?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(
                onClick = { onResponse(true) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Evet") }
            OutlinedButton(
                onClick = { onResponse(false) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Hayır") }
        }
    }
}

@Composable
private fun NameStep(
    finder: ProfileFinderState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    StepCard("TV adı") {
        OutlinedTextField(
            value = finder.tvName,
            onValueChange = onNameChange,
            label = { Text("TV adı") },
            supportingText = { finder.nameError?.let { Text(it) } },
            isError = finder.nameError != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("finder_name"),
        )
        finder.saveError?.let {
            Text("TV kaydedilemedi. Tekrar deneyin.", color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onSave,
            enabled = !finder.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("finder_save"),
        ) { Text(if (finder.isSaving) "Kaydediliyor…" else if (finder.saveError != null) "Tekrar dene" else "TV'yi kaydet") }
    }
}

@Composable
private fun ExhaustedStep(finder: ProfileFinderState, onBack: () -> Unit) {
    StepCard("Uyumlu kumanda bulunamadı") {
        Text(
            if (finder.selectedModel == null) {
                "Bu marka için uygun tüm kumanda seçenekleri denendi."
            } else {
                "Bu model için uygun tüm kumanda seçenekleri denendi."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Model listesine dönüp başka bir seçim yapabilirsiniz.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Model seçimine dön")
        }
    }
}

@Composable
private fun StepCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

private fun testTitle(command: RemoteCommand): String = when (command) {
    RemoteCommand.POWER -> "Güç"
    RemoteCommand.VOLUME_UP -> "Ses +"
    RemoteCommand.SOURCE -> "Kaynak"
    else -> command.name
}

private fun testProgress(command: RemoteCommand): String = when (command) {
    RemoteCommand.POWER -> "1/2"
    RemoteCommand.VOLUME_UP -> "2/2"
    else -> "Test"
}
