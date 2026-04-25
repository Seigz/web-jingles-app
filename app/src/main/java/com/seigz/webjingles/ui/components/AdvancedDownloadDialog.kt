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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
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

    // Zoom for timeline trimmer
    var zoomLevel by remember { mutableIntStateOf(1) }
    val maxZoom = 8
    var isHolding by remember { mutableStateOf(false) }
    var sliderMoved by remember { mutableStateOf(false) }

    // Auto-zoom while holding the trim slider still — stops if user moves it
    LaunchedEffect(isHolding) {
        if (!isHolding) return@LaunchedEffect
        sliderMoved = false
        delay(1000)
        while (isHolding && !sliderMoved && zoomLevel < maxZoom) {
            zoomLevel++
            delay(800)
        }
    }

    // Zoom window centered on the selection
    val zoomWindowSec = durationSec / zoomLevel
    val rangeCenter = (trimRange.start + trimRange.endInclusive) / 2f
    val zoomStart = (rangeCenter - zoomWindowSec / 2f).coerceIn(0f, (durationSec - zoomWindowSec).coerceAtLeast(0f))
    val zoomEnd = (zoomStart + zoomWindowSec).coerceAtMost(durationSec)

    // Synthetic waveform seeded by title for consistency
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

    fun buildEffects() = AudioEffects(
        fadeIn = fadeIn,
        fadeOut = fadeOut,
        fadeInDurationSec = fadeInDuration,
        fadeOutDurationSec = fadeOutDuration,
        volume = volume,
        trimStartMs = if (trimEnabled) (trimRange.start * 1000).toLong() else 0L,
        trimEndMs = if (trimEnabled) (trimRange.endInclusive * 1000).toLong() else 0L
    )

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
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState(), enabled = !isHolding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = trackTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2
                )

                Spacer(Modifier.height(4.dp))

                // ── Timeline Trim Section ──
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ContentCut,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.size(4.dp))
                                Text("Trim Audio", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            }
                            if (trimEnabled) {
                                Text(
                                    "${formatEffectTimePrecise(trimRange.start)} → ${formatEffectTimePrecise(trimRange.endInclusive)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandRed
                                )
                            }
                        }
                    }

                    if (trimEnabled) {
                        // Time indicators
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Start", style = MaterialTheme.typography.labelSmall, color = TextDim)
                                Text(
                                    formatEffectTimePrecise(trimRange.start),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BrandRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Selection", style = MaterialTheme.typography.labelSmall, color = TextDim)
                                Text(
                                    formatEffectTimePrecise(trimRange.endInclusive - trimRange.start),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("End", style = MaterialTheme.typography.labelSmall, color = TextDim)
                                Text(
                                    formatEffectTimePrecise(trimRange.endInclusive),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BrandRed
                                )
                            }
                        }

                        // ── Waveform Timeline ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .clickable { zoomLevel = 1 }
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                val w = size.width
                                val h = size.height
                                val centerY = h / 2f
                                val barCount = waveformData.size
                                val gap = 2f
                                val barW = ((w - gap * barCount) / barCount).coerceAtLeast(2f)

                                // Draw waveform bars
                                for (i in 0 until barCount) {
                                    val sampleTime = zoomStart + (zoomEnd - zoomStart) * (i.toFloat() / barCount)
                                    val globalIdx = ((sampleTime / durationSec) * (waveformData.size - 1))
                                        .toInt().coerceIn(0, waveformData.size - 1)
                                    val amp = waveformData[globalIdx]
                                    val barH = amp * h * 0.42f
                                    val inSel = sampleTime >= trimRange.start && sampleTime <= trimRange.endInclusive
                                    val color = if (inSel) BrandRed else BrandRed.copy(alpha = 0.15f)
                                    val x = i * (barW + gap)

                                    // Top half
                                    drawRect(color, Offset(x, centerY - barH), Size(barW, barH))
                                    // Bottom mirror
                                    drawRect(color, Offset(x, centerY), Size(barW, barH))
                                }

                                // Selection boundary markers
                                val startX = ((trimRange.start - zoomStart) / (zoomEnd - zoomStart) * w).coerceIn(0f, w)
                                val endX = ((trimRange.endInclusive - zoomStart) / (zoomEnd - zoomStart) * w).coerceIn(0f, w)
                                drawLine(BrandRed, Offset(startX, 0f), Offset(startX, h), strokeWidth = 3f)
                                drawLine(BrandRed, Offset(endX, 0f), Offset(endX, h), strokeWidth = 3f)

                                // Dimmed regions outside selection
                                if (startX > 0f) {
                                    drawRect(
                                        DarkSurfaceVariant.copy(alpha = 0.6f),
                                        Offset(0f, 0f),
                                        Size(startX, h)
                                    )
                                }
                                if (endX < w) {
                                    drawRect(
                                        DarkSurfaceVariant.copy(alpha = 0.6f),
                                        Offset(endX, 0f),
                                        Size(w - endX, h)
                                    )
                                }
                            }
                        }

                        // Zoom indicator
                        Text(
                            text = if (zoomLevel > 1) "${zoomLevel}x zoom  •  Tap waveform to reset"
                            else "Hold trim slider to auto-zoom",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (zoomLevel > 1) AccentPurple else TextDim,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        // Trim range slider — wrapped in pointer detector for hold-to-zoom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        isHolding = true
                                        sliderMoved = false
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            if (event.changes.none { it.pressed }) break
                                        }
                                        isHolding = false
                                    }
                                }
                        ) {
                            RangeSlider(
                                value = trimRange,
                                onValueChange = { sliderMoved = true; trimRange = it },
                                valueRange = 0f..durationSec,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = BrandRed,
                                    activeTrackColor = BrandRed,
                                    inactiveTrackColor = DarkSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                }

                // ── Fade In ──
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

                // ── Fade Out ──
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

                // ── Volume ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
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
                    TextButton(onClick = { onPreview(buildEffects()) }) {
                        Text("Preview", color = AccentPurple)
                    }
                }
                TextButton(onClick = { onConfirm(buildEffects()) }) {
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

private fun formatEffectTimePrecise(seconds: Float): String {
    val totalMs = (seconds * 1000).toLong().coerceAtLeast(0)
    val min = totalMs / 60000
    val sec = (totalMs % 60000) / 1000
    val ms = totalMs % 1000
    return "$min:${sec.toString().padStart(2, '0')}.${(ms / 10).toString().padStart(2, '0')}"
}
