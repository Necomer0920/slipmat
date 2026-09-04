package com.example.slipmat.nowplaying

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import android.view.HapticFeedbackConstants
import com.example.slipmat.core.media.PitchRange
import com.example.slipmat.core.media.isAtDetent
import com.example.slipmat.core.media.playingBpm
import com.example.slipmat.core.media.tempoPercent
import java.util.Locale

/**
 * The pitch/tempo control: a slider, a key-lock switch, and a range selector.
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
    onKeyLockChange: (Boolean) -> Unit,
    onRangeChange: (PitchRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    // Remembered so the haptic fires on entering the detent, not on every frame spent inside it.
    val wasAtDetent = remember { mutableStateOf(isAtDetent(sliderValue)) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatTempoPercent(tempoPercent(sliderValue, range)),
                style = MaterialTheme.typography.titleMedium,
            )
            playingBpm(sourceBpm, sliderValue, range)?.let { bpm ->
                Text(
                    text = String.format(Locale.US, "%.1f BPM", bpm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Slider(
            value = sliderValue,
            onValueChange = { raw ->
                val nowAtDetent = isAtDetent(raw)
                // A tick as the slider snaps home, so centre can be found without looking.
                if (nowAtDetent && !wasAtDetent.value) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                wasAtDetent.value = nowAtDetent
                onSliderChange(raw)
            },
            valueRange = -1f..1f,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Key lock",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(checked = keyLock, onCheckedChange = onKeyLockChange)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PitchRange.entries.forEach { option ->
                    FilterChip(
                        selected = option == range,
                        onClick = { onRangeChange(option) },
                        label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }
    }
}

/** Always signed, so `+0.0%` reads as "deliberately normal" rather than "unset". */
internal fun formatTempoPercent(percent: Float): String =
    String.format(Locale.US, "%+.1f%%", percent)
