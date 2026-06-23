package pt.paulinoo.mediagetter.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream

object FfmpegManager {

    private val appDir =
        File(
            System.getProperty("user.home"),
            ".mediagetter"
        )

    suspend fun ensureInstalled(): File =
        withContext(Dispatchers.IO) {

            val ffmpeg =
                File(appDir, "ffmpeg.exe")

            if (ffmpeg.exists())
                return@withContext ffmpeg

            appDir.mkdirs()

            val zipFile =
                File(appDir, "ffmpeg.zip")

            val downloadUrl =
                "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip"

            URI.create(downloadUrl).toURL().openStream().use { input ->
                zipFile.outputStream().use {
                    input.copyTo(it)
                }
            }

            ZipInputStream(
                zipFile.inputStream()
            ).use { zip ->

                var entry = zip.nextEntry

                while (entry != null) {

                    if (
                        entry.name.endsWith("/ffmpeg.exe")
                    ) {

                        ffmpeg.outputStream().use {
                            zip.copyTo(it)
                        }
                    }

                    if (
                        entry.name.endsWith("/ffprobe.exe")
                    ) {

                        File(
                            appDir,
                            "ffprobe.exe"
                        ).outputStream().use {
                            zip.copyTo(it)
                        }
                    }

                    entry = zip.nextEntry
                }
            }

            zipFile.delete()

            ffmpeg.setExecutable(true)

            ffmpeg
        }
}