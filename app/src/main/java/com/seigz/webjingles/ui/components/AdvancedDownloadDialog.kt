package com.seigz.webjingles.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seigz.webjingles.ui.theme.BrandRed
import com.seigz.webjingles.ui.theme.AccentGreen
import com.seigz.webjingles.ui.theme.AccentPurple
import com.seigz.webjingles.ui.theme.DarkSurface
import com.seigz.webjingles.ui.theme.DarkSurfaceVariant
import com.seigz.webjingles.ui.theme.TextDim
import com.seigz.webjingles.ui.theme.TextPrimary
import com.seigz.webjingles.ui.theme.TextSecondary

data class AudioEffects(
    val fadeIn: Boolean = false,
    val fadeOut: Boolean = false,
    val fadeInDurationSec: Float = 2f,
    val fadeOutDurationSec: Float = 2f,
    val volume: Float = 1.0f,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L
)

@Composable
fun AdvancedDownloadDialog(
    trackTitle: String,
    durationMs: Long = 0L,
    onDismiss: () -> Unit,
    onPreview: ((AudioEffects) -> Unit)? = null,
    onConfirm: (AudioEffects) -> Unit
) {
    var fadeIn by remember { mutableStateOf(false) }
    var fadeOut by remember { mutableStateOf(false) }
    var fadeInDuration by remember { mutableFloatStateOf(2f) }
    var fadeOutDuration by remember { mutableFloatStateOf(2f) }
    var volume by remember { mutableFloatStateOf(1.0f) }
    var trimEnabled by remember { mutableStateOf(false) }
    val durationSec = (durationMs / 1000f).coerceAtLeast(1f)
    var trimRange by remember { mutableStateOf(0f..durationSec) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text("Download Options", color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = trackTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2
                )

                Spacer(Modifier.height(4.dp))

                // Trim section
                if (durationMs > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = trimEnabled,
                            onCheckedChange = { trimEnabled = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BrandRed,
                                uncheckedColor = TextDim
                            )
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trim Audio", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            if (trimEnabled) {
                                Text(
                                    "${formatEffectTime(trimRange.start)} - ${formatEffectTime(trimRange.endInclusive)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandRed
                                )
                            }
                        }
                    }
                    if (trimEnabled) {
                        RangeSlider(
                            value = trimRange,
                            onValueChange = { trimRange = it },
                            valueRange = 0f..durationSec,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = BrandRed,
                                activeTrackColor = BrandRed,
                                inactiveTrackColor = DarkSurfaceVariant
                            )
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                }

                // Fade In
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = fadeIn,
                        onCheckedChange = { fadeIn = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = BrandRed,
                            uncheckedColor = TextDim
                        )
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fade In", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        if (fadeIn) {
                            Text(
                                "${"%.1f".format(fadeInDuration)}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandRed
                            )
                        }
                    }
                }
                if (fadeIn) {
                    Slider(
                        value = fadeInDuration,
                        onValueChange = { fadeInDuration = it },
                        valueRange = 0.5f..10f,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = BrandRed,
                            activeTrackColor = BrandRed,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                }

                // Fade Out
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = fadeOut,
                        onCheckedChange = { fadeOut = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = BrandRed,
                            uncheckedColor = TextDim
                        )
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fade Out", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        if (fadeOut) {
                            Text(
                                "${"%.1f".format(fadeOutDuration)}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandRed
                            )
                        }
                    }
                }
                if (fadeOut) {
                    Slider(
                        value = fadeOutDuration,
                        onValueChange = { fadeOutDuration = it },
                        valueRange = 0.5f..10f,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = BrandRed,
                            activeTrackColor = BrandRed,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Volume
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Volume", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text(
                                "${"%.0f".format(volume * 100)}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentGreen
                            )
                        }
                        Slider(
                            value = volume,
                            onValueChange = { volume = it },
                            valueRange = 0.1f..2.0f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = AccentGreen,
                                activeTrackColor = AccentGreen,
                                inactiveTrackColor = DarkSurface
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onPreview != null) {
                    TextButton(
                        onClick = {
                            onPreview(
                                AudioEffects(
                                    fadeIn = fadeIn,
                                    fadeOut = fadeOut,
                                    fadeInDurationSec = fadeInDuration,
                                    fadeOutDurationSec = fadeOutDuration,
                                    volume = volume,
                                    trimStartMs = if (trimEnabled) (trimRange.start * 1000).toLong() else 0L,
                                    trimEndMs = if (trimEnabled) (trimRange.endInclusive * 1000).toLong() else 0L
                                )
                            )
                        }
                    ) {
                        Text("Preview", color = AccentPurple)
                    }
                }
                TextButton(
                    onClick = {
                        onConfirm(
                            AudioEffects(
                                fadeIn = fadeIn,
                                fadeOut = fadeOut,
                                fadeInDurationSec = fadeInDuration,
                                fadeOutDurationSec = fadeOutDuration,
                                volume = volume,
                                trimStartMs = if (trimEnabled) (trimRange.start * 1000).toLong() else 0L,
                                trimEndMs = if (trimEnabled) (trimRange.endInclusive * 1000).toLong() else 0L
                            )
                        )
                    }
                ) {
                    Text("Download", color = AccentGreen)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

private fun formatEffectTime(seconds: Float): String {
    val totalSec = seconds.toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
