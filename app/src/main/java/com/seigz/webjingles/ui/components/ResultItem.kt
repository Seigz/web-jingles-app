package com.seigz.webjingles.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.seigz.webjingles.data.model.DownloadState
import com.seigz.webjingles.data.model.SearchResult
import com.seigz.webjingles.ui.theme.AccentGreen
import com.seigz.webjingles.ui.theme.BrandRed
import com.seigz.webjingles.ui.theme.AccentOrange
import com.seigz.webjingles.ui.theme.AccentRed
import com.seigz.webjingles.ui.theme.DarkCard
import com.seigz.webjingles.ui.theme.DarkSurfaceVariant

import com.seigz.webjingles.ui.theme.TextDim
import com.seigz.webjingles.ui.theme.TextPrimary
import com.seigz.webjingles.ui.theme.TextSecondary

@Composable
fun ResultItem(
    result: SearchResult,
    isSelected: Boolean,
    isPlaying: Boolean,
    downloadState: DownloadState,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(72.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (result.thumbnailUrl != null) {
                AsyncImage(
                    model = result.thumbnailUrl,
                    contentDescription = result.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextDim,
                    modifier = Modifier.size(24.dp)
                )
            }
            // Duration badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = result.duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }

        // Title and game name
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isPlaying) BrandRed else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = result.gameName,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Play button
        IconButton(
            onClick = onPlay,
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isPlaying) BrandRed.copy(alpha = 0.15f) else Color.Transparent,
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = if (isPlaying) BrandRed else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Download button / state
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            when (downloadState) {
                is DownloadState.Idle -> {
                    IconButton(onClick = onDownload) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                is DownloadState.Downloading -> {
                    CircularProgressIndicator(
                        progress = { if (downloadState.progress >= 0) downloadState.progress else 0f },
                        modifier = Modifier.size(24.dp),
                        color = AccentOrange,
                        strokeWidth = 2.dp
                    )
                }
                is DownloadState.Completed -> {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloaded",
                        tint = AccentGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                is DownloadState.Failed -> {
                    IconButton(onClick = onDownload) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Retry download",
                            tint = AccentRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
