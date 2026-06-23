package pt.paulinoo.mediagetter.viewmodel

import pt.paulinoo.mediagetter.model.DownloadOptions
import pt.paulinoo.mediagetter.model.DownloadTask
import pt.paulinoo.mediagetter.model.PlaylistInfo
import pt.paulinoo.mediagetter.model.SearchResult
import pt.paulinoo.mediagetter.model.Settings
import pt.paulinoo.mediagetter.model.VideoInfo
import java.io.File

data class MainUiState(
    val query: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val searchResults: List<SearchResult> = emptyList(),
    val selectedUrl: String? = null,
    val videoInfo: VideoInfo? = null,
    val selectedVideoFormat: String? = null,
    val selectedAudioFormat: String? = null,
    val audioOnly: Boolean = false,
    val lastDownloadedFile: File? = null,
    val toolsReady: Boolean = false,
    val options: DownloadOptions = DownloadOptions(),
    val settings: Settings = Settings(),
    val showSettings: Boolean = false,
    val ytDlpVersion: String? = null,
    val updatingYtDlp: Boolean = false,
    val playlist: PlaylistInfo? = null,
    val queue: List<DownloadTask> = emptyList(),
    val showQueue: Boolean = false
) {
    val activeQueueCount: Int get() = queue.count { it.active }
}
