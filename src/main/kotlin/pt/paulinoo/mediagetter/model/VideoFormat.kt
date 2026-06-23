package pt.paulinoo.mediagetter.model

data class VideoFormat(
    val id: String,
    val height: Int,
    val ext: String,
    val fps: Int,
    val codec: String,
    val tbr: Double,
    val filesize: Long
) {
    val resolution: String get() = "${height}p"
}
