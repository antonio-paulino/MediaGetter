package pt.paulinoo.mediagetter.model

/**
 * Snapshot of an ongoing download, emitted as yt-dlp reports progress.
 *
 * [percent] is in the range 0..100, or -1 when the phase has no measurable
 * progress (e.g. while merging or post-processing).
 */
data class DownloadProgress(
    val percent: Float = -1f,
    val speed: String = "",
    val eta: String = "",
    val phase: Phase = Phase.PREPARING
) {
    enum class Phase(val label: String) {
        PREPARING("A preparar"),
        DOWNLOADING("A descarregar"),
        MERGING("A juntar faixas"),
        PROCESSING("A finalizar"),
        DONE("Concluído")
    }
}
