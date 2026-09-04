package com.example.slipmat.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.slipmat.core.media.SleepTimerState
import java.util.Locale

private val PRESET_MINUTES = listOf(15, 30, 45, 60)

/**
 * Sleep timer: a row of preset durations, replaced by a live countdown once one is running.
 *
 * Presets rather than a free-form picker — the choice is always "roughly how long until I am
 * asleep", which no one answers to the minute.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepTimerControls(
    state: SleepTimerState,
    onStart: (minutes: Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state) {
            is SleepTimerState.Idle -> {
                if (expanded) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PRESET_MINUTES.forEach { minutes ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onStart(minutes)
                                    expanded = false
                                },
                                label = { Text("$minutes min") },
                            )
                        }
                    }
                } else {
                    AssistChip(onClick = { expanded = true }, label = { Text("Sleep timer") })
                }
            }

            is SleepTimerState.Running -> {
                AssistChip(
                    onClick = onCancel,
                    label = {
                        Text(
                            text = if (state.isFading) {
                                "Fading out \u00b7 ${formatCountdown(state.remainingMs)}"
                            } else {
                                "Sleeping in ${formatCountdown(state.remainingMs)}"
                            },
                        )
                    },
                )
                Text(
                    text = "Tap to cancel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** `m:ss`, rounding up so a countdown never shows 0:00 while music is still playing. */
internal fun formatCountdown(remainingMs: Long): String {
    val totalSeconds = ((remainingMs + 999) / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
