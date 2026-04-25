package com.seigz.webjingles.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.seigz.webjingles.player.LocalSoundManager
import com.seigz.webjingles.ui.theme.BrandRed
import com.seigz.webjingles.ui.theme.AccentGreen
import com.seigz.webjingles.ui.theme.AccentOrange
import com.seigz.webjingles.ui.theme.AccentPurple
import com.seigz.webjingles.ui.theme.DarkBackground
import com.seigz.webjingles.ui.theme.DarkCard
import com.seigz.webjingles.ui.theme.DarkSurface
import com.seigz.webjingles.ui.theme.DarkSurfaceVariant
import com.seigz.webjingles.ui.theme.DividerColor
import com.seigz.webjingles.ui.theme.TextDim
import com.seigz.webjingles.ui.theme.TextPrimary
import com.seigz.webjingles.ui.theme.TextSecondary
import com.seigz.webjingles.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onChooseFolder: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current
    val soundManager = LocalSoundManager.current
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedRow) {
        if (state.selectedRow >= 0) {
            listState.animateScrollToItem(state.selectedRow.coerceAtMost(listState.layoutInfo.totalItemsCount - 1))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { soundManager?.playClick(); onBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = BrandRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // API Key section
            item {
                SettingsSectionHeader(
                    title = "API Configuration",
                    icon = Icons.Default.Key,
                    accentColor = AccentOrange
                )
            }
            item {
                SettingsRow(
                    title = "YouTube API Key",
                    subtitle = if (state.youtubeApiKey.isNotBlank()) "Key configured" else "Required - tap to set",
                    icon = Icons.Default.Key,
                    isSelected = state.selectedRow == 0,
                    onClick = { showApiKeyDialog = true },
                    accentColor = AccentOrange
                )
            }
            item {
                SettingsRow(
                    title = "Get YouTube API Key",
                    subtitle = "Open Google Cloud Console to enable API",
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    isSelected = state.selectedRow == 1,
                    onClick = { uriHandler.openUri("https://console.cloud.google.com/marketplace/product/google/youtube.googleapis.com") },
                    accentColor = AccentOrange
                )
            }

            // Audio settings
            item {
                Spacer(Modifier.height(12.dp))
                SettingsSectionHeader(
                    title = "Audio Settings",
                    icon = Icons.Default.MusicNote,
                    accentColor = AccentPurple
                )
            }
            item {
                SettingsRow(
                    title = "Preferred Format",
                    subtitle = state.preferredFormat,
                    icon = Icons.Default.AudioFile,
                    isSelected = state.selectedRow == 2,
                    onClick = { showFormatDialog = true },
                    accentColor = AccentPurple
                )
            }
            item {
                SettingsToggleRow(
                    title = "UI Sounds",
                    subtitle = "Play click sounds on button presses",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    isChecked = state.soundEnabled,
                    isSelected = state.selectedRow == 3,
                    onToggle = { viewModel.setSoundEnabled(it) },
                    accentColor = AccentPurple
                )
            }

            // Search settings
            item {
                Spacer(Modifier.height(12.dp))
                SettingsSectionHeader(
                    title = "Search",
                    icon = Icons.Default.Search,
                    accentColor = AccentGreen
                )
            }
            item {
                SettingsToggleRow(
                    title = "Better Searching",
                    subtitle = if (state.betterSearching) "Find any video on YouTube" else "Optimized for game music (appends keywords)",
                    icon = Icons.Default.Search,
                    isChecked = state.betterSearching,
                    isSelected = state.selectedRow == 4,
                    onToggle = { viewModel.setBetterSearching(it) },
                    accentColor = AccentGreen
                )
            }

            // Download location
            item {
                Spacer(Modifier.height(12.dp))
                SettingsSectionHeader(
                    title = "Download Location",
                    icon = Icons.Default.Folder,
                    accentColor = AccentGreen
                )
            }
            item {
                SettingsRow(
                    title = "Download Folder",
                    subtitle = state.downloadFolderName,
                    icon = Icons.Default.Folder,
                    isSelected = state.selectedRow == 5,
                    onClick = onChooseFolder,
                    accentColor = AccentGreen
                )
            }

            // General settings
            item {
                Spacer(Modifier.height(12.dp))
                SettingsSectionHeader(
                    title = "General Settings",
                    icon = Icons.Default.Settings,
                    accentColor = BrandRed
                )
            }
            item {
                SettingsToggleRow(
                    title = "Enable Portrait Mode",
                    subtitle = "Allow screen rotation to portrait",
                    icon = Icons.Default.ScreenRotation,
                    isChecked = state.enablePortrait,
                    isSelected = state.selectedRow == 6,
                    onToggle = { viewModel.setEnablePortrait(it) },
                    accentColor = BrandRed
                )
            }
            item {
                SettingsToggleRow(
                    title = "Auto-download Highest Quality",
                    subtitle = "Automatically select best audio quality",
                    icon = Icons.Default.Speed,
                    isChecked = state.autoDownloadHQ,
                    isSelected = state.selectedRow == 7,
                    onToggle = { viewModel.setAutoDownloadHQ(it) },
                    accentColor = BrandRed
                )
            }
            item {
                SettingsToggleRow(
                    title = "Normalize Audio Volume",
                    subtitle = "Equalize volume levels across tracks",
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    isChecked = state.normalizeAudio,
                    isSelected = state.selectedRow == 8,
                    onToggle = { viewModel.setNormalizeAudio(it) },
                    accentColor = BrandRed
                )
            }

            // Display & Fullscreen
            item {
                Spacer(Modifier.height(12.dp))
                SettingsSectionHeader(
                    title = "Display",
                    icon = Icons.Default.Fullscreen,
                    accentColor = AccentPurple
                )
            }
            item {
                SettingsToggleRow(
                    title = "Fullscreen Mode",
                    subtitle = "Hide system bars for immersive experience",
                    icon = Icons.Default.Fullscreen,
                    isChecked = state.fullscreenMode,
                    isSelected = state.selectedRow == 9,
                    onToggle = { viewModel.setFullscreenMode(it) },
                    accentColor = AccentPurple
                )
            }
            item {
                SettingsSliderRow(
                    title = "UI Scale",
                    subtitle = "${"%.0f".format(state.uiScale * 100)}%",
                    icon = Icons.Default.TextFields,
                    value = state.uiScale,
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    isSelected = state.selectedRow == 10,
                    onValueChange = { viewModel.setUiScale(it) },
                    accentColor = AccentPurple
                )
            }

            // Audio Tools
            item {
                Spacer(Modifier.height(12.dp))
                SettingsSectionHeader(
                    title = "Audio Tools",
                    icon = Icons.Default.ContentCut,
                    accentColor = AccentGreen
                )
            }
            item {
                SettingsToggleRow(
                    title = "Advanced Mode",
                    subtitle = "Adds audio editing to downloads & trims",
                    icon = Icons.Default.Settings,
                    isChecked = state.advancedMode,
                    isSelected = state.selectedRow == 11,
                    onToggle = { viewModel.setAdvancedMode(it) },
                    accentColor = AccentGreen
                )
            }

            // Storage & Cache
            item {
                Spacer(Modifier.height(12.dp))
                SettingsSectionHeader(
                    title = "Storage & Cache",
                    icon = Icons.Default.Storage,
                    accentColor = AccentOrange
                )
            }
            item {
                SettingsRow(
                    title = "Clear Preview Cache",
                    subtitle = "Current cache: ${state.cacheSize}",
                    icon = Icons.Default.CleaningServices,
                    isSelected = state.selectedRow == 12,
                    onClick = { viewModel.clearCache() },
                    accentColor = AccentOrange
                )
            }

            // About
            item {
                Spacer(Modifier.height(12.dp))
                SettingsSectionHeader(
                    title = "About",
                    icon = Icons.Default.Info,
                    accentColor = TextDim
                )
            }
            item {
                SettingsRow(
                    title = "About Web Jingles",
                    subtitle = "Version beta-2.0",
                    icon = Icons.Default.Info,
                    isSelected = state.selectedRow == 13,
                    onClick = { showAboutDialog = true },
                    accentColor = TextDim
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = state.youtubeApiKey,
            onDismiss = { showApiKeyDialog = false },
            onSave = { key ->
                viewModel.setYoutubeApiKey(key)
                showApiKeyDialog = false
            }
        )
    }

    // Format Dialog
    if (showFormatDialog) {
        FormatPickerDialog(
            currentFormat = state.preferredFormat,
            onDismiss = { showFormatDialog = false },
            onSelect = { format ->
                viewModel.setPreferredFormat(format)
                showFormatDialog = false
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = accentColor
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            color = DividerColor
        )
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    val soundManager = LocalSoundManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .clickable { soundManager?.playClick(); onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    accentColor: Color
) {
    val soundManager = LocalSoundManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .clickable { soundManager?.playClick(); onToggle(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = { soundManager?.playClick(); onToggle(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = BrandRed,
                checkedTrackColor = BrandRed.copy(alpha = 0.3f),
                uncheckedThumbColor = TextDim,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}

@Composable
@Suppress("UNUSED_PARAMETER", "SameParameterValue")
private fun SettingsSliderRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    isSelected: Boolean,
    onValueChange: (Float) -> Unit,
    accentColor: Color
) {
    var localValue by remember(value) { mutableFloatStateOf(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "${"%.0f".format(localValue * 100)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value = localValue,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onValueChange(localValue) },
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = DarkSurfaceVariant
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${"%.0f".format(valueRange.start * 100)}%",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
            Text(
                text = "${"%.0f".format(valueRange.endInclusive * 100)}%",
                style = MaterialTheme.typography.labelSmall,
                color = TextDim
            )
        }
    }
}

@Composable
private fun ApiKeyDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("YouTube API Key", color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter your YouTube Data API v3 key to enable search.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    singleLine = true,
                    cursorBrush = SolidColor(BrandRed),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSave(text) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text("Save", color = BrandRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun FormatPickerDialog(
    currentFormat: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text("Preferred Audio Format", color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("WAV", "MP3").forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (format == currentFormat) BrandRed.copy(alpha = 0.15f)
                                else DarkSurfaceVariant
                            )
                            .border(
                                width = if (format == currentFormat) 2.dp else 0.dp,
                                color = if (format == currentFormat) BrandRed else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelect(format) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = format,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (format == currentFormat) BrandRed else TextPrimary
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = if (format == "WAV") "Lossless (larger)" else "Compressed (smaller)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = BrandRed,
                    modifier = Modifier.size(28.dp)
                )
                Text("Web Jingles", color = BrandRed)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Version beta-2.0",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Search, preview, and download video game music for use with frontend launchers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Built for handheld gaming devices like the AYN Thor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDim
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = BrandRed)
            }
        }
    )
}
