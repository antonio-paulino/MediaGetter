package pt.paulinoo.mediagetter.service

/**
 * Process-wide knobs that affect every yt-dlp invocation. Set from the current
 * [pt.paulinoo.mediagetter.model.Settings] so the service layer doesn't need the
 * settings threaded through every call.
 */
object YtDlpConfig {
    /** Path to a deno binary for the JS runtime, or null to let yt-dlp decide. */
    @Volatile
    var jsRuntimePath: String? = null

    /** yt-dlp --limit-rate value (e.g. "2M"), or null/blank for unlimited. */
    @Volatile
    var rateLimit: String? = null

    /** Common args derived from the config, prepended to every command. */
    fun commonArgs(): List<String> = buildList {
        jsRuntimePath?.let { add("--js-runtimes"); add("deno:$it") }
        rateLimit?.takeIf { it.isNotBlank() }?.let { add("--limit-rate"); add(it) }
    }
}
