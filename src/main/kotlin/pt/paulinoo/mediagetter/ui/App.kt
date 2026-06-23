package pt.paulinoo.mediagetter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import pt.paulinoo.mediagetter.model.AppTheme
import pt.paulinoo.mediagetter.ui.theme.MediaGetterTheme
import pt.paulinoo.mediagetter.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val vm = remember { MainViewModel() }
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.init() }

    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (message.startsWith("Download concluído")) "Abrir pasta" else null,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) vm.openDownloadsFolder()
        }
    }

    val darkTheme = state.settings.theme == AppTheme.DARK

    MediaGetterTheme(darkTheme = darkTheme) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                AppHeader(
                    showSettings = state.showSettings,
                    showQueue = state.showQueue,
                    showBack = state.videoInfo != null || state.playlist != null,
                    darkTheme = darkTheme,
                    queueCount = state.activeQueueCount,
                    onBack = vm::clearSelection,
                    onToggleTheme = {
                        vm.updateSettings {
                            it.copy(theme = if (darkTheme) AppTheme.LIGHT else AppTheme.DARK)
                        }
                    },
                    onOpenSettings = vm::openSettings,
                    onCloseSettings = vm::closeSettings,
                    onOpenQueue = vm::openQueue,
                    onCloseQueue = vm::closeQueue
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(padding)
            ) {
                when {
                    state.showSettings -> SettingsScreen(
                        settings = state.settings,
                        ytDlpVersion = state.ytDlpVersion,
                        updating = state.updatingYtDlp,
                        onChange = vm::updateSettings,
                        onPickFolder = vm::pickDefaultFolder,
                        onUpdateYtDlp = vm::updateYtDlp
                    )
                    state.showQueue -> QueueScreen(
                        tasks = state.queue,
                        onCancel = vm::cancelTask,
                        onRetry = vm::retryTask,
                        onRemove = vm::removeTask,
                        onOpen = vm::openTaskFile,
                        onClearFinished = vm::clearFinished
                    )
                    else -> MainContent(vm = vm, state = state)
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    vm: MainViewModel,
    state: pt.paulinoo.mediagetter.viewmodel.MainUiState
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(8.dp))

        SearchField(
            query = state.query,
            enabled = true,
            onQueryChange = vm::updateQuery,
            onSubmit = vm::submit
        )

        Spacer(Modifier.height(14.dp))

        state.error?.let { ErrorBanner(it) }

        val screen = when {
            state.loading -> "loading"
            state.playlist != null -> "playlist"
            state.videoInfo != null -> "detail"
            state.searchResults.isNotEmpty() -> "results"
            else -> "empty"
        }

        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "screen"
        ) { target ->
            when (target) {
                "loading" -> LoadingState()

                "playlist" -> state.playlist?.let { playlist ->
                    PlaylistScreen(playlist = playlist, onEnqueue = vm::enqueuePlaylist)
                }

                "detail" -> state.videoInfo?.let { info ->
                    VideoInfoCard(
                        info = info,
                        selectedVideo = state.selectedVideoFormat,
                        selectedAudio = state.selectedAudioFormat,
                        audioOnly = state.audioOnly,
                        options = state.options,
                        onPresetSelected = vm::applyPreset,
                        onVideoSelected = vm::selectVideo,
                        onAudioSelected = vm::selectAudio,
                        onAudioOnlyChanged = vm::setAudioOnly,
                        onOptionsChange = vm::updateOptions,
                        onPickFolder = vm::pickOutputFolder,
                        onDownload = vm::download
                    )
                }

                "results" -> SearchResults(
                    results = state.searchResults,
                    onSelect = { vm.selectSearchResult(it) }
                )

                else -> EmptyState(toolsReady = state.toolsReady)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppHeader(
    showSettings: Boolean,
    showQueue: Boolean,
    showBack: Boolean,
    darkTheme: Boolean,
    queueCount: Int,
    onBack: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onOpenQueue: () -> Unit,
    onCloseQueue: () -> Unit
) {
    val isSubScreen = showSettings || showQueue
    val title = when {
        showSettings -> "Definições"
        showQueue -> "Fila de downloads"
        else -> "MediaGetter"
    }

    CenterAlignedTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!showBack && !isSubScreen) {
                    AppLogo(size = 28.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Text(title, fontWeight = FontWeight.SemiBold)
            }
        },
        navigationIcon = {
            when {
                showSettings -> IconButton(onClick = onCloseSettings) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
                showQueue -> IconButton(onClick = onCloseQueue) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
                showBack -> IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
        },
        actions = {
            if (!isSubScreen) {
                IconButton(onClick = onOpenQueue) {
                    BadgedBox(badge = {
                        if (queueCount > 0) Badge { Text("$queueCount") }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = "Fila"
                        )
                    }
                }
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Alternar tema"
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Definições")
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    query: String,
    enabled: Boolean,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            enabled = enabled,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Pesquisar ou colar URL") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onSubmit() }),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        FilledTonalButton(
            onClick = onSubmit,
            enabled = enabled && query.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.height(56.dp)
        ) {
            Text("Procurar")
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(14.dp)
        )
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(toolsReady: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppLogo(size = 72.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                if (toolsReady) "Pesquisa um vídeo ou cola um URL para começar"
                else "A preparar as ferramentas...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!toolsReady) {
                Spacer(Modifier.height(20.dp))
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun SearchResults(
    results: List<pt.paulinoo.mediagetter.model.SearchResult>,
    onSelect: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(results) { result ->
            SearchResultCard(result = result, onClick = { onSelect(result.url) })
        }
    }
}
