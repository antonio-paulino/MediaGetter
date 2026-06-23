package pt.paulinoo.mediagetter.model

/** One-click download presets applied against a video's available formats. */
enum class Preset(val label: String) {
    BEST("Melhor"),
    HD1080("1080p"),
    SMALLEST("Mais pequeno"),
    MP3("MP3")
}
