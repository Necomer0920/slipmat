package com.example.slipmat.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.slipmat.core.data.eq.EqPreset
import com.example.slipmat.core.media.dsp.EQ_BANDS
import com.example.slipmat.core.media.dsp.EQ_MAX_GAIN_DB
import com.example.slipmat.core.media.dsp.EqState
import com.example.slipmat.core.media.dsp.curveFractionFor
import com.example.slipmat.ui.components.Chip
import java.util.Locale
import kotlin.math.roundToInt

private val EQ_CURVE_HEIGHT = 160.dp
private val EQ_HANDLE_RADIUS = 8.dp // 16dp handle (§4.3)
private val EQ_HANDLE_RING_WIDTH = 2.dp

/**
 * The multiband EQ (§4.3): a 160dp curve area, a dashed zero line, a smooth cubic through the eight
 * handles, frequency labels from [EQ_BANDS] beneath, and the preset chip row (R3.13). No enable
 * switch on the curve itself - as with the filter and delay panels, §5.1 makes the tab's own state
 * dot the only on/off control, so dragging a handle (or loading a preset) is itself what turns EQ
 * on (R3.6).
 */
@Composable
fun EqControls(
    state: EqState,
    onGainChange: (band: Int, gainDb: Float) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<EqPreset> = emptyList(),
    onSavePreset: (String) -> Unit = {},
    onLoadPreset: (String) -> Unit = {},
    onDeletePreset: (String) -> Unit = {},
) {
    // Which preset the curve currently matches, purely for the chip row's own highlight - not
    // playback state, so it lives here rather than in the view model (R3.13).
    var selectedPresetName by rememberSaveable { mutableStateOf<String?>(null) }
    val onBandDragged: (Int, Float) -> Unit = { band, gainDb ->
        selectedPresetName = null // Custom, the moment any point moves (§4.3).
        onGainChange(band, gainDb)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val curveColor = MaterialTheme.colorScheme.primary
        val ringColor = MaterialTheme.colorScheme.surface
        val gridColor = MaterialTheme.colorScheme.onSurfaceVariant
        val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

        Box(modifier = Modifier.fillMaxWidth().height(EQ_CURVE_HEIGHT)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // The down event is consumed immediately, which claims the gesture before
                        // the scrolling column this sits in can take it. The cost is that a drag
                        // started on the curve will not scroll the page — the right trade for a
                        // control whose whole purpose is being dragged.
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val band = nearestBand(down.position.x, size.width.toFloat())
                            onBandDragged(band, snappedDbForY(down.position.y, size.height.toFloat()))

                            drag(down.id) { change ->
                                change.consume()
                                onBandDragged(
                                    band,
                                    snappedDbForY(change.position.y, size.height.toFloat()),
                                )
                            }
                        }
                    },
            ) {
                val handlePoints = EQ_BANDS.indices.map { band ->
                    Offset(
                        x = size.width * curveFractionFor(EQ_BANDS[band]),
                        y = yForDb(state.gainsDb.getOrElse(band) { 0f }, size.height),
                    )
                }
                drawZeroLine(gridColor)
                drawSmoothCurve(handlePoints, curveColor)
                drawHandles(handlePoints, curveColor, ringColor)
            }

            // Semantics only - no gesture handling of its own, so the Canvas above keeps doing
            // the actual dragging (verified on device in R3.11). This is what makes each handle
            // announce and adjust via TalkBack's "Adjust" gesture without a physical drag (§5.11).
            EqHandleSemantics(gainsDb = state.gainsDb, onGainChange = onBandDragged)
        }

        EqBandLabels(color = labelColor)

        EqPresetChips(
            selectedPresetName = selectedPresetName,
            presets = presets,
            onLoad = { name ->
                selectedPresetName = name
                onLoadPreset(name)
            },
            onSave = onSavePreset,
            onDelete = onDeletePreset,
        )
    }
}

