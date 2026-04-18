package com.seigz.webjingles.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.seigz.webjingles.player.PlayerState
import com.seigz.webjingles.ui.theme.BrandRed
import com.seigz.webjingles.ui.theme.DarkSurface
import com.seigz.webjingles.ui.theme.DarkSurfaceVariant

import com.seigz.webjingles.ui.theme.TextDim
import com.seigz.webjingles.ui.theme.TextPrimary
import com.seigz.webjingles.ui.theme.TextSecondary

@Composable
fun MiniPlayer(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isVisible = playerState.currentTrackId != null

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .border(
                    width = 1.dp,
                    color = BrandRed.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (playerState.currentThumbnail != null) {
                        AsyncImage(
                            model = playerState.currentThumbnail,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = TextDim,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Track info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playerState.currentTrackTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = playerState.currentTrackGame,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play/Pause
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(42.dp)
                        .background(BrandRed.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = BrandRed,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Stop
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop",
                        tint = TextDim,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Seek slider + time
            val duration = playerState.duration.coerceAtLeast(1L)
            var isSeeking by remember { mutableFloatStateOf(-1f) }
            val sliderPosition = if (isSeeking >= 0f) isSeeking
                else playerState.currentPosition.toFloat() / duration.toFloat()

            Slider(
                value = sliderPosition.coerceIn(0f, 1f),
                onValueChange = { isSeeking = it },
                onValueChangeFinished = {
                    if (isSeeking >= 0f) {
                        onSeek((isSeeking * duration).toLong())
                        isSeeking = -1f
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = BrandRed,
                    activeTrackColor = BrandRed,
                    inactiveTrackColor = DarkSurfaceVariant
                )
            )

            // Time labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayPosition = if (isSeeking >= 0f) (isSeeking * duration).toLong()
                    else playerState.currentPosition
                Text(
                    text = formatTime(displayPosition),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )
                Text(
                    text = formatTime(playerState.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDim
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
