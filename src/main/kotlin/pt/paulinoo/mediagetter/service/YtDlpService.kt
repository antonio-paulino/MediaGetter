package pt.paulinoo.mediagetter.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pt.paulinoo.mediagetter.model.AudioFormat
import pt.paulinoo.mediagetter.model.DownloadOptions
import pt.paulinoo.mediagetter.model.DownloadProgress
import pt.paulinoo.mediagetter.model.PlaylistInfo
import pt.paulinoo.mediagetter.model.SearchResult
import pt.paulinoo.mediagetter.model.VideoFormat
import pt.paulinoo.mediagetter.model.VideoInfo
import java.io.File

object YtDlpService {

    /** Marker emitted via --progress-template so we can parse progress lines. */
    private const val PROGRESS_PREFIX = "MGPROGRESS"

    suspend fun getVideoInfo(url: String): VideoInfo = withContext(Dispatchers.IO) {
        val ytDlp = YtDlpManager.ensureInstalled()

        val result = ProcessRunner.run(
            listOf(ytDlp.absolutePath) + YtDlpConfig.commonArgs() +
                listOf("-J", "--no-playlist", url)
        )
        if (result.exitCode != 0) throw RuntimeException(friendlyError(result.output))

        val obj = JSONObject(extractJson(result.output))
        val formats = obj.getJSONArray("formats")

        val videos = mutableListOf<VideoFormat>()
        val audios = mutableListOf<AudioFormat>()

        repeat(formats.length()) { i ->
            val f = formats.getJSONObject(i)
            val vcodec = f.optString("vcodec", "none")
            val acodec = f.optString("acodec", "none")
            val filesize = f.optLong("filesize").takeIf { it > 0 }
                ?: f.optLong("filesize_approx")

            if (vcodec != "none") {
                val height = f.optInt("height")
                if (height > 0) {
                    videos += VideoFormat(
                        id = f.getString("format_id"),
                        height = height,
                        ext = f.optString("ext"),
                        fps = f.optInt("fps"),
                        codec = vcodec,
                        tbr = f.optDouble("tbr", 0.0),
                        filesize = filesize
                    )
                }
            } else if (acodec != "none") {
                val bitrate = f.optInt("abr").takeIf { it > 0 } ?: f.optInt("tbr")
                audios += AudioFormat(
                    id = f.getString("format_id"),
                    ext = f.optString("ext"),
                    bitrate = bitrate,
                    codec = acodec,
                    filesize = filesize
                )
            }
        }

        // One entry per resolution, preferring mp4 then highest bitrate.
        val uniqueVideos = videos
            .groupBy { it.height }
            .map { (_, list) ->
                list.maxWithOrNull(
                    compareBy({ it.ext == "mp4" }, { it.tbr })
                )!!
            }
            .sortedByDescending { it.height }

        val uniqueAudios = audios
            .filter { it.bitrate > 0 }
            .sortedByDescending { it.bitrate }

        VideoInfo(
            title = obj.optString("title"),
            thumbnail = obj.optString("thumbnail"),
            videoFormats = uniqueVideos,
            audioFormats = uniqueAudios,
            uploader = obj.optString("uploader"),
            duration = obj.optLong("duration")
        )
    }

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val ytDlp = YtDlpManager.ensureInstalled()

        val result = ProcessRunner.run(
            listOf(ytDlp.absolutePath) + YtDlpConfig.commonArgs() +
                listOf("--flat-playlist", "-J", "ytsearch20:$query")
        )
        if (result.exitCode != 0) throw RuntimeException(friendlyError(result.output))

        val entries = JSONObject(extractJson(result.output)).getJSONArray("entries")

