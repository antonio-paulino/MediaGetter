package pt.paulinoo.mediagetter.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pt.paulinoo.mediagetter.model.DownloadTask
import pt.paulinoo.mediagetter.model.TaskStatus

@Composable
fun QueueScreen(
    tasks: List<DownloadTask>,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit,
    onOpen: (DownloadTask) -> Unit,
    onClearFinished: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "A fila está vazia",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val active = tasks.count { it.active }
            Text(
                if (active > 0) "$active em curso" else "${tasks.size} na fila",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val hasFinished = tasks.any { !it.active }
            TextButton(onClick = onClearFinished, enabled = hasFinished) {
                Text("Limpar concluídos")
            }
        }

        Spacer(Modifier.height(4.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onCancel = { onCancel(task.id) },
                    onRetry = { onRetry(task.id) },
                    onRemove = { onRemove(task.id) },
                    onOpen = { onOpen(task) }
                )
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: DownloadTask,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = task.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 96.dp, height = 54.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    StatusChip(task.status)
                }
            }

            if (task.status == TaskStatus.DOWNLOADING) {
                Spacer(Modifier.height(10.dp))
                val percent = task.progress?.percent ?: -1f
                val animated by animateFloatAsState(
                    targetValue = if (percent >= 0) percent / 100f else 0f,
                    label = "p"
                )
                if (percent >= 0) {
                    LinearProgressIndicator(
                        progress = { animated },
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                } else {
                    LinearProgressIndicator(
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                }
                val detail = listOfNotNull(
                    task.progress?.phase?.label,
                    if (percent >= 0) "${percent.toInt()}%" else null,
                    task.progress?.speed?.takeIf { it.isNotBlank() },
                    task.progress?.eta?.takeIf { it.isNotBlank() }?.let { "ETA $it" }
                ).joinToString("  •  ")
                if (detail.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            task.error?.takeIf { task.status == TaskStatus.FAILED }?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (task.status) {
                    TaskStatus.QUEUED, TaskStatus.DOWNLOADING ->
                        TextButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp)); Text("Cancelar")
                        }
                    TaskStatus.COMPLETED -> {
                        if (task.outputFile != null) {
                            TextButton(onClick = onOpen) {
                                Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp)); Text("Abrir")
                            }
                        }
                        TextButton(onClick = onRemove) {
                            Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp)); Text("Remover")
                        }
                    }
                    TaskStatus.FAILED, TaskStatus.CANCELLED -> {
                        TextButton(onClick = onRetry) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp)); Text("Tentar de novo")
                        }
                        TextButton(onClick = onRemove) {
                            Icon(Icons.Default.Delete, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp)); Text("Remover")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: TaskStatus) {
    val (container, content) = when (status) {
        TaskStatus.DOWNLOADING -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
        TaskStatus.COMPLETED -> Color(0xFF1B5E20) to Color(0xFFB9F6CA)
        TaskStatus.FAILED -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh to
            MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = container, shape = RoundedCornerShape(6.dp)) {
        Text(
            status.label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
