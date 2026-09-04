package com.example.slipmat.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.slipmat.library.formatDuration
import kotlin.math.max
import kotlin.math.roundToInt

private val WAVEFORM_HEIGHT = 72.dp

/** How much of the track the magnifier shows, as a fraction of the whole. */
private const val MAGNIFIER_WINDOW = 0.06f

/**
 * The waveform, drawn from peaks and scrubbed by dragging.
 *
 * While a drag is in progress the bar follows the finger and ignores incoming positions — the same
 * rule as the plain seek bar, for the same reason: the 500 ms position ticker would otherwise drag
 * the playhead back out from under the user.
 */
@Composable
fun WaveformSeekBar(
    peaks: FloatArray?,
    progress: Float,
    durationMs: Long,
    onSeek: (fraction: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    var widthPx by remember { mutableFloatStateOf(1f) }

    val shown = scrubFraction ?: progress
    val played = MaterialTheme.colorScheme.primary
    val unplayed = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Box {
            if (peaks == null) {
                // Decoding takes seconds on a long track; a bar that says so beats a blank gap
                // that looks like a bug.
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(WAVEFORM_HEIGHT),
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WAVEFORM_HEIGHT)
                        .pointerInput(peaks) {
                            widthPx = size.width.toFloat()
                            detectTapGestures { offset ->
                                onSeek((offset.x / size.width).coerceIn(0f, 1f))
                            }
                        }
                        .pointerInput(peaks) {
                            widthPx = size.width.toFloat()
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    scrubFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    scrubFraction =
                                        (change.position.x / size.width).coerceIn(0f, 1f)
                                },
                                onDragEnd = {
                                    scrubFraction?.let(onSeek)
                                    scrubFraction = null
                                },
                                onDragCancel = { scrubFraction = null },
                            )
                        },
                ) {
                    drawWaveform(peaks, shown, played, unplayed)
                }

                // Only while dragging: a magnified slice around the finger, so a scrub can be
                // placed on a beat rather than approximately.
                scrubFraction?.let { fraction ->
                    WaveformMagnifier(
                        peaks = peaks,
                        centre = fraction,
                        played = played,
                        unplayed = unplayed,
                        modifier = Modifier.fillMaxWidth().height(WAVEFORM_HEIGHT),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration((shown * durationMs).toLong()),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/**
 * A zoomed slice of the waveform around the finger.
 *
 * Drawn over the top half so it does not sit under the thumb, which is the whole point — a
 * magnifier you cannot see because your hand is on it is decoration.
 */
@Composable
private fun WaveformMagnifier(
    peaks: FloatArray,
    centre: Float,
    played: Color,
    unplayed: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val half = MAGNIFIER_WINDOW / 2f
        val from = ((centre - half) * peaks.size).roundToInt().coerceIn(0, peaks.lastIndex)
        val to = ((centre + half) * peaks.size).roundToInt().coerceIn(from + 1, peaks.size)
        val slice = peaks.copyOfRange(from, to)

        val panelHeight = size.height / 2f
        drawRect(
            color = unplayed.copy(alpha = 0.95f),
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(size.width, panelHeight),
        )

        val barWidth = size.width / max(slice.size, 1)
        slice.forEachIndexed { index, peak ->
            val barHeight = (peak * panelHeight).coerceAtLeast(1f)
            drawRect(
                color = played,
                topLeft = Offset(index * barWidth, (panelHeight - barHeight) / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth * 0.7f, barHeight),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaveform(
    peaks: FloatArray,
    progress: Float,
    played: Color,
    unplayed: Color,
) {
    if (peaks.isEmpty()) return
    val barWidth = size.width / peaks.size
    val centreY = size.height / 2f
    val playedUpTo = progress * peaks.size

    peaks.forEachIndexed { index, peak ->
        // Always at least a hair tall, so silence still reads as part of the track.
        val barHeight = (peak * size.height).coerceAtLeast(2f)
        drawRect(
            color = if (index < playedUpTo) played else unplayed,
            topLeft = Offset(index * barWidth, centreY - barHeight / 2f),
            size = androidx.compose.ui.geometry.Size(barWidth * 0.8f, barHeight),
        )
    }
}
