package com.seigz.webjingles.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.seigz.webjingles.ui.theme.BrandRed
import com.seigz.webjingles.ui.theme.AccentGreen
import com.seigz.webjingles.ui.theme.AccentPurple
import com.seigz.webjingles.ui.theme.DarkSurface
import com.seigz.webjingles.ui.theme.DarkSurfaceVariant
import com.seigz.webjingles.ui.theme.TextDim
import com.seigz.webjingles.ui.theme.TextPrimary
import com.seigz.webjingles.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

data class TrimRange(
    val startMs: Long,
    val endMs: Long
)

data class TrimWithEffects(
    val startMs: Long,
    val endMs: Long,
    val effects: AudioEffects
)

@Suppress("unused")
@Composable
fun TrimDialog(
    trackTitle: String,
    durationMs: Long,
    advancedMode: Boolean = false,
    onDismiss: () -> Unit,
    onApply: (TrimRange) -> Unit,
    onApplyWithEffects: ((TrimWithEffects) -> Unit)? = null,
    onPreviewRange: ((TrimRange) -> Unit)? = null
) {
    val durationSec = (durationMs / 1000f).coerceAtLeast(1f)
    var range by remember { mutableStateOf(0f..durationSec) }

    // Zoom level: 1 = full, higher = zoomed in
    var zoomLevel by remember { mutableIntStateOf(1) }
    val maxZoom = 8

    // Hold-to-zoom: track whether user is dragging the slider
    var isDragging by remember { mutableStateOf(false) }

    // Auto-zoom while holding the slider — like Android's video editor
    LaunchedEffect(isDragging) {
        if (!isDragging) return@LaunchedEffect
        delay(1000) // Initial hold threshold before zoom kicks in
        while (isDragging && zoomLevel < maxZoom) {
            zoomLevel++
            delay(800) // Progressive zoom interval
        }
    }

    // Zoom window computed from current range center
    val zoomWindowSec = durationSec / zoomLevel
    val rangeCenter = (range.start + range.endInclusive) / 2f
    val zoomStart = (rangeCenter - zoomWindowSec / 2f).coerceIn(0f, (durationSec - zoomWindowSec).coerceAtLeast(0f))
    val zoomEnd = (zoomStart + zoomWindowSec).coerceAtMost(durationSec)

    // Generate a synthetic waveform based on the title hash for consistency
    val waveformData = remember(trackTitle, durationMs) {
        val seed = trackTitle.hashCode().toLong()
        val rng = Random(seed)
        val sampleCount = 80
        FloatArray(sampleCount) { i ->
            val base = abs(sin(i * 0.15f + rng.nextFloat() * 3f))
            val detail = rng.nextFloat() * 0.3f
            val envelope = 0.3f + 0.7f * sin(i.toFloat() / sampleCount * Math.PI.toFloat())
            ((base * 0.7f + detail) * envelope).coerceIn(0.08f, 1f)
        }
    }

    // Advanced mode effects
    var fadeIn by remember { mutableStateOf(false) }
    var fadeOut by remember { mutableStateOf(false) }
    var fadeInDuration by remember { mutableFloatStateOf(2f) }
    var fadeOutDuration by remember { mutableFloatStateOf(2f) }
    var volume by remember { mutableFloatStateOf(1.0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(24.dp)
                )
                Text("Trim Audio", color = TextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = trackTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1
                )

                Spacer(Modifier.height(4.dp))

                // Time display with millisecond precision
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Start", style = MaterialTheme.typography.labelSmall, color = TextDim)
                        Text(
                            text = formatTrimTimePrecise(range.start),
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandRed
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Duration", style = MaterialTheme.typography.labelSmall, color = TextDim)
                        Text(
                            text = formatTrimTimePrecise(range.endInclusive - range.start),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("End", style = MaterialTheme.typography.labelSmall, color = TextDim)
                        Text(
                            text = formatTrimTimePrecise(range.endInclusive),
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandRed
                        )
                    }
                }

                // Waveform visualization — tap to reset zoom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .clickable { zoomLevel = 1 }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp)) {
                        val w = size.width
                        val h = size.height
                        val centerY = h / 2f
                        val barCount = waveformData.size
                        val gap = 2f
                        val barW = (w - gap * barCount) / barCount

                        for (i in 0 until barCount) {
                            val sampleTime = zoomStart + (zoomEnd - zoomStart) * (i.toFloat() / barCount)
                            val globalIndex = ((sampleTime / durationSec) * (waveformData.size - 1)).toInt()
                                .coerceIn(0, waveformData.size - 1)
                            val amplitude = waveformData[globalIndex]
                            val barH = amplitude * h * 0.44f

                            val inSelection = sampleTime >= range.start && sampleTime <= range.endInclusive
                            val color = if (inSelection) BrandRed else BrandRed.copy(alpha = 0.18f)
                            val x = i * (barW + gap)

                            // Top half
                            drawRect(
                                color = color,
                                topLeft = Offset(x, centerY - barH),
                                size = Size(barW.coerceAtLeast(2f), barH)
                            )
                            // Bottom half (mirror)
                            drawRect(
                                color = color,
                                topLeft = Offset(x, centerY),
                                size = Size(barW.coerceAtLeast(2f), barH)
                            )
                        }

                        // Selection boundary lines
                        val startX = ((range.start - zoomStart) / (zoomEnd - zoomStart) * w).coerceIn(0f, w)
                        val endX = ((range.endInclusive - zoomStart) / (zoomEnd - zoomStart) * w).coerceIn(0f, w)
                        drawLine(BrandRed, Offset(startX, 0f), Offset(startX, h), strokeWidth = 3f)
                        drawLine(BrandRed, Offset(endX, 0f), Offset(endX, h), strokeWidth = 3f)
                    }
                }

                // Zoom indicator
                if (zoomLevel > 1) {
                    Text(
                        text = "${zoomLevel}x zoom  •  Tap waveform to reset",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentPurple,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Hold slider to auto-zoom for precision",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // Range slider with hold-to-zoom
                RangeSlider(
                    value = range,
                    onValueChange = { isDragging = true; range = it },
                    onValueChangeFinished = { isDragging = false },
                    valueRange = 0f..durationSec,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = BrandRed,
                        activeTrackColor = BrandRed,
                        inactiveTrackColor = DarkSurfaceVariant
                    )
                )

                // Advanced effects (only in advanced mode)
                if (advancedMode) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Audio Effects",
                        style = MaterialTheme.typography.titleSmall,
                        color = AccentPurple
                    )

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
                                inactiveTrackColor = DarkSurface
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
                                inactiveTrackColor = DarkSurface
                            )
                        )
                    }

                    // Volume
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = AccentGreen, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Volume", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                Text("${"%.0f".format(volume * 100)}%", style = MaterialTheme.typography.labelMedium, color = AccentGreen)
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
                } else {
                    Text(
                        text = "Select the portion of the audio you want to keep.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDim
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onPreviewRange != null) {
                    TextButton(
                        onClick = {
                            onPreviewRange(
                                TrimRange(
                                    startMs = (range.start * 1000).toLong(),
                                    endMs = (range.endInclusive * 1000).toLong()
                                )
                            )
                        }
                    ) {
                        Text("Preview", color = AccentPurple)
                    }
                }
                TextButton(
                    onClick = {
                        if (advancedMode && onApplyWithEffects != null) {
                            onApplyWithEffects(
                                TrimWithEffects(
                                    startMs = (range.start * 1000).toLong(),
                                    endMs = (range.endInclusive * 1000).toLong(),
                                    effects = AudioEffects(
                                        fadeIn = fadeIn,
                                        fadeOut = fadeOut,
                                        fadeInDurationSec = fadeInDuration,
                                        fadeOutDurationSec = fadeOutDuration,
                                        volume = volume
                                    )
                                )
                            )
                        } else {
                            onApply(
                                TrimRange(
                                    startMs = (range.start * 1000).toLong(),
                                    endMs = (range.endInclusive * 1000).toLong()
                                )
                            )
                        }
                    }
                ) {
                    Text("Trim & Save", color = BrandRed)
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

private fun formatTrimTimePrecise(seconds: Float): String {
    val totalMs = (seconds * 1000).toLong().coerceAtLeast(0)
    val min = totalMs / 60000
    val sec = (totalMs % 60000) / 1000
    val ms = totalMs % 1000
    return "$min:${sec.toString().padStart(2, '0')}.${(ms / 10).toString().padStart(2, '0')}"
}
