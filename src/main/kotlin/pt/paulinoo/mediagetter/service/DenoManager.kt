package pt.paulinoo.mediagetter.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream

/**
 * Manages an optional deno binary that yt-dlp can use as its JavaScript runtime
 * for maximum extraction compatibility. Downloaded on demand (it is large).
 */
object DenoManager {

    private val appDir = File(System.getProperty("user.home"), ".mediagetter")
    private val deno = File(appDir, "deno.exe")

    private const val DOWNLOAD_URL =
        "https://github.com/denoland/deno/releases/latest/download/deno-x86_64-pc-windows-msvc.zip"

    fun existing(): File? = deno.takeIf { it.exists() }

    suspend fun ensureInstalled(): File = withContext(Dispatchers.IO) {
        if (deno.exists()) return@withContext deno

        appDir.mkdirs()
        val zipFile = File(appDir, "deno.zip")

        URI.create(DOWNLOAD_URL).toURL().openStream().use { input ->
            zipFile.outputStream().use { input.copyTo(it) }
        }

        ZipInputStream(zipFile.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.endsWith("deno.exe")) {
                    deno.outputStream().use { zip.copyTo(it) }
                }
                entry = zip.nextEntry
            }
        }

        zipFile.delete()
        deno.setExecutable(true)
        deno
    }
}
