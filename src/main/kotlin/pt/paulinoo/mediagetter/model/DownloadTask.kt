package pt.paulinoo.mediagetter.model

import java.io.File

enum class TaskStatus(val label: String) {
    QUEUED("Em fila"),
    DOWNLOADING("A descarregar"),
    COMPLETED("Concluído"),
    FAILED("Erro"),
    CANCELLED("Cancelado")
}

/** A single unit of work in the download queue. */
data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val thumbnail: String?,
    val format: String,          // yt-dlp -f value (specific ids or a selector)
    val isVideo: Boolean,
    val options: DownloadOptions,
    val status: TaskStatus = TaskStatus.QUEUED,
    val progress: DownloadProgress? = null,
    val outputFile: File? = null,
    val error: String? = null
) {
    val active: Boolean get() = status == TaskStatus.QUEUED || status == TaskStatus.DOWNLOADING
}
