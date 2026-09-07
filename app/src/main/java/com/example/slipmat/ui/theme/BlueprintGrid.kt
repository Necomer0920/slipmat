package com.example.slipmat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

/** §4.3's hairline grid: 22dp squares, offset -1dp "so the lines meet the edge". */
private const val GRID_SPACING_DP = 22f
private const val GRID_OFFSET_DP = -1f

/** The nth grid line's position along one axis, in dp. n=0 is the -1dp offset line itself. */
internal fun gridLinePosition(
    n: Int,
    spacingDp: Float = GRID_SPACING_DP,
    offsetDp: Float = GRID_OFFSET_DP,
): Float = n * spacingDp + offsetDp

/**
 * The Performance screen's background — a fine grid distinguishing it from Now Playing's flat
 * surface. Drawn in `outlineVariant`, already the design's own low-contrast neutral role (§3.1);
 * no further alpha reduction on top of it.
 */
@Composable
fun Modifier.blueprintGrid(): Modifier {
    val color = MaterialTheme.colorScheme.outlineVariant
    return this.drawBehind {
        val spacingPx = GRID_SPACING_DP.dp.toPx()
        var x = GRID_OFFSET_DP.dp.toPx()
        while (x < size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += spacingPx
        }
        var y = GRID_OFFSET_DP.dp.toPx()
        while (y < size.height) {
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += spacingPx
        }
    }
}
