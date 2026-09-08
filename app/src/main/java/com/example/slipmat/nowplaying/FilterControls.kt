package com.example.slipmat.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.example.slipmat.core.media.dsp.FILTER_MAX_HZ
import com.example.slipmat.core.media.dsp.FILTER_MIN_HZ
import com.example.slipmat.core.media.dsp.FilterMode
import com.example.slipmat.core.media.dsp.FilterState
import com.example.slipmat.core.media.dsp.frequencyToSlider
import com.example.slipmat.core.media.dsp.sliderToFrequency
import com.example.slipmat.ui.components.SegmentedToggle
import java.util.Locale

/**
 * The sweepable filter (§4.3): a centred LP/HP toggle, a 26/800 cutoff readout, a full-width sweep
 * slider, and scale labels read off the engine. No enable switch here - §5.1 settles on exactly one
 * on/off control for an effect, the tab's own state dot, so this panel is always fully interactive
 * and touching the slider or the toggle is itself what turns the filter on (R3.6).
 */
@Composable
fun FilterControls(
    state: FilterState,
    onCutoffChange: (Float) -> Unit,
    onModeChange: (FilterMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SegmentedToggle(
            options = listOf(FilterMode.LowPass, FilterMode.HighPass),
            selected = state.mode,
            onSelect = onModeChange,
            label = { mode -> if (mode == FilterMode.LowPass) "Low-pass" else "High-pass" },
            modifier = Modifier.width(240.dp),
        )

        FilterResponseCurve(cutoffPosition = state.sliderPosition, mode = state.mode)

        Text(text = formatCutoff(state.cutoffHz), style = MaterialTheme.typography.displayMedium)

        FilterCutoffTrack(
            position = state.sliderPosition,
            onValueChange = { position -> onCutoffChange(sliderToFrequency(position)) },
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatCutoff(FILTER_MIN_HZ), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
            Text(formatCutoff(sliderToFrequency(0.5f)), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
            Text(formatCutoff(FILTER_MAX_HZ), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
        }
    }
}

private val FILTER_TRACK_TOUCH_HEIGHT = 48.dp
private val FILTER_RAIL_HEIGHT = 4.dp
private val FILTER_THUMB_SIZE = 20.dp
private const val FILTER_FILL_ALPHA = 0.55f

/**
 * A 0f..1f sweep, custom-drawn like [PitchTempoControls]'s tempo track (§3.4) rather than a styled
 * M3 `Slider` - the fill running from the rail's own start, not its centre, is the one visible
 * difference between the two.
 *
 * Carries [progressSemantics] in real Hz (R5.3 - previously no semantics at all, so TalkBack had no
 * way to read or adjust the cutoff), converting back through [frequencyToSlider] since this
 * composable otherwise only knows the 0f..1f track position, not the frequency curve.
 */
@Composable
private fun FilterCutoffTrack(
    position: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val rail = MaterialTheme.colorScheme.surfaceContainerHighest

    fun valueAt(x: Float, width: Float): Float = (x / width).coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(FILTER_TRACK_TOUCH_HEIGHT)
            .progressSemantics(sliderToFrequency(position), FILTER_MIN_HZ..FILTER_MAX_HZ)
            .semantics {
                contentDescription = "Filter cutoff"
                setProgress { target ->
                    onValueChange(frequencyToSlider(target))
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset -> onValueChange(valueAt(offset.x, size.width.toFloat())) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> onValueChange(valueAt(offset.x, size.width.toFloat())) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onValueChange(valueAt(change.position.x, size.width.toFloat()))
                    },
                )
            },
    ) {
        val centerY = size.height / 2f
        val railTopLeft = Offset(0f, centerY - FILTER_RAIL_HEIGHT.toPx() / 2f)
        val railSize = Size(size.width, FILTER_RAIL_HEIGHT.toPx())
        val railRadius = CornerRadius(FILTER_RAIL_HEIGHT.toPx() / 2f)

        drawRoundRect(color = rail, topLeft = railTopLeft, size = railSize, cornerRadius = railRadius)

        val thumbX = position.coerceIn(0f, 1f) * size.width
        if (thumbX > 0f) {
            drawRoundRect(
                color = primary.copy(alpha = FILTER_FILL_ALPHA),
                topLeft = railTopLeft,
                size = Size(thumbX, railSize.height),
                cornerRadius = railRadius,
            )
        }

        drawCircle(color = primary, radius = FILTER_THUMB_SIZE.toPx() / 2f, center = Offset(thumbX, centerY))
    }
}

private val FILTER_CURVE_HEIGHT = 56.dp
private const val FILTER_CURVE_FLOOR = 0.08f
private const val FILTER_CURVE_POINT_COUNT = 48

/**
 * Decorative response points (§4.3), not a real dB curve - only the shape needs to be right, since
 * nothing but a stroked line renders from it. `y` is a plain 0f (fully cut) .. 1f (fully passed)
 * amount; the corner sits exactly at `cutoffPosition`, and it never quite reaches zero so the line
 * stays visible even fully swept to one edge.
 */
internal fun filterResponseCurve(
    cutoffPosition: Float,
    mode: FilterMode,
    pointCount: Int = FILTER_CURVE_POINT_COUNT,
): List<Float> {
    val cutoff = cutoffPosition.coerceIn(0f, 1f)
    return List(pointCount) { i ->
        val x = i / (pointCount - 1).toFloat()
        val passed = when (mode) {
            FilterMode.LowPass -> if (x <= cutoff) 1f else 1f - (x - cutoff) / (1f - cutoff).coerceAtLeast(0.0001f)
            FilterMode.HighPass -> if (x >= cutoff) 1f else x / cutoff.coerceAtLeast(0.0001f)
        }
        FILTER_CURVE_FLOOR + passed.coerceIn(0f, 1f) * (1f - FILTER_CURVE_FLOOR)
    }
}

/** The response curve's shape follows [filterResponseCurve]; only the stroke colour lives here. */
@Composable
private fun FilterResponseCurve(cutoffPosition: Float, mode: FilterMode, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val points = filterResponseCurve(cutoffPosition, mode)

    Canvas(modifier = modifier.fillMaxWidth().height(FILTER_CURVE_HEIGHT)) {
        val path = Path()
        points.forEachIndexed { i, y ->
            val x = i / (points.size - 1).toFloat() * size.width
            val plotY = (1f - y) * size.height
            if (i == 0) path.moveTo(x, plotY) else path.lineTo(x, plotY)
        }
        drawPath(path, color = primary, style = Stroke(width = 2.dp.toPx()))
    }
}

/** Hz below a kilohertz, kHz above — how the number is normally spoken. */
internal fun formatCutoff(hz: Float): String = when {
    hz >= 1000f -> String.format(Locale.US, "%.1f kHz", hz / 1000f)
    else -> String.format(Locale.US, "%.0f Hz", hz)
}
