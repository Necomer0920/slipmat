package com.example.slipmat.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.slipmat.core.media.SleepTimerState
import java.util.Locale

/**
 * A compact countdown, visible on the main screen for as long as a sleep timer runs (R4.3) -
 * picking or changing the duration now happens in the popover under the header's sleep-timer
 * button, but that popover closes the moment a choice is made, so this is what keeps the running
 * timer visible without reopening it. Renders nothing while idle.
 */
@Composable
fun SleepTimerControls(
    state: SleepTimerState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state !is SleepTimerState.Running) return

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            onClick = onCancel,
            label = {
                Text(
                    text = if (state.isFading) {
                        "Fading out · ${formatCountdown(state.remainingMs)}"
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

/** `m:ss`, rounding up so a countdown never shows 0:00 while music is still playing. */
internal fun formatCountdown(remainingMs: Long): String {
    val totalSeconds = ((remainingMs + 999) / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}
