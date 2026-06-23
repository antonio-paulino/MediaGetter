package pt.paulinoo.mediagetter.model

/** A playlist/channel and its (flat-extracted) entries. */
data class PlaylistInfo(
    val title: String,
    val entries: List<SearchResult>
)
