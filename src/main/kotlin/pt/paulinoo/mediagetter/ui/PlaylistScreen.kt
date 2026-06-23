package pt.paulinoo.mediagetter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pt.paulinoo.mediagetter.model.AudioOutputFormat
import pt.paulinoo.mediagetter.model.PlaylistInfo
import pt.paulinoo.mediagetter.model.SearchResult
import pt.paulinoo.mediagetter.util.formatDuration

private val RESOLUTIONS = listOf<Pair<String, Int?>>(
    "Melhor" to null, "1080p" to 1080, "720p" to 720, "480p" to 480
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlist: PlaylistInfo,
    onEnqueue: (entries: List<SearchResult>, isVideo: Boolean, maxHeight: Int?, audioFormat: AudioOutputFormat) -> Unit
) {
    val selected = remember(playlist) {
        mutableStateListOf<String>().apply { addAll(playlist.entries.map { it.url }) }
    }
    var isVideo by remember { mutableStateOf(true) }
    var maxHeight by remember { mutableStateOf<Int?>(null) }
    var audioFormat by remember { mutableStateOf(AudioOutputFormat.MP3) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(8.dp))

        Text(
            playlist.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "${playlist.entries.size} vídeos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(14.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = isVideo,
                onClick = { isVideo = true },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                icon = {},
                label = { Text("Vídeo") }
            )
            SegmentedButton(
                selected = !isVideo,
                onClick = { isVideo = false },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                icon = {},
                label = { Text("Só áudio") }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (isVideo) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RESOLUTIONS.forEach { (label, height) ->
                    FilterChip(
                        selected = maxHeight == height,
                        onClick = { maxHeight = height },
                        label = { Text(label) }
                    )
                }
            }
        } else {
            AudioFormatChips(selected = audioFormat, onSelected = { audioFormat = it })
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            val allSelected = selected.size == playlist.entries.size
            TextButton(onClick = {
                if (allSelected) selected.clear()
                else {
                    selected.clear()
                    selected.addAll(playlist.entries.map { it.url })
                }
            }) {
                Text(if (allSelected) "Desmarcar todos" else "Selecionar todos")
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${selected.size} selecionados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(playlist.entries, key = { it.url }) { entry ->
                EntryRow(
                    entry = entry,
                    checked = entry.url in selected,
                    onToggle = {
                        if (entry.url in selected) selected.remove(entry.url)
                        else selected.add(entry.url)
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val chosen = playlist.entries.filter { it.url in selected }
                onEnqueue(chosen, isVideo, maxHeight, audioFormat)
            },
            enabled = selected.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Adicionar ${selected.size} à fila", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioFormatChips(
    selected: AudioOutputFormat,
    onSelected: (AudioOutputFormat) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AudioOutputFormat.entries.forEach { fmt ->
            FilterChip(
                selected = selected == fmt,
                onClick = { onSelected(fmt) },
                label = { Text(fmt.label) }
            )
        }
    }
}

@Composable
private fun EntryRow(
    entry: SearchResult,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            AsyncImage(
                model = entry.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 80.dp, height = 45.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = listOf(entry.uploader, formatDuration(entry.duration))
                    .filter { it.isNotBlank() }.joinToString("  •  ")
                if (sub.isNotBlank()) {
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
