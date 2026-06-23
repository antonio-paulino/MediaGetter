package pt.paulinoo.mediagetter.model

data class VideoInfo(
    val title: String,
    val thumbnail: String,
    val uploader: String,
    val duration: Long,
    val videoFormats: List<VideoFormat>,
    val audioFormats: List<AudioFormat>
)