package pt.paulinoo.mediagetter.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.paulinoo.mediagetter.model.AudioOutputFormat
import pt.paulinoo.mediagetter.model.DownloadOptions
import pt.paulinoo.mediagetter.model.DownloadProgress
import pt.paulinoo.mediagetter.model.DownloadTask
import pt.paulinoo.mediagetter.model.Preset
import pt.paulinoo.mediagetter.model.SearchResult
import pt.paulinoo.mediagetter.model.Settings
import pt.paulinoo.mediagetter.model.TaskStatus
import pt.paulinoo.mediagetter.service.DenoManager
import pt.paulinoo.mediagetter.service.FfmpegManager
import pt.paulinoo.mediagetter.service.SettingsManager
import pt.paulinoo.mediagetter.service.YtDlpConfig
import pt.paulinoo.mediagetter.service.YtDlpManager
import pt.paulinoo.mediagetter.service.YtDlpService
import java.awt.Desktop
import java.awt.EventQueue
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JFileChooser

class MainViewModel {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    /** One-shot user-facing messages (e.g. "download complete"). */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val runningProcesses = ConcurrentHashMap<String, Process>()
    private var queueJob: Job? = null

    fun init() {
        scope.launch {
            val settings = SettingsManager.load()
            _state.update { it.copy(settings = settings) }
            applyConfig(settings)
            try {
                YtDlpManager.ensureInstalled()
                FfmpegManager.ensureInstalled()
                _state.update { it.copy(toolsReady = true) }
                val version = YtDlpManager.version()
                _state.update { it.copy(ytDlpVersion = version) }
                if (settings.jsRuntime) ensureJsRuntime()
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "Falha ao preparar as ferramentas: ${e.message}")
                }
            }
        }
    }

    /** Pushes settings-derived knobs into the service layer. */
    private fun applyConfig(settings: Settings) {
        YtDlpConfig.rateLimit = settings.rateLimit
        YtDlpConfig.jsRuntimePath =
            if (settings.jsRuntime) DenoManager.existing()?.absolutePath else null
    }

    private suspend fun ensureJsRuntime() {
        runCatching {
            val deno = DenoManager.ensureInstalled()
            YtDlpConfig.jsRuntimePath = deno.absolutePath
        }
    }

    fun updateQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun submit() {
        val query = state.value.query.trim()
        if (query.isBlank()) return

        if (query.startsWith("http://") || query.startsWith("https://")) {
            if (isPlaylistUrl(query)) loadPlaylist(query) else loadVideoInfo(query)
        } else {
            search(query)
        }
    }

    private fun isPlaylistUrl(url: String): Boolean {
        val u = url.lowercase()
        val isSingleVideo = u.contains("watch?v=") || u.contains("/shorts/") || u.contains("youtu.be/")
        if (isSingleVideo) return false
        return u.contains("list=") || u.contains("/playlist") ||
            u.contains("/channel/") || u.contains("/@") || u.contains("/c/") || u.contains("/user/")
    }

    private fun loadPlaylist(url: String) {
        scope.launch {
            _state.update {
                it.copy(loading = true, error = null, searchResults = emptyList(), videoInfo = null)
            }
            try {
                val playlist = YtDlpService.getPlaylist(url)
                _state.update { it.copy(loading = false, playlist = playlist) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    private fun loadVideoInfo(url: String) {
        scope.launch {
            _state.update {
                it.copy(loading = true, error = null, searchResults = emptyList(), playlist = null)
            }
            try {
                val info = YtDlpService.getVideoInfo(url)
                val bestVideo = info.videoFormats.maxByOrNull { it.height }
                val bestAudio = info.audioFormats.maxByOrNull { it.bitrate }
                val settings = state.value.settings

                _state.update {
                    it.copy(
                        loading = false,
                        videoInfo = info,
                        selectedUrl = url,
                        selectedVideoFormat = bestVideo?.id,
                        selectedAudioFormat = bestAudio?.id,
                        options = DownloadOptions(
                            audioFormat = settings.defaultAudioFormat,
                            outputDir = settings.defaultOutputDir,
                            sponsorBlock = settings.sponsorBlockByDefault,
                            embedSubtitles = settings.embedSubtitlesByDefault
                        )
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    private fun search(query: String) {
        scope.launch {
            _state.update {
                it.copy(loading = true, error = null, videoInfo = null, playlist = null)
            }
            try {
                val results = YtDlpService.search(query)
                _state.update { it.copy(loading = false, searchResults = results) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun selectSearchResult(url: String) {
        _state.update { it.copy(searchResults = emptyList()) }
        loadVideoInfo(url)
    }

    fun clearSelection() {
        _state.update {
            it.copy(videoInfo = null, selectedUrl = null, playlist = null, error = null)
        }
    }

    fun selectVideo(formatId: String) {
        _state.update { it.copy(selectedVideoFormat = formatId) }
    }

    fun selectAudio(formatId: String) {
        _state.update { it.copy(selectedAudioFormat = formatId) }
    }

    fun setAudioOnly(value: Boolean) {
        _state.update { it.copy(audioOnly = value) }
    }

    fun updateOptions(transform: (DownloadOptions) -> DownloadOptions) {
        _state.update { it.copy(options = transform(it.options)) }
    }

    fun pickOutputFolder() {
        scope.launch {
            val dir = chooseDirectory(state.value.options.outputDir)
            if (dir != null) updateOptions { it.copy(outputDir = dir) }
        }
    }

    private fun chooseDirectory(current: String?): String? {
        var result: String? = null
        EventQueue.invokeAndWait {
            val chooser = JFileChooser(current ?: defaultDownloadDir().absolutePath).apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "Escolher pasta de destino"
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                result = chooser.selectedFile.absolutePath
            }
        }
        return result
    }

    private fun defaultDownloadDir() =
        File(System.getProperty("user.home"), "Downloads")

    // --- Settings ---------------------------------------------------------

    fun openSettings() = _state.update { it.copy(showSettings = true) }
    fun closeSettings() = _state.update { it.copy(showSettings = false) }

    fun updateSettings(transform: (Settings) -> Settings) {
        val updated = transform(state.value.settings)
        _state.update { it.copy(settings = updated) }
        applyConfig(updated)
        scope.launch {
            SettingsManager.save(updated)
            if (updated.jsRuntime && DenoManager.existing() == null) ensureJsRuntime()
        }
    }

    fun pickDefaultFolder() {
        scope.launch {
            val dir = chooseDirectory(state.value.settings.defaultOutputDir)
            if (dir != null) updateSettings { it.copy(defaultOutputDir = dir) }
        }
    }

    fun updateYtDlp() {
        scope.launch {
            _state.update { it.copy(updatingYtDlp = true) }
            val message = runCatching { YtDlpManager.update() }
                .getOrElse { "Falha na atualização: ${it.message}" }
            val version = YtDlpManager.version()
            _state.update { it.copy(updatingYtDlp = false, ytDlpVersion = version) }
            _messages.emit(message)
        }
    }

    // --- Presets ----------------------------------------------------------

    fun applyPreset(preset: Preset) {
        val info = state.value.videoInfo ?: return
        val videos = info.videoFormats
        val audios = info.audioFormats
        val bestAudio = audios.maxByOrNull { it.bitrate }?.id

        when (preset) {
            Preset.BEST -> _state.update {
                it.copy(
                    audioOnly = false,
                    selectedVideoFormat = videos.maxByOrNull { v -> v.height }?.id,
                    selectedAudioFormat = bestAudio,
                    options = it.options.copy(audioFormat = AudioOutputFormat.ORIGINAL)
                )
            }
            Preset.HD1080 -> _state.update {
                val target = videos.filter { v -> v.height <= 1080 }.maxByOrNull { v -> v.height }
                    ?: videos.minByOrNull { v -> v.height }
                it.copy(
                    audioOnly = false,
                    selectedVideoFormat = target?.id,
                    selectedAudioFormat = bestAudio
                )
            }
            Preset.SMALLEST -> _state.update {
                it.copy(
                    audioOnly = false,
                    selectedVideoFormat = videos.minByOrNull { v -> v.height }?.id,
                    selectedAudioFormat = audios.minByOrNull { a -> a.bitrate }?.id ?: bestAudio
                )
            }
            Preset.MP3 -> _state.update {
                it.copy(
                    audioOnly = true,
                    selectedAudioFormat = bestAudio,
                    options = it.options.copy(audioFormat = AudioOutputFormat.MP3)
                )
            }
        }
    }

    // --- Download queue ---------------------------------------------------

    /** Enqueues the currently selected single video/audio download. */
    fun download() {
        val s = state.value
        val url = s.selectedUrl ?: return
        val audioId = s.selectedAudioFormat ?: return
        val info = s.videoInfo

        val format = if (s.audioOnly) audioId
        else "${s.selectedVideoFormat ?: return}+$audioId"

        enqueue(
            listOf(
                DownloadTask(
                    id = newId(),
                    url = url,
                    title = info?.title ?: "media",
                    thumbnail = info?.thumbnail,
                    format = format,
                    isVideo = !s.audioOnly,
                    options = s.options
                )
            )
        )
        openQueue()
    }

    /** Enqueues selected playlist entries with a shared format selector. */
    fun enqueuePlaylist(
        entries: List<SearchResult>,
        isVideo: Boolean,
        maxHeight: Int?,
        audioFormat: AudioOutputFormat
    ) {
        if (entries.isEmpty()) return
        val settings = state.value.settings
        val options = DownloadOptions(
            audioFormat = if (isVideo) AudioOutputFormat.ORIGINAL else audioFormat,
            outputDir = settings.defaultOutputDir,
            sponsorBlock = settings.sponsorBlockByDefault,
            embedSubtitles = isVideo && settings.embedSubtitlesByDefault
        )
        val format = if (isVideo) videoSelector(maxHeight) else "ba/b"

        val tasks = entries.map { e ->
            DownloadTask(
                id = newId(),
                url = e.url,
                title = e.title,
                thumbnail = e.thumbnail,
                format = format,
                isVideo = isVideo,
                options = options
            )
        }
        enqueue(tasks)
        _state.update { it.copy(playlist = null, showQueue = true) }
    }

    private fun videoSelector(maxHeight: Int?): String =
        if (maxHeight == null) "bv*+ba/b"
        else "bv*[height<=$maxHeight]+ba/b[height<=$maxHeight]/b[height<=$maxHeight]/b"

    private fun enqueue(tasks: List<DownloadTask>) {
        _state.update { it.copy(queue = it.queue + tasks) }
        ensureQueueRunning()
    }

    private fun ensureQueueRunning() {
        if (queueJob?.isActive == true) return
        queueJob = scope.launch {
            while (true) {
                val limit = state.value.settings.concurrentDownloads.coerceIn(1, 4)
                val running = state.value.queue.count { it.status == TaskStatus.DOWNLOADING }
                val next = state.value.queue.firstOrNull { it.status == TaskStatus.QUEUED }
                when {
                    next != null && running < limit -> {
                        // Claim the task synchronously so the next iteration won't re-pick it.
                        updateTask(next.id) {
                            it.copy(status = TaskStatus.DOWNLOADING, progress = DownloadProgress(), error = null)
                        }
                        scope.launch { runTask(next) }
                    }
                    next == null && running == 0 -> break
                    else -> delay(150)
                }
            }
        }
    }

    private suspend fun runTask(task: DownloadTask) {
        val dir = task.options.outputDir?.let { File(it) } ?: defaultDownloadDir()
        dir.mkdirs()
        val safeTitle = task.title
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .ifBlank { "media" }
        val output = File(dir, "$safeTitle.%(ext)s")

        try {
            YtDlpService.runDownload(
                url = task.url,
                format = task.format,
                isVideo = task.isVideo,
                output = output,
                options = task.options,
                onStart = { runningProcesses[task.id] = it },
                onProgress = { p -> updateTask(task.id) { it.copy(progress = p) } }
            )
            val file = findDownloadedFile(dir, safeTitle)
            updateTask(task.id) {
                it.copy(status = TaskStatus.COMPLETED, progress = null, outputFile = file)
            }
            _state.update { it.copy(lastDownloadedFile = file) }
            _messages.emit("Download concluído: ${file?.name ?: safeTitle}")
        } catch (e: Exception) {
            val wasCancelled = state.value.queue
                .firstOrNull { it.id == task.id }?.status == TaskStatus.CANCELLED
            if (wasCancelled) {
                updateTask(task.id) { it.copy(progress = null) }
            } else {
                updateTask(task.id) {
                    it.copy(status = TaskStatus.FAILED, progress = null, error = e.message)
                }
            }
        } finally {
            runningProcesses.remove(task.id)
            // Remove leftover thumbnail/temp files when the format couldn't embed them.
            cleanupArtifacts(dir, safeTitle)
        }
    }

    private fun cleanupArtifacts(folder: File, baseName: String) {
        folder.listFiles { f ->
            f.nameWithoutExtension == baseName &&
                f.extension.lowercase() in ARTIFACT_EXTENSIONS
        }?.forEach { runCatching { it.delete() } }
    }

    fun cancelTask(id: String) {
        val task = state.value.queue.firstOrNull { it.id == id } ?: return
        when (task.status) {
            TaskStatus.DOWNLOADING -> {
                updateTask(id) { it.copy(status = TaskStatus.CANCELLED) }
                runningProcesses.remove(id)?.destroyForcibly()
            }
            TaskStatus.QUEUED -> updateTask(id) { it.copy(status = TaskStatus.CANCELLED) }
            else -> Unit
        }
    }

    fun retryTask(id: String) {
        updateTask(id) {
            if (it.status == TaskStatus.FAILED || it.status == TaskStatus.CANCELLED)
                it.copy(status = TaskStatus.QUEUED, error = null, progress = null)
            else it
        }
        ensureQueueRunning()
    }

    fun removeTask(id: String) {
        _state.update {
            it.copy(queue = it.queue.filterNot { t -> t.id == id && t.status != TaskStatus.DOWNLOADING })
        }
    }

    fun clearFinished() {
        _state.update { it.copy(queue = it.queue.filter { t -> t.active }) }
    }

    fun openTaskFile(task: DownloadTask) {
        task.outputFile?.let { openInDesktop(it) }
    }

    fun openQueue() = _state.update { it.copy(showQueue = true) }
    fun closeQueue() = _state.update { it.copy(showQueue = false) }

    private fun updateTask(id: String, transform: (DownloadTask) -> DownloadTask) {
        _state.update { s ->
            s.copy(queue = s.queue.map { if (it.id == id) transform(it) else it })
        }
    }

    private fun newId() = java.util.UUID.randomUUID().toString()

    fun openDownloadsFolder() {
        val folder = state.value.lastDownloadedFile?.parentFile
            ?: File(System.getProperty("user.home"), "Downloads")
        openInDesktop(folder)
    }

    fun openLastFile() {
        state.value.lastDownloadedFile?.let { openInDesktop(it) }
    }

    private fun findDownloadedFile(folder: File, baseName: String): File? =
        folder.listFiles { f ->
            f.nameWithoutExtension == baseName &&
                f.extension.lowercase() !in NON_MEDIA_EXTENSIONS
        }?.maxByOrNull { it.lastModified() }

    private fun openInDesktop(file: File) {
        if (!Desktop.isDesktopSupported()) return
        scope.launch {
            runCatching { Desktop.getDesktop().open(file) }
        }
    }

    private companion object {
        // Thumbnail/temp artefacts yt-dlp may leave next to the real download.
        val NON_MEDIA_EXTENSIONS = setOf(
            "webp", "jpg", "jpeg", "png", "part", "ytdl", "temp", "vtt", "srt"
        )

        // Leftovers to delete after a download (e.g. an un-embeddable thumbnail).
        val ARTIFACT_EXTENSIONS = setOf(
            "webp", "jpg", "jpeg", "png", "part", "ytdl", "temp"
        )
    }
}
