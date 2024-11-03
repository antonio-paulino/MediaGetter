package pt.paulinoo.mediagetter.pt.paulinoo.mediagetter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import coil3.compose.AsyncImage
import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback
import com.github.kiulian.downloader.downloader.request.RequestSearchResult
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo
import com.github.kiulian.downloader.model.search.*
import com.github.kiulian.downloader.model.search.field.*
import com.github.kiulian.downloader.model.videos.formats.Format
import kotlinx.coroutines.*
import pt.paulinoo.mediagetter.pt.paulinoo.mediagetter.ui.theme.MediaGetterTheme
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.JFileChooser
import javax.swing.SwingUtilities


private val logger = Logger.getLogger("FFmpegLogger")


fun selectSaveFile(videoTitle: String, defaultExtension: String = ".mp4"): File? {
    var selectedFile: File? = null

    // Use SwingUtilities to ensure the file chooser runs on the Event Dispatch Thread
    SwingUtilities.invokeAndWait {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Save File"
            // Set the default file name with extension
            selectedFile = File("$videoTitle$defaultExtension")
            setSelectedFile(selectedFile) // Autofill the selected file in the dialog

            // Set file filters
            addChoosableFileFilter(object : javax.swing.filechooser.FileFilter() {
                override fun accept(f: File?): Boolean {
                    return f != null && (f.isDirectory || f.extension in listOf("mp4", "mkv"))
                }

                override fun getDescription(): String = "Video Files (*.mp4, *.mkv)"
            })
            addChoosableFileFilter(object : javax.swing.filechooser.FileFilter() {
                override fun accept(f: File?): Boolean {
                    return f != null && (f.isDirectory || f.extension in listOf("mp3", "wav"))
                }

                override fun getDescription(): String = "Audio Files (*.mp3, *.wav)"
            })
            addChoosableFileFilter(object : javax.swing.filechooser.FileFilter() {
                override fun accept(f: File?): Boolean = true // Accept all files
                override fun getDescription(): String = "All Files (*.*)"
            })
        }

        // Show the save dialog and capture the selected file
        val userSelection = fileChooser.showSaveDialog(null)
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            val chosenFile = fileChooser.selectedFile
            // Ensure the selected file has the correct extension
            selectedFile = if (!chosenFile.name.endsWith(defaultExtension)) {
                File("${chosenFile.absolutePath}$defaultExtension") // Append the extension if missing
            } else {
                chosenFile // Return the file as is
            }
        }
    }

    return selectedFile
}


