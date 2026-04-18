package com.seigz.webjingles.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.os.Environment
import com.seigz.webjingles.data.model.SearchResult
import com.seigz.webjingles.data.repository.StreamResolver
import com.seigz.webjingles.player.AudioPlayerManager
import com.seigz.webjingles.player.PlayerState
import com.seigz.webjingles.player.TrimManager
import com.seigz.webjingles.ui.components.AudioEffects
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class TrimState {
    data object Idle : TrimState()
    data object Trimming : TrimState()
    data class Success(val outputPath: String) : TrimState()
    data class Error(val message: String) : TrimState()
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PlayerViewModel"
    }

    val playerManager = AudioPlayerManager(application)
    val playerState: StateFlow<PlayerState> = playerManager.playerState
    private val trimManager = TrimManager(application)

    private val _trimState = MutableStateFlow<TrimState>(TrimState.Idle)
    val trimState: StateFlow<TrimState> = _trimState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                playerManager.updatePosition()
                delay(250)
            }
        }
    }

    fun playPreview(result: SearchResult) {
        Log.d(TAG, "playPreview called for: ${result.id} - ${result.title}")

        // Reset effects before playing new preview
        resetEffects()

        // If we already have a direct stream URL, use it
        if (result.streamUrl != null) {
            Log.d(TAG, "Using existing streamUrl: ${result.streamUrl}")
            playerManager.playUrl(
                url = result.streamUrl,
                trackId = result.id,
                title = result.title,
                game = result.gameName,
                thumbnail = result.thumbnailUrl
            )
            return
        }

        // Otherwise resolve a real audio stream via Piped
        Log.d(TAG, "Resolving stream via Piped for videoId: ${result.id}")
        playerManager.setBufferingState(result.id, result.title, result.gameName, result.thumbnailUrl)

        viewModelScope.launch {
            val resolved = StreamResolver.resolveAudioStream(result.id)
            resolved.fold(
                onSuccess = { stream ->
                    Log.d(TAG, "Stream resolved: ${stream.audioUrl.take(100)}")
                    playerManager.playUrl(
                        url = stream.audioUrl,
                        trackId = result.id,
                        title = result.title,
                        game = result.gameName,
                        thumbnail = result.thumbnailUrl
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "Stream resolution failed: ${error.message}")
                    playerManager.setError(error.message ?: "Could not load audio stream")
                }
            )
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun stop() {
        playerManager.stop()
    }

    fun previewRange(startMs: Long, endMs: Long) {
        val state = playerState.value
        if (state.currentTrackId != null) {
            playerManager.seekTo(startMs)
        }
    }

    fun trimCurrentTrack(startMs: Long, endMs: Long, fileName: String) {
        val state = playerState.value
        val trackId = state.currentTrackId ?: return

        _trimState.value = TrimState.Trimming

        viewModelScope.launch {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val webJinglesDir = File(musicDir, "WebJingles")
            if (!webJinglesDir.exists()) webJinglesDir.mkdirs()

            val sanitized = fileName
                .replace(Regex("[^a-zA-Z0-9._\\- ]"), "")
                .replace(Regex("\\s+"), "_")
                .take(80)
            val outputFile = File(webJinglesDir, "${sanitized}_trimmed.m4a")

            val sourceUrl = playerManager.currentSourceUrl
            if (sourceUrl != null) {
                val result = trimManager.trimFromCacheUrl(sourceUrl, startMs, endMs, outputFile)
                _trimState.value = if (result.success) {
                    TrimState.Success(result.outputPath ?: outputFile.absolutePath)
                } else {
                    TrimState.Error(result.error ?: "Trim failed")
                }
            } else {
                _trimState.value = TrimState.Error("No audio source available")
            }
        }
    }

    fun clearTrimState() {
        _trimState.value = TrimState.Idle
    }

    private var currentFadeOutDuration = 0f
    private var trimEndJob: Job? = null

    fun playPreviewWithTrim(result: SearchResult, effects: AudioEffects) {
        trimEndJob?.cancel()
        playPreview(result)
        applyEffects(effects)

        // After stream is ready, seek to trim start and schedule auto-stop at trim end
        viewModelScope.launch {
            // Wait for playback to actually start
            var waited = 0
            while (!playerState.value.isPlaying && waited < 5000) {
                delay(100)
                waited += 100
            }
            if (effects.trimStartMs > 0) {
                seekTo(effects.trimStartMs)
            }
            if (effects.trimEndMs > 0) {
                trimEndJob = viewModelScope.launch {
                    while (true) {
                        delay(100)
                        val pos = playerState.value.currentPosition
                        if (pos >= effects.trimEndMs) {
                            playerManager.pause()
                            break
                        }
                    }
                }
            }
        }
    }

    fun applyEffects(effects: AudioEffects) {
        // Apply fade in effect
        if (effects.fadeIn) {
            playerManager.setVolume(0f)
            viewModelScope.launch {
                val steps = 20
                val stepDelay = (effects.fadeInDurationSec * 1000 / steps).toLong()
                delay(100) // small delay to ensure volume is set
                for (i in 1..steps) {
                    delay(stepDelay)
                    playerManager.setVolume(effects.volume * (i.toFloat() / steps))
                }
            }
        } else {
            playerManager.setVolume(effects.volume)
        }

        // Store fade out info for later use when stopping
        currentFadeOutDuration = if (effects.fadeOut) effects.fadeOutDurationSec else 0f
    }

    fun stopWithFadeOut() {
        if (currentFadeOutDuration > 0f) {
            viewModelScope.launch {
                val steps = 20
                val stepDelay = (currentFadeOutDuration * 1000 / steps).toLong()
                val startVolume = playerManager.getCurrentVolume()

                for (i in steps downTo 1) {
                    delay(stepDelay)
                    playerManager.setVolume(startVolume * (i.toFloat() / steps))
                }

                playerManager.stop()
                currentFadeOutDuration = 0f
            }
        } else {
            playerManager.stop()
        }
    }

    fun resetEffects() {
        currentFadeOutDuration = 0f
        trimEndJob?.cancel()
        trimEndJob = null
        playerManager.resetEffects()
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
