package pt.paulinoo.mediagetter.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pt.paulinoo.mediagetter.model.AppTheme
import pt.paulinoo.mediagetter.model.AudioOutputFormat
import pt.paulinoo.mediagetter.model.Settings
import java.io.File

/** Loads and persists [Settings] as JSON in the app's home directory. */
object SettingsManager {

    private val file = File(
        File(System.getProperty("user.home"), ".mediagetter"),
        "settings.json"
    )

    suspend fun load(): Settings = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext Settings()
        runCatching {
            val o = JSONObject(file.readText())
            Settings(
                theme = runCatching { AppTheme.valueOf(o.optString("theme", "DARK")) }
                    .getOrDefault(AppTheme.DARK),
                defaultOutputDir = o.optString("defaultOutputDir").ifBlank { null },
                defaultAudioFormat = runCatching {
                    AudioOutputFormat.valueOf(o.optString("defaultAudioFormat", "ORIGINAL"))
                }.getOrDefault(AudioOutputFormat.ORIGINAL),
                rateLimit = o.optString("rateLimit"),
                jsRuntime = o.optBoolean("jsRuntime", false),
                sponsorBlockByDefault = o.optBoolean("sponsorBlockByDefault", false),
                embedSubtitlesByDefault = o.optBoolean("embedSubtitlesByDefault", false),
                concurrentDownloads = o.optInt("concurrentDownloads", 2).coerceIn(1, 4)
            )
        }.getOrDefault(Settings())
    }

    suspend fun save(settings: Settings) = withContext(Dispatchers.IO) {
        runCatching {
            file.parentFile.mkdirs()
            val o = JSONObject()
                .put("theme", settings.theme.name)
                .put("defaultOutputDir", settings.defaultOutputDir ?: "")
                .put("defaultAudioFormat", settings.defaultAudioFormat.name)
                .put("rateLimit", settings.rateLimit)
                .put("jsRuntime", settings.jsRuntime)
                .put("sponsorBlockByDefault", settings.sponsorBlockByDefault)
                .put("embedSubtitlesByDefault", settings.embedSubtitlesByDefault)
                .put("concurrentDownloads", settings.concurrentDownloads)
            file.writeText(o.toString(2))
        }
        Unit
    }
}
