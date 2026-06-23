package pt.paulinoo.mediagetter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pt.paulinoo.mediagetter.model.AudioFormat
import pt.paulinoo.mediagetter.model.AudioOutputFormat
import pt.paulinoo.mediagetter.model.DownloadOptions
import pt.paulinoo.mediagetter.model.Preset
import pt.paulinoo.mediagetter.model.VideoInfo
import pt.paulinoo.mediagetter.util.formatDuration
import pt.paulinoo.mediagetter.util.formatFileSize

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoInfoCard(
    info: VideoInfo,
    selectedVideo: String?,
    selectedAudio: String?,
    audioOnly: Boolean,
    options: DownloadOptions,
    onPresetSelected: (Preset) -> Unit,
    onVideoSelected: (String) -> Unit,
    onAudioSelected: (String) -> Unit,
    onAudioOnlyChanged: (Boolean) -> Unit,
    onOptionsChange: ((DownloadOptions) -> DownloadOptions) -> Unit,
    onPickFolder: () -> Unit,
    onDownload: () -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

        AsyncImage(
            model = info.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = info.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        val subtitle = listOf(
            info.uploader,
            formatDuration(info.duration)
        ).filter { it.isNotBlank() }.joinToString("  •  ")

        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(20.dp))

        SectionLabel("Presets")
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Preset.entries.forEach { preset ->
                AssistChip(
                    onClick = { onPresetSelected(preset) },
                    label = { Text(preset.label) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !audioOnly,
                onClick = { onAudioOnlyChanged(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {},
                label = { Text("Vídeo") }
            )
            SegmentedButton(
                selected = audioOnly,
                onClick = { onAudioOnlyChanged(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {},
                label = { Text("Só áudio") }
            )
        }

        Spacer(Modifier.height(20.dp))

        if (!audioOnly) {
            SectionLabel("Resolução")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                info.videoFormats.forEach { format ->
                    FilterChip(
                        selected = format.id == selectedVideo,
                        onClick = { onVideoSelected(format.id) },
                        label = { Text(format.resolution) }
                    )
                }
            }

            val selectedFmt = info.videoFormats.find { it.id == selectedVideo }
            val detail = selectedFmt?.let {
                listOf(
                    it.ext.uppercase(),
                    if (it.fps > 0) "${it.fps} fps" else "",
                    formatFileSize(it.filesize)
                ).filter { d -> d.isNotBlank() }.joinToString("  •  ")
            }
            if (!detail.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            SectionLabel("Formato de saída")
            Spacer(Modifier.height(10.dp))
            OutputFormatDropdown(
                selected = options.audioFormat,
                enabled = true,
                onSelected = { fmt -> onOptionsChange { it.copy(audioFormat = fmt) } }
            )
        }

        Spacer(Modifier.height(18.dp))

        SectionLabel(if (audioOnly) "Faixa de áudio (origem)" else "Faixa de áudio")
        Spacer(Modifier.height(10.dp))
        AudioDropdown(
            options = info.audioFormats,
            selectedId = selectedAudio,
            enabled = true,
            onSelected = onAudioSelected
        )

        Spacer(Modifier.height(16.dp))

        AdvancedOptions(
            options = options,
            audioOnly = audioOnly,
            enabled = true,
            onOptionsChange = onOptionsChange,
            onPickFolder = onPickFolder
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onDownload,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(
                if (audioOnly) "Descarregar áudio" else "Descarregar vídeo",
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedOptions(
    options: DownloadOptions,
    audioOnly: Boolean,
    enabled: Boolean,
    onOptionsChange: ((DownloadOptions) -> DownloadOptions) -> Unit,
    onPickFolder: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Tune, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                "Opções avançadas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(arrow)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {

                OptionSwitch(
                    title = "Remover patrocínios",
                    subtitle = "SponsorBlock: corta sponsors, intros e outros segmentos",
                    checked = options.sponsorBlock,
                    enabled = enabled,
                    onCheckedChange = { v -> onOptionsChange { it.copy(sponsorBlock = v) } }
                )

                if (!audioOnly) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    OptionSwitch(
                        title = "Embutir capítulos",
                        subtitle = "Marca os capítulos do vídeo no ficheiro",
                        checked = options.embedChapters,
                        enabled = enabled,
                        onCheckedChange = { v -> onOptionsChange { it.copy(embedChapters = v) } }
                    )

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    OptionSwitch(
                        title = "Embutir legendas",
                        subtitle = "Inclui legendas (também auto-geradas)",
                        checked = options.embedSubtitles,
                        enabled = enabled,
                        onCheckedChange = { v -> onOptionsChange { it.copy(embedSubtitles = v) } }
                    )
                    AnimatedVisibility(visible = options.embedSubtitles) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = options.subtitleLangs,
                                onValueChange = { v -> onOptionsChange { it.copy(subtitleLangs = v) } },
                                enabled = enabled,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                label = { Text("Idiomas (ex.: pt,en)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    "Cortar troço (opcional)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Deixa em branco para o vídeo completo. Formato: 0:30 ou 1:05:20",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = options.clipStart,
                        onValueChange = { v -> onOptionsChange { it.copy(clipStart = v) } },
                        enabled = enabled,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("Início") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = options.clipEnd,
                        onValueChange = { v -> onOptionsChange { it.copy(clipEnd = v) } },
                        enabled = enabled,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        label = { Text("Fim") },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    "Pasta de destino",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = options.outputDir ?: "Downloads (predefinido)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onPickFolder,
                        enabled = enabled,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mudar")
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutputFormatDropdown(
    selected: AudioOutputFormat,
    enabled: Boolean,
    onSelected: (AudioOutputFormat) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AudioOutputFormat.entries.forEach { fmt ->
                DropdownMenuItem(
                    text = { Text(fmt.label) },
                    onClick = {
                        onSelected(fmt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioDropdown(
    options: List<AudioFormat>,
    selectedId: String?,
    enabled: Boolean,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.find { it.id == selectedId }

    fun label(f: AudioFormat) = buildString {
        append("${f.ext.uppercase()} • ${f.bitrate}kbps")
        formatFileSize(f.filesize).takeIf { it.isNotBlank() }?.let { append(" • $it") }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected?.let(::label) ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        onSelected(option.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
