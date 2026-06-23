package pt.paulinoo.mediagetter.model

enum class AppTheme { DARK, LIGHT }

/** Persisted, app-wide preferences. */
data class Settings(
    val theme: AppTheme = AppTheme.DARK,
    val defaultOutputDir: String? = null,
    val defaultAudioFormat: AudioOutputFormat = AudioOutputFormat.ORIGINAL,
    val rateLimit: String = "",        // e.g. "2M"; blank = unlimited
    val jsRuntime: Boolean = false,    // download deno for maximum extraction compatibility
    val sponsorBlockByDefault: Boolean = false,
    val embedSubtitlesByDefault: Boolean = false,
    val concurrentDownloads: Int = 2   // how many queue items run at once (1..4)
)
