package pt.paulinoo.mediagetter.model

data class SearchResult(
    val title: String,
    val url: String,
    val thumbnail: String?,
    val uploader: String = "",
    val duration: Long = 0
)
