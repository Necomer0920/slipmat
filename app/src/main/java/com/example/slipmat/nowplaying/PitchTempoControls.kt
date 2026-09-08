package com.example.slipmat.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import android.view.HapticFeedbackConstants
import com.example.slipmat.core.media.PitchRange
import com.example.slipmat.core.media.isAtDetent
import com.example.slipmat.core.media.playingBpm
import com.example.slipmat.core.media.semitonesFor
import com.example.slipmat.core.media.sliderValueForPercent
import com.example.slipmat.core.media.tempoPercent
import com.example.slipmat.ui.theme.CornerLarge
import com.example.slipmat.ui.theme.CornerTempoCard
import java.util.Locale

private val KeyLockPillPadding = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)

/**
 * The tempo card (§4.2): a containerLow card, the TEMPO eyebrow with its percentage and speed
 * multiplier, a key-lock pill, and the slider - over the fixed ±30% range (§5.2), no control
 * offers a way to choose a different one any more.
 *
 * Stateless so it can be previewed and screenshot-tested; the caller owns the slider position.
 */
@Composable
fun PitchTempoControls(
    sliderValue: Float,
    keyLock: Boolean,
    range: PitchRange,
    sourceBpm: Float?,
    onSliderChange: (Float) -> Unit,
    onSliderChangeFinished: () -> Unit,
    onKeyLockChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showStatusLine: Boolean = true,
) {
    val view = LocalView.current
    // Remembered so the haptic fires on entering the detent, not on every frame spent inside it.
    val wasAtDetent = remember { mutableStateOf(isAtDetent(sliderValue)) }
    val percent = tempoPercent(sliderValue, range)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerTempoCard),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "TEMPO",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatTempoPercent(percent),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = formatSpeedMultiplier(percent),
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Not part of the redesign's own tempo card, but existing functionality
                    // neither source addresses either way - kept, not dropped, just relocated
                    // out of the header row the key-lock pill now occupies.
                    playingBpm(sourceBpm, sliderValue, range)?.let { bpm ->
                        Text(
                            text = String.format(Locale.US, "%.1f BPM", bpm),
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                KeyLockPill(locked = keyLock, onClick = { onKeyLockChange(!keyLock) })
            }

            TempoTrack(
                sliderValue = sliderValue,
                range = range,
                onValueChange = { raw ->
                    val nowAtDetent = isAtDetent(raw)
                    // A tick as the slider snaps home, so centre can be found without looking.
                    if (nowAtDetent && !wasAtDetent.value) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    wasAtDetent.value = nowAtDetent
                    onSliderChange(raw)
                },
                onValueChangeFinished = onSliderChangeFinished,
                modifier = Modifier.fillMaxWidth(),
            )

            // §5.4's third pressure-absorption step: dropped on a short screen once shrinking the
            // artwork and collapsing the door haven't freed enough room on their own. Purely
            // descriptive of a state the key-lock pill itself already shows, so nothing is lost
            // that isn't recoverable by looking at the pill.
            if (showStatusLine) {
                Text(
                    text = if (keyLock) {
                        "Key holds while tempo changes"
                    } else {
                        "Pitch follows tempo · ${formatSemitones(semitonesFor(percent))}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val TEMPO_TRACK_TOUCH_HEIGHT = 48.dp
private val TEMPO_RAIL_HEIGHT = 4.dp
private val TEMPO_THUMB_SIZE = 20.dp
private val TEMPO_DETENT_WIDTH = 2.dp
private val TEMPO_DETENT_HEIGHT = 12.dp
private const val TEMPO_FILL_ALPHA = 0.55f

/**
 * §4.2's tempo track: a 4dp rail, a centre detent mark, a 55%-opacity fill running from centre to
 * the thumb rather than from either end, and a 20dp thumb. Custom-drawn rather than a styled M3
 * `Slider` - the centre-anchored fill and detent tick have no equivalent in `SliderDefaults`.
 *
 * Carries [progressSemantics] in real percent (R5.3, found during the same sweep that fixed the
 * filter cutoff and delay knobs - this track had the identical bare-`Canvas`-with-no-semantics
 * gap), converting back through [sliderValueForPercent] since this composable otherwise only knows
 * the raw -1f..1f slider value, not the signed percentage [range] maps it to.
 */
@Composable
private fun TempoTrack(
    sliderValue: Float,
    range: PitchRange,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val rail = MaterialTheme.colorScheme.surfaceContainerHighest
    val detentColor = MaterialTheme.colorScheme.outline

    fun valueAt(x: Float, width: Float): Float =
        ((x / width) * 2f - 1f).coerceIn(-1f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(TEMPO_TRACK_TOUCH_HEIGHT)
            .progressSemantics(tempoPercent(sliderValue, range), -range.percent..range.percent)
            .semantics {
                contentDescription = "Tempo"
                setProgress { target ->
                    onValueChange(sliderValueForPercent(target, range))
                    onValueChangeFinished()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onValueChange(valueAt(offset.x, size.width.toFloat()))
                    onValueChangeFinished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> onValueChange(valueAt(offset.x, size.width.toFloat())) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onValueChange(valueAt(change.position.x, size.width.toFloat()))
                    },
                    onDragEnd = onValueChangeFinished,
                    onDragCancel = onValueChangeFinished,
                )
            },
    ) {
        val centerY = size.height / 2f
        val railTopLeft = Offset(0f, centerY - TEMPO_RAIL_HEIGHT.toPx() / 2f)
        val railSize = Size(size.width, TEMPO_RAIL_HEIGHT.toPx())
        val railRadius = CornerRadius(TEMPO_RAIL_HEIGHT.toPx() / 2f)

        drawRoundRect(color = rail, topLeft = railTopLeft, size = railSize, cornerRadius = railRadius)

        val centerX = size.width / 2f
        val thumbX = ((sliderValue.coerceIn(-1f, 1f) + 1f) / 2f) * size.width
        val fillLeft = minOf(centerX, thumbX)
        val fillRight = maxOf(centerX, thumbX)
        if (fillRight > fillLeft) {
            drawRoundRect(
                color = primary.copy(alpha = TEMPO_FILL_ALPHA),
                topLeft = Offset(fillLeft, railTopLeft.y),
                size = Size(fillRight - fillLeft, railSize.height),
                cornerRadius = railRadius,
            )
        }

        drawRect(
            color = detentColor,
            topLeft = Offset(centerX - TEMPO_DETENT_WIDTH.toPx() / 2f, centerY - TEMPO_DETENT_HEIGHT.toPx() / 2f),
            size = Size(TEMPO_DETENT_WIDTH.toPx(), TEMPO_DETENT_HEIGHT.toPx()),
        )

        drawCircle(color = primary, radius = TEMPO_THUMB_SIZE.toPx() / 2f, center = Offset(thumbX, centerY))
    }
}

/**
 * §4.2's key-lock pill: icon + "Key lock"/"Key free", filling with primary when locked. Uses the
 * stock Lock/LockOpen glyphs rather than the mock's bespoke split-padlock icon (§5.9 keeps
 * material-icons-extended for anything with a stock equivalent close enough to it).
 */
@Composable
private fun KeyLockPill(locked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(CornerLarge),
            color = if (locked) colorScheme.primary else Color.Transparent,
            contentColor = if (locked) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
        ) {
            Row(
                modifier = KeyLockPillPadding,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = if (locked) "Key lock" else "Key free",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** Always signed, so `+0.0%` reads as "deliberately normal" rather than "unset". */
internal fun formatTempoPercent(percent: Float): String =
    String.format(Locale.US, "%+.1f%%", percent)

/** The speed multiplier the percentage corresponds to, e.g. `1.08×` at +8%. */
internal fun formatSpeedMultiplier(percent: Float): String =
    String.format(Locale.US, "%.2f×", 1f + percent / 100f)

/** Always signed, matching [formatTempoPercent]'s convention. */
internal fun formatSemitones(semitones: Float): String =
    String.format(Locale.US, "%+.1f st", semitones)