/**
 * Frequency labels from [EQ_BANDS] (§5.10), one under each handle. A custom [Layout] rather than a
 * plain `Row` because the bands are log-spaced, not even - each label's centre must land on the
 * same x [curveFractionFor] puts its handle at, not on a fraction of the row's child count.
 */
@Composable
private fun EqBandLabels(color: Color, modifier: Modifier = Modifier) {
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            for (hz in EQ_BANDS) {
                Text(text = formatEqBandLabel(hz), style = MaterialTheme.typography.labelSmall, color = color)
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                val fraction = curveFractionFor(EQ_BANDS[index])
                val x = (fraction * constraints.maxWidth - placeable.width / 2f)
                    .roundToInt()
                    .coerceIn(0, constraints.maxWidth - placeable.width)
                placeable.placeRelative(x, 0)
            }
        }
    }
}

private val EQ_HANDLE_TOUCH_SIZE = 48.dp // §5.11's touch minimum, well past the 16dp visual handle.
private const val EQ_HANDLE_STEPS = 23 // Whole-dB values from -12 to 12: 25 values, 23 between the ends.

/**
 * One accessibility node per handle (§5.11/R3.12), positioned at the same [curveFractionFor]/
 * [yForDb] coordinates the Canvas draws its dot at. `setProgress` is what makes a handle operable
 * without dragging - TalkBack's own "Adjust" gesture calls it with a target value already stepped
 * to the nearest whole dB, so [snappedDbForY]'s rounding only need happen once, in the fallback
 * `coerceIn`/`roundToInt` here for safety against out-of-range callers.
 */
@Composable
private fun EqHandleSemantics(
    gainsDb: List<Float>,
    onGainChange: (band: Int, gainDb: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            for (band in EQ_BANDS.indices) {
                val gainDb = gainsDb.getOrElse(band) { 0f }
                Box(
                    modifier = Modifier
                        .size(EQ_HANDLE_TOUCH_SIZE)
                        .progressSemantics(gainDb, -EQ_MAX_GAIN_DB..EQ_MAX_GAIN_DB, steps = EQ_HANDLE_STEPS)
                        .semantics {
                            contentDescription = "${formatEqBandLabel(EQ_BANDS[band])} Hz band gain"
                            setProgress { target ->
                                onGainChange(band, target.roundToInt().toFloat().coerceIn(-EQ_MAX_GAIN_DB, EQ_MAX_GAIN_DB))
                                true
                            }
                        },
                )
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val xFraction = curveFractionFor(EQ_BANDS[index])
                val x = (xFraction * constraints.maxWidth - placeable.width / 2f).roundToInt()
                val y = (yForDb(gainsDb.getOrElse(index) { 0f }, constraints.maxHeight.toFloat()) - placeable.height / 2f).roundToInt()
                placeable.placeRelative(x, y)
            }
        }
    }
}

/** "1.5k"/"7k", never "1.5000k" or "7.0k" - a trailing `.0` reads as a number someone typed. */
internal fun formatEqBandLabel(hz: Float): String {
    if (hz < 1000f) return hz.roundToInt().toString()
    val k = hz / 1000f
    return if (k == k.roundToInt().toFloat()) {
        "${k.roundToInt()}k"
    } else {
        String.format(Locale.US, "%.1fk", k)
    }
}

/**
 * The preset row (§4.3/R3.13): Flat, Bass Boost, Vocal, then Custom, then saved presets, then a
 * chip that opens the name field to save the curve as it stands - R0.8's own listed order for this
 * exact row. `selectedPresetName == null` is what makes Custom the highlighted one; it carries no
 * `onClick` of its own; there is nothing to switch it to.
 */
