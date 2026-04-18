package com.seigz.webjingles.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seigz.webjingles.data.model.DownloadState
import com.seigz.webjingles.data.model.SearchResult
import com.seigz.webjingles.data.preferences.AppPreferences
import com.seigz.webjingles.data.repository.StreamResolver
import com.seigz.webjingles.download.DownloadManager
import com.seigz.webjingles.player.AudioProcessor
import com.seigz.webjingles.ui.components.AudioEffects
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "DownloadViewModel"
    }

    private val preferences = AppPreferences(application)
    private val downloadManager = DownloadManager(application, preferences)

    val downloadStates: StateFlow<Map<String, DownloadState>> = downloadManager.downloadStates

    fun downloadTrackWithEffects(result: SearchResult, effects: AudioEffects) {
        val fileName = formatFileName(result)
        val processorEffects = AudioProcessor.Effects(
            trimStartMs = effects.trimStartMs,
            trimEndMs = effects.trimEndMs,
            fadeIn = effects.fadeIn,
            fadeOut = effects.fadeOut,
            fadeInDurationSec = effects.fadeInDurationSec,
            fadeOutDurationSec = effects.fadeOutDurationSec,
            volume = effects.volume
        )

        viewModelScope.launch {
            Log.d(TAG, "Resolving stream for effects download: ${result.id} - ${result.title}")
            downloadManager.updateDownloadState(result.id, DownloadState.Downloading(0f, "Resolving stream..."))

            // Get stream URL
            val streamUrl = if (result.streamUrl != null) {
                result.streamUrl
            } else {
                val resolved = StreamResolver.resolveAudioStream(result.id)
                resolved.getOrNull()?.audioUrl
            }

            if (streamUrl == null) {
                downloadManager.updateDownloadState(result.id, DownloadState.Failed("Could not resolve audio stream"))
                return@launch
            }

            Log.d(TAG, "Starting effects download for: ${result.title}")
            downloadManager.downloadAudioWithEffects(
                trackId = result.id,
                url = streamUrl,
                fileName = fileName,
                effects = processorEffects
            )
        }
    }

    fun downloadTrack(result: SearchResult, customName: String? = null) {
        val fileName = customName ?: formatFileName(result)

        viewModelScope.launch {
            // If we already have a direct stream URL, use it
            if (result.streamUrl != null) {
                Log.d(TAG, "Downloading with existing streamUrl for: ${result.title}")
                downloadManager.downloadAudio(
                    trackId = result.id,
                    url = result.streamUrl,
                    fileName = fileName
                )
                return@launch
            }

            // Resolve the audio stream URL first
            Log.d(TAG, "Resolving stream for download: ${result.id} - ${result.title}")
            downloadManager.updateDownloadState(result.id, DownloadState.Downloading(0f, "Resolving stream..."))

            val resolved = StreamResolver.resolveAudioStream(result.id)
            resolved.fold(
                onSuccess = { stream ->
                    Log.d(TAG, "Stream resolved for download: ${stream.audioUrl.take(100)}")
                    downloadManager.downloadAudio(
                        trackId = result.id,
                        url = stream.audioUrl,
                        fileName = fileName
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "Stream resolution failed for download: ${error.message}")
                    downloadManager.updateDownloadState(
                        result.id,
                        DownloadState.Failed("Could not resolve audio: ${error.message}")
                    )
                }
            )
        }
    }

    fun getDownloadState(trackId: String): DownloadState {
        return downloadManager.getDownloadState(trackId)
    }

    private fun formatFileName(result: SearchResult): String {
        val game = result.gameName
            .replace(Regex("[^a-zA-Z0-9 ]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
        val title = result.title
            .replace(Regex("[^a-zA-Z0-9 ]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
        return "${game}_${title}".take(80)
    }
}
