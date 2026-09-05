package com.example.slipmat.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.slipmat.core.media.dsp.EQ_BANDS
import com.example.slipmat.core.media.dsp.EQ_MAX_GAIN_DB
import com.example.slipmat.core.media.dsp.EqState
import com.example.slipmat.core.media.dsp.curveFractionFor
import com.example.slipmat.core.media.dsp.eqCurveDb

/**
 * The rate the curve is *drawn* at, which need not be the rate being played.
 *
 * A peaking band's shape barely moves between 44.1 and 48 kHz, and the alternative is plumbing the
 * stream's format up to the UI so a line can be a fraction of a pixel different.
 */
private const val CURVE_SAMPLE_RATE = 44_100

/** Points along the curve. Enough to look smooth; far fewer than the pixels it is stretched over. */
private const val CURVE_POINTS = 160

/**
 * The multiband EQ: a switch and the response curve.
 *
 * The curve is the cascade's **actual** response, not a line drawn through the handles. The handles
 * sit at the gain each band was given, so where two neighbours are both boosted the curve rides
 * above them — which is what is really happening, and what every EQ that draws a curve shows.
 */
@Composable
fun EqControls(
    state: EqState,
    onEnabledChange: (Boolean) -> Unit,
    onGainChange: (band: Int, gainDb: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "EQ", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = state.enabled, onCheckedChange = onEnabledChange)
        }

        AnimatedVisibility(visible = state.enabled) {
            val curve = remember(state.gainsDb) {
                eqCurveDb(state.gains(), CURVE_SAMPLE_RATE, CURVE_POINTS)
            }
            val curveColor = MaterialTheme.colorScheme.primary
            val gridColor = MaterialTheme.colorScheme.onSurfaceVariant

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .pointerInput(Unit) {
                        // The down event is consumed immediately, which claims the gesture before
                        // the scrolling column this sits in can take it. The cost is that a drag
                        // started on the curve will not scroll the page — the right trade for a
                        // control whose whole purpose is being dragged.
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val band = nearestBand(down.position.x, size.width.toFloat())
                            onGainChange(band, dbForY(down.position.y, size.height.toFloat()))

                            drag(down.id) { change ->
                                change.consume()
                                onGainChange(
                                    band,
                                    dbForY(change.position.y, size.height.toFloat()),
                                )
                            }
                        }
                    },
            ) {
                drawZeroLine(gridColor)
                drawCurve(curve, curveColor)
                drawHandles(state.gainsDb, curveColor)
            }
        }
    }
}

private fun DrawScope.drawZeroLine(color: Color) {
    val y = size.height / 2f
    drawLine(
        color = color.copy(alpha = 0.4f),
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
    )
}

private fun DrawScope.drawCurve(curveDb: FloatArray, color: Color) {
    if (curveDb.size < 2) return

    val path = Path()
    for (point in curveDb.indices) {
        val x = size.width * point / (curveDb.size - 1)
        val y = yForDb(curveDb[point])
        if (point == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path = path, color = color, style = Stroke(width = 3f))
}

private fun DrawScope.drawHandles(gainsDb: List<Float>, color: Color) {
    for (band in EQ_BANDS.indices) {
        val x = size.width * curveFractionFor(EQ_BANDS[band])
        val y = yForDb(gainsDb.getOrElse(band) { 0f })
        drawCircle(color = color, radius = 7f, center = Offset(x, y))
    }
}

/**
 * Which band a touch belongs to: the nearest centre along the drawn axis.
 *
 * Chosen once, when the finger goes down, and held for the whole drag. Re-choosing per movement
 * would hand the drag to a neighbour as soon as it crossed the midpoint, so a diagonal gesture
 * would smear a boost across several bands instead of setting the one that was grabbed.
 */
private fun nearestBand(x: Float, width: Float): Int {
    if (width <= 0f) return 0
    val fraction = (x / width).coerceIn(0f, 1f)
    return EQ_BANDS.indices.minBy { band -> kotlin.math.abs(curveFractionFor(EQ_BANDS[band]) - fraction) }
}

/** The inverse of [yForDb], for turning a finger position back into a gain. */
private fun dbForY(y: Float, height: Float): Float {
    if (height <= 0f) return 0f
    val fraction = (1f - (y / height).coerceIn(0f, 1f))
    return fraction * 2f * EQ_MAX_GAIN_DB - EQ_MAX_GAIN_DB
}

/** Maps decibels to a y coordinate: +[EQ_MAX_GAIN_DB] at the top, zero through the middle. */
private fun DrawScope.yForDb(db: Float): Float {
    val fraction = (db.coerceIn(-EQ_MAX_GAIN_DB, EQ_MAX_GAIN_DB) + EQ_MAX_GAIN_DB) /
        (2f * EQ_MAX_GAIN_DB)
    return size.height * (1f - fraction)
}
