package com.example.slipmat.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.slipmat.core.media.dsp.FilterMode
import com.example.slipmat.core.media.dsp.FilterState
import com.example.slipmat.core.media.dsp.sliderToFrequency
import java.util.Locale

/**
 * The sweepable filter: on/off, a cutoff slider, and which way it cuts.
 *
 * The slider is logarithmic (see `sliderToFrequency`) so a sweep sounds even rather than spending
 * most of its travel above anything audible.
 */
@Composable
fun FilterControls(
    state: FilterState,
    onEnabledChange: (Boolean) -> Unit,
    onCutoffChange: (Float) -> Unit,
    onModeChange: (FilterMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "Filter", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = state.enabled, onCheckedChange = onEnabledChange)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = state.mode == FilterMode.LowPass,
                    onClick = { onModeChange(FilterMode.LowPass) },
                    enabled = state.enabled,
                    label = { Text("Low", style = MaterialTheme.typography.labelSmall) },
                )
                FilterChip(
                    selected = state.mode == FilterMode.HighPass,
                    onClick = { onModeChange(FilterMode.HighPass) },
                    enabled = state.enabled,
                    label = { Text("High", style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        // Hidden when off, so the screen does not carry a dead control.
        AnimatedVisibility(visible = state.enabled) {
            Column {
                Text(
                    text = formatCutoff(state.cutoffHz),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = state.sliderPosition,
                    onValueChange = { position -> onCutoffChange(sliderToFrequency(position)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Hz below a kilohertz, kHz above — how the number is normally spoken. */
internal fun formatCutoff(hz: Float): String = when {
    hz >= 1000f -> String.format(Locale.US, "%.1f kHz", hz / 1000f)
    else -> String.format(Locale.US, "%.0f Hz", hz)
}
