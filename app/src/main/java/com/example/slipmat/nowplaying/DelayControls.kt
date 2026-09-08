package com.example.slipmat.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.slipmat.core.media.dsp.DelayState
import com.example.slipmat.core.media.dsp.MAX_DELAY_MS
import com.example.slipmat.core.media.dsp.MAX_FEEDBACK
import com.example.slipmat.core.media.dsp.MIN_DELAY_MS
import com.example.slipmat.ui.components.RotaryKnob
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The delay (§4.3): three rotary knobs, Time/Feedback/Mix, each bound to the engine's own range.
 * No enable switch - as with the filter panel, §5.1 makes the tab's own state dot the only on/off
 * control, so turning any knob is itself what turns delay on (R3.6).
 */
@Composable
fun DelayControls(
    state: DelayState,
    onTimeChange: (Float) -> Unit,
    onFeedbackChange: (Float) -> Unit,
    onMixChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        LabelledKnob(
            label = "Time",
            value = formatMillis(state.timeMs),
            knobValue = state.timeMs,
            min = MIN_DELAY_MS,
            max = MAX_DELAY_MS,
            onValueChange = onTimeChange,
        )
        LabelledKnob(
            label = "Feedback",
            value = formatPercent(state.feedbackSlider),
            knobValue = state.feedback,
            min = 0f,
            max = MAX_FEEDBACK,
            onValueChange = onFeedbackChange,
        )
        LabelledKnob(
            label = "Mix",
            value = formatPercent(state.mix),
            knobValue = state.mix,
            min = 0f,
            max = 1f,
            onValueChange = onMixChange,
        )
    }
}

@Composable
private fun LabelledKnob(
    label: String,
    value: String,
    knobValue: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RotaryKnob(value = knobValue, min = min, max = max, onValueChange = onValueChange, label = label)
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Milliseconds below a second, seconds above — how a delay time is normally spoken. */
internal fun formatMillis(ms: Float): String = when {
    ms >= 1000f -> String.format(Locale.US, "%.2f s", ms / 1000f)
    else -> String.format(Locale.US, "%d ms", ms.roundToInt())
}

internal fun formatPercent(fraction: Float): String =
    String.format(Locale.US, "%d%%", (fraction * 100f).roundToInt())
