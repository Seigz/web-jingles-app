package com.seigz.webjingles.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentTrackId: String? = null,
    val currentTrackTitle: String = "",
    val currentTrackGame: String = "",
    val currentThumbnail: String? = null,
    val isBuffering: Boolean = false
)

class AudioPlayerManager(context: Context) {

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    var currentSourceUrl: String? = null
        private set

    @OptIn(UnstableApi::class)
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .build()
        .apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            _playerState.value = _playerState.value.copy(isBuffering = true)
                        }
                        Player.STATE_READY -> {
                            _playerState.value = _playerState.value.copy(
                                isBuffering = false,
                                duration = duration
                            )
                        }
                        Player.STATE_ENDED -> {
                            _playerState.value = _playerState.value.copy(
                                isPlaying = false,
                                currentPosition = 0L
                            )
                        }
                        Player.STATE_IDLE -> {
                            _playerState.value = _playerState.value.copy(isBuffering = false)
                        }
                    }
                }
            })
        }

    fun playUrl(url: String, trackId: String, title: String, game: String, thumbnail: String?) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        currentSourceUrl = url
        val mediaItem = MediaItem.fromUri(url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        _playerState.value = PlayerState(
            isPlaying = true,
            currentTrackId = trackId,
            currentTrackTitle = title,
            currentTrackGame = game,
            currentThumbnail = thumbnail,
            isBuffering = true
        )
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPosition = positionMs)
    }

    fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        currentSourceUrl = null
        _playerState.value = PlayerState()
    }

    fun updatePosition() {
        if (exoPlayer.isPlaying) {
            _playerState.value = _playerState.value.copy(
                currentPosition = exoPlayer.currentPosition,
                duration = exoPlayer.duration.coerceAtLeast(0L)
            )
        }
    }

    fun setBufferingState(trackId: String, title: String, game: String, thumbnail: String?) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _playerState.value = PlayerState(
            isPlaying = false,
            currentTrackId = trackId,
            currentTrackTitle = title,
            currentTrackGame = game,
            currentThumbnail = thumbnail,
            isBuffering = true
        )
    }

    fun setError(message: String) {
        _playerState.value = _playerState.value.copy(
            isBuffering = false,
            isPlaying = false
        )
    }

    fun setPitch(pitch: Float) {
        val current = exoPlayer.playbackParameters
        exoPlayer.playbackParameters = PlaybackParameters(current.speed, pitch.coerceIn(0.5f, 2.0f))
    }

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 2.0f)
    }

    fun getCurrentVolume(): Float {
        return exoPlayer.volume
    }

    fun setSpeed(speed: Float) {
        val current = exoPlayer.playbackParameters
        exoPlayer.playbackParameters = PlaybackParameters(speed.coerceIn(0.25f, 4.0f), current.pitch)
    }

    fun resetEffects() {
        exoPlayer.playbackParameters = PlaybackParameters.DEFAULT
        exoPlayer.volume = 1.0f
    }

    fun release() {
        exoPlayer.release()
    }
}
