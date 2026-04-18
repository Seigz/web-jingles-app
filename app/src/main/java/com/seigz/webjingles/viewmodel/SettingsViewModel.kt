package com.seigz.webjingles.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seigz.webjingles.data.preferences.AppPreferences
import com.seigz.webjingles.data.repository.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class SettingsUiState(
    val preferredFormat: String = "WAV",
    val downloadFolderName: String = "/Music/WebJingles/",
    val downloadFolderUri: String? = null,
    val enablePortrait: Boolean = false,
    val autoDownloadHQ: Boolean = false,
    val normalizeAudio: Boolean = false,
    val youtubeApiKey: String = "",
    val cacheSize: String = "0 MB",
    val fullscreenMode: Boolean = true,
    val uiScale: Float = 1.0f,
    val advancedMode: Boolean = false,
    val soundEnabled: Boolean = true,
    val selectedRow: Int = 0
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val preferences = AppPreferences(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.preferredFormat.collect { format ->
                _uiState.value = _uiState.value.copy(preferredFormat = format)
            }
        }
        viewModelScope.launch {
            preferences.downloadFolderName.collect { name ->
                _uiState.value = _uiState.value.copy(downloadFolderName = name)
            }
        }
        viewModelScope.launch {
            preferences.downloadFolderUri.collect { uri ->
                _uiState.value = _uiState.value.copy(downloadFolderUri = uri)
            }
        }
        viewModelScope.launch {
            preferences.enablePortrait.collect { enabled ->
                _uiState.value = _uiState.value.copy(enablePortrait = enabled)
            }
        }
        viewModelScope.launch {
            preferences.autoDownloadHQ.collect { enabled ->
                _uiState.value = _uiState.value.copy(autoDownloadHQ = enabled)
            }
        }
        viewModelScope.launch {
            preferences.normalizeAudio.collect { enabled ->
                _uiState.value = _uiState.value.copy(normalizeAudio = enabled)
            }
        }
        viewModelScope.launch {
            preferences.youtubeApiKey.collect { key ->
                _uiState.value = _uiState.value.copy(youtubeApiKey = key)
                SearchRepository.API_KEY = key
            }
        }
        viewModelScope.launch {
            preferences.fullscreenMode.collect { enabled ->
                _uiState.value = _uiState.value.copy(fullscreenMode = enabled)
            }
        }
        viewModelScope.launch {
            preferences.uiScale.collect { scale ->
                _uiState.value = _uiState.value.copy(uiScale = scale)
            }
        }
        viewModelScope.launch {
            preferences.advancedMode.collect { enabled ->
                _uiState.value = _uiState.value.copy(advancedMode = enabled)
            }
        }
        viewModelScope.launch {
            preferences.soundEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(soundEnabled = enabled)
            }
        }
        calculateCacheSize()
    }

    fun setPreferredFormat(format: String) {
        viewModelScope.launch { preferences.setPreferredFormat(format) }
    }

    fun setDownloadFolder(uri: String, name: String) {
        viewModelScope.launch { preferences.setDownloadFolder(uri, name) }
    }

    fun setEnablePortrait(enabled: Boolean) {
        viewModelScope.launch { preferences.setEnablePortrait(enabled) }
    }

    fun setAutoDownloadHQ(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoDownloadHQ(enabled) }
    }

    fun setNormalizeAudio(enabled: Boolean) {
        viewModelScope.launch { preferences.setNormalizeAudio(enabled) }
    }

    fun setYoutubeApiKey(key: String) {
        viewModelScope.launch {
            preferences.setYoutubeApiKey(key)
            SearchRepository.API_KEY = key
        }
    }

    fun setFullscreenMode(enabled: Boolean) {
        viewModelScope.launch { preferences.setFullscreenMode(enabled) }
    }

    fun setUiScale(scale: Float) {
        viewModelScope.launch { preferences.setUiScale(scale) }
    }

    fun setAdvancedMode(enabled: Boolean) {
        viewModelScope.launch { preferences.setAdvancedMode(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setSoundEnabled(enabled) }
    }

    fun clearCache() {
        viewModelScope.launch {
            val cacheDir = getApplication<Application>().cacheDir
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            preferences.clearCache()
            preferences.clearRecentSearches()
            calculateCacheSize()
        }
    }

    fun moveSelection(delta: Int) {
        val current = _uiState.value
        val newRow = (current.selectedRow + delta).coerceIn(0, 12)
        _uiState.value = current.copy(selectedRow = newRow)
    }

    private fun calculateCacheSize() {
        viewModelScope.launch {
            val cacheDir = getApplication<Application>().cacheDir
            val size = getDirSize(cacheDir)
            val formatted = when {
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> "${size / 1024} KB"
                else -> "${"%.1f".format(size / (1024.0 * 1024.0))} MB"
            }
            _uiState.value = _uiState.value.copy(cacheSize = formatted)
        }
    }

    private fun getDirSize(dir: File): Long {
        var size = 0L
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) getDirSize(file) else file.length()
            }
        }
        return size
    }
}
