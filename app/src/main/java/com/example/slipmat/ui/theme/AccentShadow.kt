package com.example.slipmat.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The coloured halo under the play button — `0 8px 20px -6px primary` in §3.4's own CSS shorthand.
 * Reads from `colorScheme.primary` rather than a fixed colour, so it tints with whichever record
 * is playing.
 *
 * ⚠️ `Modifier.dropShadow`/`Shadow` — the literal-spread API this shorthand maps onto most
 * directly — exists in this project's pinned Compose BOM (2026.02.01) but its constructor is
 * `internal`, unusable from `:app`. Approximated instead through `Modifier.shadow`'s elevation
 * model, which has no spread parameter but does expose `ambientColor`/`spotColor` for the tint.
 */
@Composable
fun Modifier.accentShadow(shape: Shape = CircleShape): Modifier {
    val primary = MaterialTheme.colorScheme.primary
    return this.shadow(
        elevation = 20.dp,
        shape = shape,
        ambientColor = primary,
        spotColor = primary,
    )
}
