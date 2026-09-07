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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.slipmat.library.formatDuration
import com.example.slipmat.ui.theme.CornerExtraSmall
import kotlin.math.max
import kotlin.math.roundToInt

/** §4.2: 64 bars, 52dp tall, 2px gaps, radius-2, bottom-aligned. */
private val WAVEFORM_HEIGHT = 52.dp
private const val BAR_COUNT = 64
private val BAR_GAP = 2.dp
private val BAR_CORNER_RADIUS = 2.dp

/** Thickness of the line shown before the waveform exists. A Material slider track is 4.dp. */
private val FLAT_LINE_THICKNESS = 3.dp

/** How much of the track the magnifier shows, as a fraction of the whole. */
private const val MAGNIFIER_WINDOW = 0.06f

/** Vertical clearance between the tooltip and the bar it floats above. */
private val TOOLTIP_GAP = 8.dp

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
    // surfaceContainerHighest, not surfaceVariant - §3.1 pins the two to the same value, but this
    // is the role the design actually names for the waveform's unplayed bars.
    val unplayed = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(modifier = modifier.fillMaxWidth()) {
        Box {
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
                // Peaks are null while the decode runs, and stay null for a file that cannot be
                // decoded at all. A flat line covers both: it is what the bar is already becoming,
                // so the waveform grows out of it rather than replacing something.
                if (peaks == null) {
                    drawFlatLine(shown, played, unplayed)
                } else {
                    drawWaveform(peaks, shown, played, unplayed)
                }
            }

            // Only while dragging, and only once there is a waveform to magnify.
            // A magnified slice around the finger lets a scrub be placed on a beat rather than
            // approximately.
            if (peaks != null) {
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

            scrubFraction?.let { fraction ->
                ScrubTooltip(
                    timeMs = (fraction * durationMs).toLong(),
                    touchX = fraction * widthPx,
                    trackWidth = widthPx,
                    modifier = Modifier.align(Alignment.TopStart).offset(y = -TOOLTIP_GAP),
                )
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
 * `m:ss` floating above the touch point while scrubbing, clamped so it never overflows the track
 * at either end (§4.2).
 */
@Composable
private fun ScrubTooltip(
    timeMs: Long,
    touchX: Float,
    trackWidth: Float,
    modifier: Modifier = Modifier,
) {
    var tooltipWidthPx by remember { mutableFloatStateOf(0f) }
    val offsetX = clampTooltipX(touchX, tooltipWidthPx, trackWidth)

    Surface(
        modifier = modifier
            .onSizeChanged { tooltipWidthPx = it.width.toFloat() }
            .offset { IntOffset(offsetX.roundToInt(), 0) },
        shape = RoundedCornerShape(CornerExtraSmall),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Text(
            text = formatDuration(timeMs),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
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

/**
 * The bar before there is a waveform: a plain played/unplayed line through the middle.
 *
 * Deliberately the same shape the waveform settles into, so the decode finishing reads as the line
 * gaining detail rather than as one control being swapped for another. It seeks while it is drawn,
 * which is the point of using a line rather than a progress bar — decoding a long track takes
 * seconds, and a bar you cannot scrub for those seconds is worse than no waveform at all.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFlatLine(
    progress: Float,
    played: Color,
    unplayed: Color,
) {
    // Bottom-aligned, matching the bars it grows into once the decode finishes - the line's own
    // baseline is exactly where every bar's bottom edge sits.
    val baselineY = size.height - FLAT_LINE_THICKNESS.toPx() / 2f
    val thickness = FLAT_LINE_THICKNESS.toPx()
    val playedTo = (progress.coerceIn(0f, 1f) * size.width)

    drawLine(
        color = unplayed,
        start = Offset(0f, baselineY),
        end = Offset(size.width, baselineY),
        strokeWidth = thickness,
        cap = StrokeCap.Round,
    )
    if (playedTo > 0f) {
        drawLine(
            color = played,
            start = Offset(0f, baselineY),
            end = Offset(playedTo, baselineY),
            strokeWidth = thickness,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * The scrub tooltip's left edge: centred on [touchX], clamped so it never overflows the track it
 * floats above at either end (§4.2 - "clamped inside the bar's bounds at both ends").
 */
internal fun clampTooltipX(touchX: Float, tooltipWidth: Float, trackWidth: Float): Float {
    val centred = touchX - tooltipWidth / 2f
    return centred.coerceIn(0f, (trackWidth - tooltipWidth).coerceAtLeast(0f))
}

/**
 * Reduces [peaks] to exactly [barCount] bar heights, each the loudest sample in its own span of
 * the source array. Spans are computed as `bar * peaks.size / barCount` .. `(bar+1) * peaks.size /
 * barCount`, the standard "distribute N items across M buckets as evenly as possible" split: every
 * bucket gets at least one item whenever `peaks.size >= barCount` (always true here - a decoded
 * waveform runs to hundreds of samples, never as few as 64), so no span is ever empty and spans
 * never overlap. The last bucket's span end is pinned to `peaks.size` explicitly, stating the
 * guarantee directly rather than leaving it as an implicit property of the arithmetic.
 */
internal fun bucketPeaks(peaks: FloatArray, barCount: Int): FloatArray {
    if (barCount <= 0 || peaks.isEmpty()) return FloatArray(0)
    return FloatArray(barCount) { bar ->
        val from = bar * peaks.size / barCount
        val to = if (bar == barCount - 1) peaks.size else (bar + 1) * peaks.size / barCount
        var loudest = 0f
        for (i in from until to) if (peaks[i] > loudest) loudest = peaks[i]
        loudest
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaveform(
    peaks: FloatArray,
    progress: Float,
    played: Color,
    unplayed: Color,
) {
    if (peaks.isEmpty()) return
    val bars = bucketPeaks(peaks, BAR_COUNT)
    val gapPx = BAR_GAP.toPx()
    val barWidth = (size.width - gapPx * (BAR_COUNT - 1)) / BAR_COUNT
    val cornerPx = BAR_CORNER_RADIUS.toPx()
    val playedUpTo = progress * BAR_COUNT

    bars.forEachIndexed { index, peak ->
        // Always at least a hair tall, so silence still reads as part of the track.
        val barHeight = (peak * size.height).coerceAtLeast(2f)
        val x = index * (barWidth + gapPx)
        drawRoundRect(
            color = if (index < playedUpTo) played else unplayed,
            topLeft = Offset(x, size.height - barHeight),
            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
        )
    }
}
