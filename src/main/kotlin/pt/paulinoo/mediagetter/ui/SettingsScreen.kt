package pt.paulinoo.mediagetter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pt.paulinoo.mediagetter.model.AppTheme
import pt.paulinoo.mediagetter.model.AudioOutputFormat
import pt.paulinoo.mediagetter.model.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    ytDlpVersion: String?,
    updating: Boolean,
    onChange: ((Settings) -> Settings) -> Unit,
    onPickFolder: () -> Unit,
    onUpdateYtDlp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsCard("Aparência") {
            Text("Tema", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = settings.theme == AppTheme.DARK,
                    onClick = { onChange { it.copy(theme = AppTheme.DARK) } },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = {},
                    label = { Text("Escuro") }
                )
                SegmentedButton(
                    selected = settings.theme == AppTheme.LIGHT,
                    onClick = { onChange { it.copy(theme = AppTheme.LIGHT) } },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = {},
                    label = { Text("Claro") }
                )
            }
        }

        SettingsCard("Downloads") {
            Text("Pasta de destino predefinida", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = settings.defaultOutputDir ?: "Downloads (predefinido)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onPickFolder, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mudar")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Formato de áudio predefinido", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            AudioFormatSelector(
                selected = settings.defaultAudioFormat,
                onSelected = { fmt -> onChange { it.copy(defaultAudioFormat = fmt) } }
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = settings.rateLimit,
                onValueChange = { v -> onChange { it.copy(rateLimit = v) } },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                label = { Text("Limite de velocidade (ex.: 2M, 500K)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Downloads em simultâneo", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                (1..4).forEach { n ->
                    SegmentedButton(
                        selected = settings.concurrentDownloads == n,
                        onClick = { onChange { it.copy(concurrentDownloads = n) } },
                        shape = SegmentedButtonDefaults.itemShape(n - 1, 4),
                        icon = {},
                        label = { Text("$n") }
                    )
                }
            }
        }

        SettingsCard("Predefinições de download") {
            SettingSwitch(
                title = "Remover patrocínios por defeito",
                subtitle = "Aplica SponsorBlock a novos downloads",
                checked = settings.sponsorBlockByDefault,
                onCheckedChange = { v -> onChange { it.copy(sponsorBlockByDefault = v) } }
            )
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            SettingSwitch(
                title = "Embutir legendas por defeito",
                subtitle = "Inclui legendas em novos downloads de vídeo",
                checked = settings.embedSubtitlesByDefault,
                onCheckedChange = { v -> onChange { it.copy(embedSubtitlesByDefault = v) } }
            )
        }

        SettingsCard("Avançado") {
            SettingSwitch(
                title = "Compatibilidade máxima (runtime JS)",
                subtitle = "Descarrega o deno (~100 MB) para extração mais robusta de certos vídeos",
                checked = settings.jsRuntime,
                onCheckedChange = { v -> onChange { it.copy(jsRuntime = v) } }
            )
        }

        SettingsCard("Ferramentas") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("yt-dlp", fontWeight = FontWeight.Medium)
                    Text(
                        text = ytDlpVersion?.let { "Versão $it" } ?: "Versão desconhecida",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onUpdateYtDlp,
                    enabled = !updating,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (updating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("A atualizar")
                    } else {
                        Text("Atualizar")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioFormatSelector(
    selected: AudioOutputFormat,
    onSelected: (AudioOutputFormat) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AudioOutputFormat.entries.forEach { fmt ->
                DropdownMenuItem(
                    text = { Text(fmt.label) },
                    onClick = { onSelected(fmt); expanded = false }
                )
            }
        }
    }
}