        buildList {
            repeat(entries.length()) { i ->
                val item = entries.getJSONObject(i)
                val id = item.getString("id")
                add(
                    SearchResult(
                        title = item.optString("title"),
                        url = "https://youtube.com/watch?v=$id",
                        thumbnail = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
                        uploader = item.optString("uploader").ifBlank {
                            item.optString("channel")
                        },
                        duration = item.optLong("duration")
                    )
                )
            }
        }
    }

    suspend fun getPlaylist(url: String): PlaylistInfo = withContext(Dispatchers.IO) {
        val ytDlp = YtDlpManager.ensureInstalled()

        val result = ProcessRunner.run(
            listOf(ytDlp.absolutePath) + YtDlpConfig.commonArgs() +
                listOf("--flat-playlist", "--playlist-end", "300", "-J", url)
        )
        if (result.exitCode != 0) throw RuntimeException(friendlyError(result.output))

        val obj = JSONObject(extractJson(result.output))
        val entriesJson = obj.optJSONArray("entries")
            ?: throw RuntimeException("Este URL não é uma playlist.")

        val entries = buildList {
            repeat(entriesJson.length()) { i ->
                val item = entriesJson.optJSONObject(i) ?: return@repeat
                val id = item.optString("id")
                val rawUrl = item.optString("url")
                val entryUrl = when {
                    rawUrl.startsWith("http") -> rawUrl
                    id.isNotBlank() -> "https://youtube.com/watch?v=$id"
                    else -> return@repeat
                }
                add(
                    SearchResult(
                        title = item.optString("title").ifBlank { id },
                        url = entryUrl,
                        thumbnail = id.takeIf { it.isNotBlank() }
                            ?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" },
                        uploader = item.optString("uploader").ifBlank { item.optString("channel") },
                        duration = item.optLong("duration")
                    )
                )
            }
        }

        PlaylistInfo(
            title = obj.optString("title").ifBlank { "Playlist" },
            entries = entries
        )
    }

    suspend fun runDownload(
        url: String,
        format: String,
        isVideo: Boolean,
        output: File,
        options: DownloadOptions,
        onStart: (Process) -> Unit,
        onProgress: (DownloadProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        val ytDlp = YtDlpManager.ensureInstalled()
        val ffmpeg = FfmpegManager.ensureInstalled()

        val command = buildList {
            add(ytDlp.absolutePath)
            addAll(YtDlpConfig.commonArgs())
            add("--ffmpeg-location"); add(ffmpeg.parentFile.absolutePath)
            add("-f"); add(format)

            if (isVideo) {
                add("--merge-output-format"); add("mp4")
            }

            // Audio extraction / conversion (ffmpeg).
            if (!isVideo && options.audioFormat.ytdlp != null) {
                add("--extract-audio")
                add("--audio-format"); add(options.audioFormat.ytdlp)
                add("--audio-quality"); add("0")
            }

            // Trim to a section (ffmpeg).
            clipSection(options.clipStart, options.clipEnd)?.let {
                add("--download-sections"); add(it)
                add("--force-keyframes-at-cuts")
            }

            // SponsorBlock (removes sponsor/intro/outro/... via ffmpeg).
            if (options.sponsorBlock) {
                add("--sponsorblock-remove"); add("default")
            }

            // Subtitles (video only — embedded into the container).
            if (isVideo && options.embedSubtitles) {
                add("--embed-subs")
                add("--write-auto-subs")
                add("--sub-langs"); add(options.subtitleLangs.ifBlank { "en" })
            }

            if (isVideo && options.embedChapters) add("--embed-chapters")

            add("--embed-metadata")
            add("--embed-thumbnail")
            add("--concurrent-fragments"); add("4")
            add("--newline")
            add("--progress-template")
            add("download:$PROGRESS_PREFIX %(progress._percent_str)s|%(progress._speed_str)s|%(progress._eta_str)s")
            add("-o"); add(output.absolutePath)
            add(url)
        }

        onProgress(DownloadProgress(phase = DownloadProgress.Phase.PREPARING))

        val result = ProcessRunner.run(
            command = command,
            onStart = onStart,
            onLine = { line -> parseProgress(line)?.let(onProgress) }
        )

        // A failed thumbnail-embed post-processor (e.g. opus/webm audio) still
        // leaves a perfectly good media file, so don't treat it as a failure.
        if (result.exitCode != 0 && !isThumbnailEmbedFailure(result.output)) {
            throw RuntimeException(friendlyError(result.output))
        }
    }

    /** Builds a yt-dlp `--download-sections` value, or null if no range is set. */
    private fun clipSection(start: String, end: String): String? {
        val from = start.trim()
        val to = end.trim()
        if (from.isBlank() && to.isBlank()) return null
        return "*${from.ifBlank { "0" }}-${to.ifBlank { "inf" }}"
    }

    private fun isThumbnailEmbedFailure(output: String): Boolean =
        output.contains("thumbnail", ignoreCase = true) &&
            output.contains("Supported filetypes for thumbnail embedding", ignoreCase = true)

    private fun parseProgress(line: String): DownloadProgress? {
        val trimmed = line.trim()
        return when {
            trimmed.startsWith(PROGRESS_PREFIX) -> {
                val parts = trimmed.removePrefix(PROGRESS_PREFIX).trim().split("|")
                val percent = parts.getOrNull(0)
                    ?.replace("%", "")?.trim()?.toFloatOrNull() ?: -1f
                DownloadProgress(
                    percent = percent,
                    speed = parts.getOrNull(1)?.trim().orEmpty(),
                    eta = parts.getOrNull(2)?.trim().orEmpty(),
                    phase = DownloadProgress.Phase.DOWNLOADING
                )
            }
            trimmed.startsWith("[Merger]") ->
                DownloadProgress(phase = DownloadProgress.Phase.MERGING)
            trimmed.startsWith("[ExtractAudio]") ||
                trimmed.startsWith("[EmbedThumbnail]") ||
                trimmed.startsWith("[Metadata]") ||
                trimmed.startsWith("[FixupM4a]") ->
                DownloadProgress(phase = DownloadProgress.Phase.PROCESSING)
            else -> null
        }
    }

    private fun extractJson(output: String): String {
        val start = output.indexOf('{')
        if (start == -1) throw RuntimeException(friendlyError(output))
        return output.substring(start)
    }

    /** Turns raw yt-dlp output into a short, user-facing message. */
    private fun friendlyError(raw: String): String {
        val errorLine = raw.lineSequence()
            .firstOrNull { it.contains("ERROR", ignoreCase = true) }
            ?.substringAfter("ERROR:")
            ?.trim()
        return when {
            errorLine.isNullOrBlank() -> "Ocorreu um erro inesperado. Verifica o URL e a ligação à internet."
            errorLine.contains("Private video") -> "Este vídeo é privado."
            errorLine.contains("Video unavailable") -> "Este vídeo não está disponível."
            errorLine.contains("Unsupported URL") -> "URL não suportado."
            else -> errorLine
        }
    }
}
