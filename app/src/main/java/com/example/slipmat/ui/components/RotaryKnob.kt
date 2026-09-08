package com.example.slipmat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val KNOB_MIN_DEGREES = -135f
private const val KNOB_MAX_DEGREES = 135f

/** Full-scale drag distance (§4.3/Interactions) - a 150px sweep from one end reaches the other. */
internal const val KNOB_DRAG_FULL_SCALE_PX = 150f

/** `value` in `min..max` to the pointer's rotation, degrees clockwise from straight up. */
internal fun rotationForValue(value: Float, min: Float, max: Float): Float {
    val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
    return KNOB_MIN_DEGREES + fraction * (KNOB_MAX_DEGREES - KNOB_MIN_DEGREES)
}

/** The inverse of [rotationForValue]. */
internal fun valueForRotation(rotation: Float, min: Float, max: Float): Float {
    val fraction = ((rotation - KNOB_MIN_DEGREES) / (KNOB_MAX_DEGREES - KNOB_MIN_DEGREES)).coerceIn(0f, 1f)
    return min + fraction * (max - min)
}

/**
 * A vertical drag of `dragUpPx` (positive = finger moved up) from `startValue`, clamped to the
 * knob's range - up increases (§4.3: "Drag vertically (up = increase)").
 */
internal fun valueForDrag(startValue: Float, dragUpPx: Float, min: Float, max: Float): Float =
    (startValue + (dragUpPx / KNOB_DRAG_FULL_SCALE_PX) * (max - min)).coerceIn(min, max)

private val KNOB_SIZE = 76.dp
private val KNOB_BORDER_WIDTH = 2.dp
private val KNOB_POINTER_WIDTH = 3.dp
private val KNOB_POINTER_INSET = 18.dp

/**
 * §4.3's rotary knob primitive: a 76dp ring on `surfaceContainerLow` with a 2dp `outlineVariant`
 * border and a `primary` pointer line rotating −135°…+135° across `min..max`. Bound to a specific
 * range and label by its caller (R3.10's three delay knobs); this composable only knows the knob.
 *
 * [label] names the control for TalkBack (R5.3 - previously carried no semantics at all); the
 * knob's own value/range comes through [androidx.compose.foundation.progressSemantics] in the
 * caller's real units (ms, a feedback fraction, a mix fraction) so an "adjust" gesture's step size
 * makes sense in that domain, the same reasoning R3.12 applied to the EQ handles.
 */
@Composable
fun RotaryKnob(
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val containerLow = MaterialTheme.colorScheme.surfaceContainerLow
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    var dragStartValue by remember { mutableFloatStateOf(value) }
    var accumulatedDragPx by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .size(KNOB_SIZE)
            .progressSemantics(value, min..max)
            .semantics {
                contentDescription = label
                setProgress { target ->
                    onValueChange(target.coerceIn(min, max))
                    true
                }
            }
            .pointerInput(min, max) {
                detectVerticalDragGestures(
                    onDragStart = {
                        dragStartValue = value
                        accumulatedDragPx = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragPx -= dragAmount
                        onValueChange(valueForDrag(dragStartValue, accumulatedDragPx, min, max))
                    },
                )
            },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - KNOB_BORDER_WIDTH.toPx() / 2f

        drawCircle(color = containerLow, radius = radius, center = center)
        drawCircle(color = outlineVariant, radius = radius, center = center, style = Stroke(width = KNOB_BORDER_WIDTH.toPx()))

        val rotationDegrees = rotationForValue(value, min, max)
        val rotationRadians = (rotationDegrees - 90f) * (PI.toFloat() / 180f)
        val pointerStart = radius - KNOB_POINTER_INSET.toPx()
        drawLine(
            color = primary,
            start = center + Offset(cos(rotationRadians), sin(rotationRadians)) * pointerStart,
            end = center + Offset(cos(rotationRadians), sin(rotationRadians)) * radius,
            strokeWidth = KNOB_POINTER_WIDTH.toPx(),
        )
    }
}
