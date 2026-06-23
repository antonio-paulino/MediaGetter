package pt.paulinoo.mediagetter.util

/** Human-readable byte size, e.g. 1536 -> "1.5 MB". Returns "" for unknown (<= 0). */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unit = 0
    while (size >= 1024 && unit < units.lastIndex) {
        size /= 1024
        unit++
    }
    return if (unit == 0) "${size.toInt()} ${units[unit]}"
    else "%.1f %s".format(size, units[unit])
}

/** Seconds -> "h:mm:ss" or "m:ss". Returns "" for unknown (<= 0). */
fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return ""
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}