@Composable
fun App() {
    var mode by remember { mutableStateOf("Search") }
    var query by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var job: Job? by remember { mutableStateOf(null) }
    var videoTitle by remember { mutableStateOf<String?>(null) }
    var videoFormats by remember { mutableStateOf(listOf<String>()) }
    var audioFormats by remember { mutableStateOf(listOf<String>()) }
    var thumbnailUrl by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    MediaGetterTheme {
        Scaffold(
            backgroundColor = MaterialTheme.colors.background,
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,

                ) {
                Text("YouTube Video Downloader", style = MaterialTheme.typography.h5)

                Spacer(Modifier.height(16.dp))

                // Mode Toggle: Search or URL
                Row {
                    Button(onClick = { mode = "Search" }, modifier = Modifier.weight(1f)) {
                        Text("Search YouTube")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { mode = "URL" }, modifier = Modifier.weight(1f)) {
                        Text("Enter Video URL")
                    }
                }

                Spacer(Modifier.height(16.dp))

                when (mode) {
                    "Search" -> SearchSection(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {
                            coroutineScope.launch {
                                isLoading = true
                                searchResults = searchYouTube(query)
                                isLoading = false
                            }
                        },
                        searchResults = searchResults,
                        onResultClick = { resultUrl ->
                            url = resultUrl
                            mode = "URL"
                        },
                        isLoading = isLoading // Pass loading state
                    )

                    "URL" -> UrlDownloadSection(
                        url = url,
                        onUrlChange = { url = it },
                        isLoading = isLoading,
                        isDownloading = isDownloading,
                        videoFormats = videoFormats,
                        audioFormats = audioFormats,
                        thumbnailUrl = thumbnailUrl,
                        videoTitle = videoTitle,
                        startDownload = { downloadType, videoFormat, audioFormat ->
                            coroutineScope.launch {
                                isDownloading = true
                                job = downloadVideo(url, downloadType, videoFormat, audioFormat)
                                isDownloading = false
                            }
                        },
                        cancelDownload = {
                            job?.cancel()
                            isDownloading = false
                        }
                    )
                }

                // Fetch formats only when in URL mode and URL is valid
                if (mode == "URL" && url.isNotBlank()) {
                    coroutineScope.launch {
                        fetchFormats(url) { videoFormatsResult, audioFormatsResult, thumbnail, title ->
                            videoFormats = videoFormatsResult
                            audioFormats = audioFormatsResult
                            thumbnailUrl = thumbnail
                            videoTitle = title
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    searchResults: List<SearchResultItem>,
    onResultClick: (String) -> Unit,
    isLoading: Boolean // New parameter to indicate loading state
) {
    Column {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            placeholder = { Text("Search YouTube") },
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onSearch, enabled = !isLoading) {
            Text("Search")
        }
        if (isLoading) {
            CircularProgressIndicator() // Show loading spinner
        }
        LazyColumn {
            items(searchResults) { result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { onResultClick("https://www.youtube.com/watch?v=${result.asVideo().videoId()}") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = fetchThumbnail(result.asVideo().videoId()),
                        contentDescription = "Video Thumbnail",
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Text(
                        result.asVideo().title(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.body1
                    )
                }
            }
        }
    }
}



@Composable
fun UrlDownloadSection(
    url: String,
    onUrlChange: (String) -> Unit,
    isLoading: Boolean,
    isDownloading: Boolean,
    videoFormats: List<String>,
    audioFormats: List<String>,
    thumbnailUrl: String?,
    videoTitle: String?,
    startDownload: (DownloadType, String, String) -> Unit,
    cancelDownload: () -> Unit,
) {
    var selectedVideoFormat by remember { mutableStateOf("") }
    var selectedAudioFormat by remember { mutableStateOf("") }
    Column {
        TextField(
            value = url,
            onValueChange = onUrlChange,
            placeholder = { Text("Paste YouTube URL here") },
            isError = !isValidUrl(url),
            modifier = Modifier.fillMaxWidth()
        )
        if (!isValidUrl(url)) {
            Text("Invalid URL. Please enter a valid YouTube link.", color = MaterialTheme.colors.error)
        }

        Spacer(Modifier.height(8.dp))


        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {

            thumbnailUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            videoTitle?.let {
                Text(it, style = MaterialTheme.typography.h6, modifier = Modifier.padding(8.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

            DropdownMenuComponent(
                label = "Select Video Format",
                selectedOption = selectedVideoFormat,
                options = videoFormats,
                onOptionSelected = { selectedVideoFormat = it }
            )

            Spacer(Modifier.height(8.dp))

            DropdownMenuComponent(
                label = "Select Audio Format",
                selectedOption = selectedAudioFormat,
                options = audioFormats,
                onOptionSelected = { selectedAudioFormat = it }
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = { startDownload(DownloadType.BOTH, selectedVideoFormat, selectedAudioFormat) },
                enabled = isValidUrl(url) && selectedVideoFormat.isNotBlank() && selectedAudioFormat.isNotBlank() && !isDownloading
            ) {
                Text("Download Both")
            }
            Button(
                onClick = { startDownload(DownloadType.AUDIO, selectedVideoFormat, selectedAudioFormat) },
                enabled = isValidUrl(url) && selectedAudioFormat.isNotBlank() && !isDownloading
            ) {
                Text("Download Audio")
            }
        }

        if (isDownloading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Downloading...")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = cancelDownload) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}


// Helper function to validate URL
fun isValidUrl(url: String): Boolean {
    return url.startsWith("https://www.youtube.com/watch") || url.startsWith("https://youtu.be/")
}

enum class DownloadType {
    AUDIO,
    BOTH
}


suspend fun searchYouTube(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
    val downloader = YoutubeDownloader()
    val request = RequestSearchResult(query)
        .type(TypeField.VIDEO) // Search only for video results
        .sortBy(SortField.RELEVANCE) // Sort by relevance
        .forceExactQuery(true) // Ensure the search focuses on the exact query


    val result = downloader.search(request).data()
    result.items()
}

suspend fun fetchFormats(
    url: String,
    onResult: (List<String>, List<String>, String?, String?) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            // Extract the video ID from the URL
            val videoId = Regex("""(?:v=|/)([0-9A-Za-z_-]{11}).*""")
                .find(url)?.groupValues?.get(1)
                ?: throw IllegalArgumentException("Invalid URL: Unable to extract video ID")

            val downloader = YoutubeDownloader()
            val request = RequestVideoInfo(videoId)
            val response = downloader.getVideoInfo(request)

            // Check if video data is available
            val video = response.data() ?: throw Exception("Video data could not be retrieved")

            // Extract video and audio formats
            val videoFormats = video.videoFormats().filter { it.isAdaptive }
            val audioFormats = video.audioFormats()

            // Fetch the thumbnail URL using the fetchThumbnail function
            val thumbnailUrl = fetchThumbnail(videoId)

            val videoTitle = video.details().title()

            // Pass the formats and thumbnail URL to the callback
            onResult(
                videoFormats.map { "${it.qualityLabel()} - ${it.extension().value()} - ${it.bitrate()} kbps" },
                audioFormats.map { "${it.extension().value()} - ${it.bitrate()} kbps" },
                thumbnailUrl, // Return the thumbnail URL from fetchThumbnail
                videoTitle
            )
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error fetching formats", e)
            // Handle error case, possibly return empty lists
            onResult(emptyList(), emptyList(), null, null) // Call the result with empty lists on error
        }
    }
}


suspend fun handleDownload(
    url: String,
    selectedResolution: String?,
    selectedAudioFormat: String?,
    progressState: Float,
    downloadType: DownloadType,
    onResult: (Result<File>) -> Unit
) = withContext(Dispatchers.IO) {
    val tempDir = Files.createTempDirectory("youtube_downloader").toFile()
    val videoId = url.substringAfter("v=")
    val downloader = YoutubeDownloader()

    try {
        val request = RequestVideoInfo(videoId)
        val response = downloader.getVideoInfo(request)
        val video = response.data() ?: throw Exception("Video info not found")
        val videoTitle = video.details().title()

        var videoFile: File? = null
        var audioFile: File? = null

        if (downloadType == DownloadType.AUDIO) {
            if (selectedAudioFormat != null) {
                val audioFormat = video.audioFormats().find {
                    "${it.extension().value()} - ${it.bitrate()} kbps" == selectedAudioFormat
                } ?: throw Exception("No audio format found")

                audioFile = downloadFile(downloader, audioFormat, tempDir, "${videoTitle}_audio") { progressState }
            }

            if (audioFile != null) {
                val finalFile = selectSaveFile(videoTitle, ".mp3")
                if (finalFile != null) {
                    audioFile.copyTo(File(finalFile.parent, "${videoTitle}.mp3"), overwrite = true)
                    onResult(Result.success(File(finalFile.parent, "${videoTitle}.mp3")))
                } else {
                    onResult(Result.failure(Exception("Save file not selected")))
                }
            } else {
                onResult(Result.failure(Exception("No audio file downloaded")))
            }

        } else if (downloadType == DownloadType.BOTH) {
            if (selectedResolution != null) {
                val videoFormat = video.videoFormats().find {
                    "${it.qualityLabel()} - ${
                        it.extension().value()
                    } - ${it.bitrate()} kbps" == selectedResolution && it.isAdaptive
                } ?: throw Exception("No video format found for the selected resolution")

                videoFile = downloadFile(downloader, videoFormat, tempDir, "${videoTitle}_video") { progressState }
            }

            if (selectedAudioFormat != null) {
                val audioFormat = video.audioFormats().find {
                    "${it.extension().value()} - ${it.bitrate()} kbps" == selectedAudioFormat
                } ?: throw Exception("No audio format found")

                audioFile = downloadFile(downloader, audioFormat, tempDir, "${videoTitle}_audio") { progressState }
            }

            if (videoFile != null && audioFile != null) {
                val mergedFile = mergeVideoAndAudio(videoFile, audioFile, tempDir, videoTitle)
                val finalFile = selectSaveFile(videoTitle, ".mp4")
                if (finalFile != null) {
                    mergedFile.copyTo(File(finalFile.parent, "${videoTitle}.mp4"), overwrite = true)
                    onResult(Result.success(File(finalFile.parent, "${videoTitle}.mp4")))
                } else {
                    onResult(Result.failure(Exception("Save file not selected")))
                }
            } else {
                onResult(Result.failure(Exception("No files were downloaded")))
            }
        }
    } catch (e: Exception) {
        onResult(Result.failure(Exception("Error during download: ${e.message}", e)))
    } finally {
        tempDir.deleteRecursively()
    }
}


suspend fun downloadFile(
    downloader: YoutubeDownloader,
    format: Format,
    downloadDir: File,
    fileName: String,
    updateProgress: (Float) -> Unit
): File = withContext(Dispatchers.IO) {
    downloader.downloadVideoFile(
        RequestVideoFileDownload(format)
            .saveTo(downloadDir)
            .renameTo(fileName)
            .overwriteIfExists(true)
            .callback(object : YoutubeProgressCallback<File> {
                override fun onFinished(file: File) {
                    logger.log(Level.INFO, "Download finished: ${file.absolutePath}")
                }

                override fun onError(throwable: Throwable) {
                    logger.log(Level.SEVERE, "Error during download: ${throwable.message}", throwable)
                }

                override fun onDownloading(progress: Int) {
                    // You can log progress if needed, but it might be verbose.
                    logger.log(Level.INFO, "Downloading... ${progress}%")
                    updateProgress(progress / 100f)
                }
            })
    ).data()
}


suspend fun mergeVideoAndAudio(videoFile: File, audioFile: File, outputDir: File, videoTitle: String): File = withContext(Dispatchers.IO) {
        val outputFile = File(outputDir, "${videoTitle}.mp4")
        val command = arrayOf(
            "ffmpeg",
            "-i",
            videoFile.absolutePath,
            "-i",
            audioFile.absolutePath,
            "-c:v",
            "copy",
            "-c:a",
            "copy",
            "-strict",
            "experimental",
            outputFile.absolutePath
        )
        val process = ProcessBuilder(*command).start()

        val outputReader = BufferedReader(InputStreamReader(process.inputStream))
        val errorReader = BufferedReader(InputStreamReader(process.errorStream))

        var line: String?
        while (outputReader.readLine().also { line = it } != null) {
            logger.log(Level.INFO, line)
        }
        while (errorReader.readLine().also { line = it } != null) {
            logger.log(Level.SEVERE, line)
        }

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw RuntimeException("ffmpeg process failed with exit code $exitCode")
        }

        videoFile.delete()
        audioFile.delete()
        logger.log(Level.INFO, "Merge completed: ${outputFile.absolutePath}")
        outputFile
    }






@Composable
fun DropdownMenuComponent(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) {
            Text(if (selectedOption.isNotEmpty()) selectedOption else label)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(onClick = {
                    onOptionSelected(option) // Notify parent of the selected option
                    expanded = false // Close the menu
                }) {
                    Text(option)
                }
            }
        }
    }
}


fun fetchThumbnail(videoId: String): String? {
    return try {
        // Use the direct URL construction for the maximum resolution thumbnail
        "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
    } catch (e: Exception) {
        logger.log(Level.SEVERE, "Error fetching thumbnail URL", e)
        null
    }
}

suspend fun downloadVideo(url: String, downloadType: DownloadType, videoFormat: String, audioFormat: String): Job =
    coroutineScope {
        launch {
            val progress = 0f

            when (downloadType) {
                DownloadType.AUDIO -> {
                    handleDownload(url, null, audioFormat, progress, downloadType) { result ->
                        result.onSuccess { file ->
                            println("Audio download completed: ${file.absolutePath}")
                        }.onFailure { error ->
                            println("Error during audio download: ${error.message}")
                        }
                    }
                }

                DownloadType.BOTH -> {
                    handleDownload(url, videoFormat, audioFormat, progress, downloadType) { result ->
                        result.onSuccess { file ->
                            println("Download completed: ${file.absolutePath}")
                        }.onFailure { error ->
                            println("Error during download: ${error.message}")
                        }
                    }
                }
            }
        }
    }


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
