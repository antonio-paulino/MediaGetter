package pt.paulinoo.mediagetter.model

/** Target audio container/codec for audio-only downloads (ffmpeg conversion). */
enum class AudioOutputFormat(val label: String, val ytdlp: String?) {
    ORIGINAL("Original", null),
    MP3("MP3", "mp3"),
    M4A("M4A (AAC)", "m4a"),
    OPUS("Opus", "opus"),
    FLAC("FLAC", "flac"),
    WAV("WAV", "wav")
}

/**
 * Extra, yt-dlp/ffmpeg-backed options applied to a download. Kept separate from
 * the format selection so the feature set can grow without touching the core flow.
 */
data class DownloadOptions(
    val audioFormat: AudioOutputFormat = AudioOutputFormat.ORIGINAL,
    val sponsorBlock: Boolean = false,
    val embedSubtitles: Boolean = false,
    val subtitleLangs: String = "en",
    val embedChapters: Boolean = false,
    val clipStart: String = "",
    val clipEnd: String = "",
    val outputDir: String? = null
)
