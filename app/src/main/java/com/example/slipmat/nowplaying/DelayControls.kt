package com.example.slipmat.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.slipmat.core.media.dsp.DelayState
import com.example.slipmat.core.media.dsp.sliderToDelayMs
import com.example.slipmat.core.media.dsp.sliderToFeedback
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The delay: on/off, then time, feedback and mix.
 *
 * Three sliders rather than a list of presets, because the point of the effect here is moving them
 * while a track plays.
 */
@Composable
fun DelayControls(
    state: DelayState,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (Float) -> Unit,
    onFeedbackChange: (Float) -> Unit,
    onMixChange: (Float) -> Unit,
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
            Text(text = "Delay", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = state.enabled, onCheckedChange = onEnabledChange)
        }

        // Hidden when off, so the screen does not carry three dead controls.
        AnimatedVisibility(visible = state.enabled) {
            Column {
                LabelledSlider(
                    label = "Time",
                    value = formatMillis(state.timeMs),
                    position = state.timeSlider,
                    onPositionChange = { onTimeChange(sliderToDelayMs(it)) },
                )
                LabelledSlider(
                    label = "Feedback",
                    value = formatPercent(state.feedbackSlider),
                    position = state.feedbackSlider,
                    onPositionChange = { onFeedbackChange(sliderToFeedback(it)) },
                )
                LabelledSlider(
                    label = "Mix",
                    value = formatPercent(state.mix),
                    position = state.mix,
                    onPositionChange = onMixChange,
                )
            }
        }
    }
}

@Composable
private fun LabelledSlider(
    label: String,
    value: String,
    position: Float,
    onPositionChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Slider(
        value = position,
        onValueChange = onPositionChange,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Milliseconds below a second, seconds above — how a delay time is normally spoken. */
internal fun formatMillis(ms: Float): String = when {
    ms >= 1000f -> String.format(Locale.US, "%.2f s", ms / 1000f)
    else -> String.format(Locale.US, "%d ms", ms.roundToInt())
}

internal fun formatPercent(fraction: Float): String =
    String.format(Locale.US, "%d%%", (fraction * 100f).roundToInt())
