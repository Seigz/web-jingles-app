package com.seigz.webjingles.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seigz.webjingles.data.model.DownloadState
import com.seigz.webjingles.data.model.SearchResult
import com.seigz.webjingles.player.LocalSoundManager
import com.seigz.webjingles.ui.components.AdvancedDownloadDialog
import com.seigz.webjingles.ui.components.RecentSearchesDropdown
import com.seigz.webjingles.ui.components.WebSearchBar
import com.seigz.webjingles.ui.components.MiniPlayer
import com.seigz.webjingles.ui.components.ResultItem
import com.seigz.webjingles.ui.theme.BrandRed
import com.seigz.webjingles.ui.theme.AccentRed
import com.seigz.webjingles.ui.theme.DarkBackground
import com.seigz.webjingles.ui.theme.TextDim
import com.seigz.webjingles.ui.theme.TextSecondary
import com.seigz.webjingles.viewmodel.DownloadViewModel
import com.seigz.webjingles.viewmodel.PlayerViewModel
import com.seigz.webjingles.viewmodel.SearchViewModel
import com.seigz.webjingles.viewmodel.SettingsViewModel

@Composable
fun HomeScreen(
    searchViewModel: SearchViewModel,
    playerViewModel: PlayerViewModel,
    downloadViewModel: DownloadViewModel,
    settingsViewModel: SettingsViewModel,
    onSettingsClick: () -> Unit,
    searchFocusRequester: FocusRequester
) {
    val searchState by searchViewModel.uiState.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    val downloadStates by downloadViewModel.downloadStates.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showAdvancedDownloadDialog by remember { mutableStateOf<SearchResult?>(null) }
    var searchBarFocused by remember { mutableStateOf(false) }
    val soundManager = LocalSoundManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // Dismiss keyboard and unfocus when search finishes loading
    LaunchedEffect(searchState.isLoading) {
        if (!searchState.isLoading && searchState.results.isNotEmpty()) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(searchState.selectedIndex) {
        if (searchState.selectedIndex >= 0) {
            listState.animateScrollToItem(searchState.selectedIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with search and settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WebSearchBar(
                    query = searchState.query,
                    onQueryChange = { searchViewModel.updateQuery(it) },
                    onSearch = { searchViewModel.search() },
                    onClear = { searchViewModel.clearSearch() },
                    focusRequester = searchFocusRequester,
                    onFocusChanged = { searchBarFocused = it },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { soundManager?.playClick(); onSettingsClick() }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary
                    )
                }
            }

            RecentSearchesDropdown(
                visible = searchBarFocused && searchState.query.isEmpty() && searchState.recentSearches.isNotEmpty(),
                recentSearches = searchState.recentSearches,
                onRecentClick = { query ->
                    searchViewModel.updateQuery(query)
                    searchViewModel.search(query)
                },
                onClearHistory = { searchViewModel.clearRecentSearches() },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Content area
            Box(modifier = Modifier.weight(1f)) {
                when {
                    searchState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = BrandRed,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Searching...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    searchState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text(
                                    text = "Error",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = AccentRed
                                )
                                Text(
                                    text = searchState.error ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    searchState.results.isEmpty() && searchState.query.isEmpty() -> {
                        // Empty state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = TextDim,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "Web Jingles",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = BrandRed
                                )
                                Text(
                                    text = "Search for video game music, OST tracks, and jingles",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        "\"007 Agent Under Fire GameCube\"",
                                        "\"Halo 3 menu music\"",
                                        "\"Zelda Ocarina of Time theme\""
                                    ).forEach { example ->
                                        Text(
                                            text = example,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextDim,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    searchState.results.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No results found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = searchState.results,
                                key = { _, item -> item.id }
                            ) { index, result ->
                                ResultItem(
                                    result = result,
                                    isSelected = index == searchState.selectedIndex,
                                    isPlaying = playerState.currentTrackId == result.id,
                                    downloadState = downloadStates[result.id] ?: DownloadState.Idle,
                                    onPlay = {
                                        soundManager?.playClick()
                                        playerViewModel.playPreview(result)
                                    },
                                    onDownload = {
                                        soundManager?.playClick()
                                        if (settingsState.advancedMode) {
                                            showAdvancedDownloadDialog = result
                                        } else {
                                            downloadViewModel.downloadTrack(result)
                                        }
                                    },
                                    onClick = {
                                        soundManager?.playClick()
                                        playerViewModel.playPreview(result)
                                    }
                                )
                            }
                            // Pagination controls
                            if (searchState.prevPageToken != null || searchState.nextPageToken != null) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.TextButton(
                                            onClick = {
                                                soundManager?.playClick()
                                                searchViewModel.prevPage()
                                            },
                                            enabled = searchState.prevPageToken != null
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Previous",
                                                tint = if (searchState.prevPageToken != null) BrandRed else TextDim,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.size(4.dp))
                                            Text(
                                                text = "Prev",
                                                color = if (searchState.prevPageToken != null) BrandRed else TextDim
                                            )
                                        }
                                        Spacer(Modifier.size(16.dp))
                                        Text(
                                            text = "Page ${searchState.currentPage}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextSecondary
                                        )
                                        Spacer(Modifier.size(16.dp))
                                        androidx.compose.material3.TextButton(
                                            onClick = {
                                                soundManager?.playClick()
                                                searchViewModel.nextPage()
                                            },
                                            enabled = searchState.nextPageToken != null
                                        ) {
                                            Text(
                                                text = "Next",
                                                color = if (searchState.nextPageToken != null) BrandRed else TextDim
                                            )
                                            Spacer(Modifier.size(4.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Next",
                                                tint = if (searchState.nextPageToken != null) BrandRed else TextDim,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            // Bottom spacer for mini player
                            item {
                                Spacer(Modifier.height(120.dp))
                            }
                        }
                    }
                }
            }
        }

        // Mini player at bottom
        MiniPlayer(
            playerState = playerState,
            onPlayPause = { soundManager?.playClick(); playerViewModel.togglePlayPause() },
            onStop = { soundManager?.playClick(); playerViewModel.resetEffects(); playerViewModel.stop() },
            onSeek = { playerViewModel.seekTo(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Advanced download dialog
    showAdvancedDownloadDialog?.let { result ->
        AdvancedDownloadDialog(
            trackTitle = result.title,
            durationMs = result.durationSeconds * 1000L,
            onDismiss = {
                showAdvancedDownloadDialog = null
                playerViewModel.resetEffects()
            },
            onPreview = { effects ->
                playerViewModel.playPreviewWithTrim(result, effects)
            },
            onConfirm = { effects ->
                val hasEffects = effects.fadeIn || effects.fadeOut ||
                    effects.volume != 1.0f || effects.trimStartMs > 0 || effects.trimEndMs > 0
                if (hasEffects) {
                    downloadViewModel.downloadTrackWithEffects(result, effects)
                } else {
                    downloadViewModel.downloadTrack(result)
                }
                playerViewModel.resetEffects()
                showAdvancedDownloadDialog = null
            }
        )
    }
}
