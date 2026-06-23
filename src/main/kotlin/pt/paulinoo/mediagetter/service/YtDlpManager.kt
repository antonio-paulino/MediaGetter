package pt.paulinoo.mediagetter.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

object YtDlpManager {

    private val appDir =
        File(
            System.getProperty("user.home"),
            ".mediagetter"
        )

    suspend fun ensureInstalled(): File =
        withContext(Dispatchers.IO) {

            val isWindows =
                System.getProperty("os.name")
                    .lowercase()
                    .contains("win")

            val binaryName =
                if (isWindows)
                    "yt-dlp.exe"
                else
                    "yt-dlp"

            val ytDlp =
                File(
                    appDir,
                    binaryName
                )

            if (ytDlp.exists())
                return@withContext ytDlp

            appDir.mkdirs()

            val downloadUrl =
                if (isWindows)
                    "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe"
                else
                    "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"

            URI.create(downloadUrl)
                .toURL()
                .openStream()
                .use { input ->

                    ytDlp.outputStream().use {
                        input.copyTo(it)
                    }
                }

            ytDlp.setExecutable(true)

            ytDlp
        }

    /** Returns the installed yt-dlp version string (e.g. "2026.06.09"), or null. */
    suspend fun version(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val ytDlp = ensureInstalled()
            ProcessRunner.run(listOf(ytDlp.absolutePath, "--version"))
                .output.trim().lineSequence().lastOrNull()?.trim()
        }.getOrNull()
    }

    /** Self-updates yt-dlp to the latest stable release. Returns the result line. */
    suspend fun update(): String = withContext(Dispatchers.IO) {
        val ytDlp = ensureInstalled()
        val result = ProcessRunner.run(listOf(ytDlp.absolutePath, "-U"))
        result.output.trim().lineSequence()
            .lastOrNull { it.isNotBlank() }
            ?.trim()
            ?: "Atualização concluída"
    }
}