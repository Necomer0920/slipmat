package com.example.slipmat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.slipmat.ui.theme.CornerLarge

/**
 * §5.8's filled active state (never tint-only) plus §5.11's 48dp touch minimum, shared by every
 * pill-shaped control in the redesign. Each composable here renders *bare*: a caller that wants an
 * outer strip around it (the filter LP/HP toggle's `containerHigh` wrapper, §4.3) adds that itself
 * rather than getting one baked in, since the Performance tab row explicitly does not want one.
 *
 * The colour decisions are pulled into their own functions so they're testable without standing up
 * Compose — a JVM unit test can call [pillContainerColor] etc. directly.
 */
private val TouchMinimum = 48.dp
private val PillPadding = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)

internal fun pillContainerColor(selected: Boolean, primary: Color): Color =
    if (selected) primary else Color.Transparent

internal fun pillContentColor(selected: Boolean, onPrimary: Color, onSurfaceVariant: Color): Color =
    if (selected) onPrimary else onSurfaceVariant

/** Library's Tracks/Artists/Albums/Folders row. */
@Composable
fun PillTab(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = TouchMinimum, minHeight = TouchMinimum)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(CornerLarge),
            color = pillContainerColor(selected, colorScheme.primary),
            contentColor = pillContentColor(selected, colorScheme.onPrimary, colorScheme.onSurfaceVariant),
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, modifier = PillPadding)
        }
    }
}

/**
 * `alwaysFilled` is what an EQ preset chip sets: §5.8 rejects tint-only *changes of state*, but an
 * always-visible container (rather than transparent-when-inactive) is a separate, valid choice a
 * caller can make — one PillTab's transparent-inactive style doesn't fit for a row of chips meant
 * to read as chips even before any of them is picked.
 */
internal fun chipContainerColor(
    selected: Boolean,
    alwaysFilled: Boolean,
    primary: Color,
    surfaceContainerHigh: Color,
): Color = when {
    selected -> primary
    alwaysFilled -> surfaceContainerHigh
    else -> Color.Transparent
}

/** EQ preset chips (Flat / Bass Boost / Vocal / Custom / saved presets). */
@Composable
fun Chip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    alwaysFilled: Boolean = false,
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = TouchMinimum, minHeight = TouchMinimum)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(CornerLarge),
            color = chipContainerColor(selected, alwaysFilled, colorScheme.primary, colorScheme.surfaceContainerHigh),
            contentColor = pillContentColor(selected, colorScheme.onPrimary, colorScheme.onSurfaceVariant),
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, modifier = PillPadding)
        }
    }
}

internal fun segmentContainerColor(selected: Boolean, surfaceContainerHigh: Color): Color =
    if (selected) surfaceContainerHigh else Color.Transparent

internal fun segmentContentColor(selected: Boolean, onSurface: Color, onSurfaceVariant: Color): Color =
    if (selected) onSurface else onSurfaceVariant

/**
 * The filter LP/HP toggle and the Performance Filter/Delay/EQ tab row. Bare — see the file note on
 * outer strips. Each segment fills equally (`Modifier.weight(1f)`), so three or two options both
 * span the available width the same way.
 */
@Composable
fun <T> SegmentedToggle(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(modifier = modifier) {
        for (option in options) {
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = TouchMinimum)
                    .clickable(onClick = { onSelect(option) }),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(CornerLarge),
                    color = segmentContainerColor(isSelected, colorScheme.surfaceContainerHigh),
                    contentColor = segmentContentColor(isSelected, colorScheme.onSurface, colorScheme.onSurfaceVariant),
                ) {
                    Text(text = label(option), style = MaterialTheme.typography.labelLarge, modifier = PillPadding)
                }
            }
        }
    }
}
