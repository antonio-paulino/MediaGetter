package pt.paulinoo.mediagetter.model

data class AudioFormat(
    val id: String,
    val ext: String,
    val bitrate: Int,
    val codec: String,
    val filesize: Long
)
