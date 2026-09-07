package com.example.slipmat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Bare palettes for now — R0.4 wires §3.1's fixed surface ramps (Color.kt) into these.
private val DarkColorScheme = darkColorScheme()
private val LightColorScheme = lightColorScheme()

/** Long enough to read as a deliberate change of mood, short enough not to feel slow. */
private const val RETINT_MS = 600

/**
 * @param artworkSeed a colour taken from the current sleeve. When present the accent roles are
 *   rebuilt around it; surfaces are always the app's own, so no album can make the app unreadable.
 */
@Composable
fun SlipmatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    artworkSeed: Color? = null,
    content: @Composable () -> Unit
) {
    val base = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val target = artworkSeed?.let { artworkColorScheme(base, it, darkTheme) } ?: base

    MaterialTheme(
        colorScheme = target.animatedAccents(),
        typography = Typography,
        content = content
    )
}

/**
 * Crossfades the accent roles when the album changes.
 *
 * Without this the whole interface snaps to a new colour the instant a track loads, which reads as
 * a rendering glitch rather than as the app responding to the record.
 */
@Composable
private fun ColorScheme.animatedAccents(): ColorScheme {
    val spec = tween<Color>(durationMillis = RETINT_MS)

    @Composable
    fun animate(color: Color, label: String) =
        animateColorAsState(targetValue = color, animationSpec = spec, label = label).value

    return copy(
        primary = animate(primary, "primary"),
        onPrimary = animate(onPrimary, "onPrimary"),
        primaryContainer = animate(primaryContainer, "primaryContainer"),
        onPrimaryContainer = animate(onPrimaryContainer, "onPrimaryContainer"),
        secondary = animate(secondary, "secondary"),
        onSecondary = animate(onSecondary, "onSecondary"),
        secondaryContainer = animate(secondaryContainer, "secondaryContainer"),
        onSecondaryContainer = animate(onSecondaryContainer, "onSecondaryContainer"),
        tertiary = animate(tertiary, "tertiary"),
        onTertiary = animate(onTertiary, "onTertiary"),
        tertiaryContainer = animate(tertiaryContainer, "tertiaryContainer"),
        onTertiaryContainer = animate(onTertiaryContainer, "onTertiaryContainer"),
    )
}