@Composable
private fun EqPresetChips(
    selectedPresetName: String?,
    presets: List<EqPreset>,
    onLoad: (String) -> Unit,
    onSave: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSaveField by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (builtIn in BUILT_IN_EQ_PRESETS) {
                Chip(
                    selected = selectedPresetName == builtIn.name,
                    onClick = { onLoad(builtIn.name) },
                    label = builtIn.name,
                    alwaysFilled = true,
                )
            }
            Chip(selected = selectedPresetName == null, onClick = {}, label = "Custom", alwaysFilled = true, enabled = false)
            for (preset in presets) {
                Chip(
                    selected = selectedPresetName == preset.name,
                    onClick = { onLoad(preset.name) },
                    label = preset.name,
                    alwaysFilled = true,
                    trailing = {
                        // §5.11: visual size is the design's (14dp); touch size is at least 48dp -
                        // the icon alone was the whole touch target before (R5.4).
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clickable { onDelete(preset.name) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Delete ${preset.name}",
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    },
                )
            }
            Chip(
                selected = showSaveField,
                onClick = { showSaveField = !showSaveField },
                label = "+ Save current",
                alwaysFilled = true,
            )
        }

        if (showSaveField) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save(name, onSave) { name = ""; showSaveField = false } }),
                )
                TextButton(
                    onClick = { save(name, onSave) { name = ""; showSaveField = false } },
                    // A blank name saves a preset that cannot be loaded or deleted from a list of names.
                    enabled = name.isNotBlank(),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

private inline fun save(name: String, onSave: (String) -> Unit, clear: () -> Unit) {
    if (name.isBlank()) return
    onSave(name)
    clear()
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

/**
 * A smooth curve through the handle points (§4.3: "a smooth cubic through all eight points"),
 * decorative rather than the cascade's literal response - a uniform Catmull-Rom spline converted to
 * cubic Bezier segments, the standard technique for a curve that passes through every point rather
 * than merely being shaped by them.
 */
private fun DrawScope.drawSmoothCurve(points: List<Offset>, color: Color) {
    if (points.size < 2) return
    val path = Path().apply { moveTo(points[0].x, points[0].y) }
    for (i in 0 until points.size - 1) {
        val p0 = points.getOrElse(i - 1) { points[i] }
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points.getOrElse(i + 2) { points[i + 1] }
        val control1 = p1 + (p2 - p0) / 6f
        val control2 = p2 - (p3 - p1) / 6f
        path.cubicTo(control1.x, control1.y, control2.x, control2.y, p2.x, p2.y)
    }
    drawPath(path = path, color = color, style = Stroke(width = 3f))
}

private fun DrawScope.drawHandles(points: List<Offset>, fill: Color, ring: Color) {
    for (point in points) {
        drawCircle(color = fill, radius = EQ_HANDLE_RADIUS.toPx(), center = point)
        drawCircle(
            color = ring,
            radius = EQ_HANDLE_RADIUS.toPx() - EQ_HANDLE_RING_WIDTH.toPx() / 2f,
            center = point,
            style = Stroke(width = EQ_HANDLE_RING_WIDTH.toPx()),
        )
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
internal fun dbForY(y: Float, height: Float): Float {
    if (height <= 0f) return 0f
    val fraction = (1f - (y / height).coerceIn(0f, 1f))
    return fraction * 2f * EQ_MAX_GAIN_DB - EQ_MAX_GAIN_DB
}

/**
 * [dbForY], snapped to the nearest whole decibel (R3.12) - kept separate from [dbForY] itself so
 * the continuous mapping stays available (and stays what [EqControlsTest]'s round-trip case checks)
 * for anything that wants the unrounded value.
 */
internal fun snappedDbForY(y: Float, height: Float): Float =
    dbForY(y, height).roundToInt().toFloat().coerceIn(-EQ_MAX_GAIN_DB, EQ_MAX_GAIN_DB)

/** Maps decibels to a y coordinate: +[EQ_MAX_GAIN_DB] at the top, zero through the middle. */
internal fun yForDb(db: Float, height: Float): Float {
    val fraction = (db.coerceIn(-EQ_MAX_GAIN_DB, EQ_MAX_GAIN_DB) + EQ_MAX_GAIN_DB) /
        (2f * EQ_MAX_GAIN_DB)
    return height * (1f - fraction)
}
