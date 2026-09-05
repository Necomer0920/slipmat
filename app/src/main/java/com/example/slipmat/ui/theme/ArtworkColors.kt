package com.example.slipmat.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs

/**
 * Turning one colour pulled off a record sleeve into a usable theme.
 *
 * The whole risk here is legibility. A generated palette that happens to put grey text on a grey
 * card is not a slightly worse theme, it is an unreadable screen, and it only happens for the two
 * albums out of a library whose art is that colour — which is exactly the bug that never shows up
 * while you are building it. So tones are set explicitly rather than taken from the artwork, the
 * artwork only supplies the *hue*, and every foreground/background pair this produces is asserted
 * against WCAG contrast in `ArtworkColorsTest`.
 */

/** Contrast ratio between two colours, 1:1 (identical) to 21:1 (black on white). */
fun contrastRatio(a: Color, b: Color): Float {
    val lighter = maxOf(a.luminance(), b.luminance())
    val darker = minOf(a.luminance(), b.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}

/** Black or white on [background], whichever is easier to read. */
fun onColorFor(background: Color): Color =
    if (contrastRatio(background, Color.White) >= contrastRatio(background, Color.Black)) {
        Color.White
    } else {
        Color.Black
    }

/**
 * The same hue and saturation at a different lightness.
 *
 * Lightness is what contrast is made of, so setting it directly is what makes the result
 * predictable no matter what colour the sleeve happened to be.
 */
fun Color.withLightness(lightness: Float): Color {
    val (hue, saturation, _) = toHsl()
    return hslToColor(hue, saturation, lightness.coerceIn(0f, 1f))
}

/** The same colour with its saturation scaled, for pulling a secondary role back from the primary. */
fun Color.scaleSaturation(factor: Float): Color {
    val (hue, saturation, lightness) = toHsl()
    return hslToColor(hue, (saturation * factor).coerceIn(0f, 1f), lightness)
}

/** The same lightness and saturation, rotated around the wheel. */
fun Color.rotateHue(degrees: Float): Color {
    val (hue, saturation, lightness) = toHsl()
    return hslToColor((hue + degrees).mod(360f), saturation, lightness)
}

/**
 * Recolours the accent roles of [base] from [seed], leaving surfaces and backgrounds alone.
 *
 * Deliberately partial. Retinting the surfaces too is the version that looks spectacular on three
 * albums and unreadable on the rest; keeping the app's own surfaces means the artwork changes the
 * app's character without ever being able to make it unusable.
 */
fun artworkColorScheme(base: ColorScheme, seed: Color, dark: Boolean): ColorScheme {
    val accent = if (seed.saturationOrZero() < MIN_SATURATION) {
        // Nearly grey artwork carries no usable hue, so the base theme is left in place rather
        // than generating a mud-coloured scheme from noise.
        return base
    } else {
        seed
    }

    val surface = base.surface
    val primary = accent.readableOn(surface, dark)
    val container = accent.withLightness(if (dark) DARK_CONTAINER_L else LIGHT_CONTAINER_L)
    val secondary = accent.scaleSaturation(SECONDARY_SATURATION).readableOn(surface, dark)
    val secondaryContainer = accent.scaleSaturation(SECONDARY_SATURATION)
        .withLightness(if (dark) DARK_CONTAINER_L else LIGHT_CONTAINER_L)
    val tertiary = accent.rotateHue(TERTIARY_HUE_SHIFT).readableOn(surface, dark)
    val tertiaryContainer = accent.rotateHue(TERTIARY_HUE_SHIFT)
        .withLightness(if (dark) DARK_CONTAINER_L else LIGHT_CONTAINER_L)

    return base.copy(
        primary = primary,
        onPrimary = onColorFor(primary),
        primaryContainer = container,
        onPrimaryContainer = onColorFor(container),
        secondary = secondary,
        onSecondary = onColorFor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onColorFor(secondaryContainer),
        tertiary = tertiary,
        onTertiary = onColorFor(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onColorFor(tertiaryContainer),
    )
}

/**
 * The accent, moved away from [surface] until it is clearly visible against it.
 *
 * A fixed lightness cannot do this job. HSL lightness is not perceptually uniform: at L=0.36 a
 * yellow carries far more luminance than a blue, because luminance is weighted heavily towards
 * green. Setting every hue to the same nominal tone produced a red album's tertiary — which is a
 * hue rotation into yellow — at **2.92:1** against a light surface, an accent you can barely find.
 *
 * Stepping until the target is met makes the *property* the input rather than a number that happens
 * to satisfy it for the hues that were tried.
 */
private fun Color.readableOn(surface: Color, dark: Boolean): Color {
    val step = if (dark) TONE_STEP else -TONE_STEP
    var lightness = if (dark) DARK_ACCENT_L else LIGHT_ACCENT_L
    var candidate = withLightness(lightness)

    while (contrastRatio(candidate, surface) < ACCENT_CONTRAST_TARGET && lightness in 0f..1f) {
        lightness += step
        if (lightness !in 0f..1f) break
        candidate = withLightness(lightness)
    }
    return candidate
}

private fun Color.saturationOrZero(): Float = toHsl().second

/** Below this the artwork is effectively greyscale and its hue is noise. */
const val MIN_SATURATION = 0.12f

private const val LIGHT_ACCENT_L = 0.36f
private const val LIGHT_CONTAINER_L = 0.88f
private const val DARK_ACCENT_L = 0.74f
private const val DARK_CONTAINER_L = 0.26f
private const val SECONDARY_SATURATION = 0.5f
private const val TERTIARY_HUE_SHIFT = 60f

/** WCAG AA for interface components is 3:1; the margin absorbs rounding across hues. */
private const val ACCENT_CONTRAST_TARGET = 3.4f

private const val TONE_STEP = 0.02f

/** Hue in degrees, saturation and lightness in 0f..1f. */
internal fun Color.toHsl(): Triple<Float, Float, Float> {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val lightness = (max + min) / 2f

    if (delta < 1e-6f) return Triple(0f, 0f, lightness)

    val saturation = delta / (1f - abs(2f * lightness - 1f)).coerceAtLeast(1e-6f)
    val hue = when (max) {
        r -> 60f * (((g - b) / delta).mod(6f))
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    return Triple(hue.mod(360f), saturation.coerceIn(0f, 1f), lightness)
}

internal fun hslToColor(hue: Float, saturation: Float, lightness: Float): Color {
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val hPrime = hue.mod(360f) / 60f
    val x = c * (1f - abs(hPrime.mod(2f) - 1f))
    val m = lightness - c / 2f

    val (r, g, b) = when (hPrime.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color((r + m).coerceIn(0f, 1f), (g + m).coerceIn(0f, 1f), (b + m).coerceIn(0f, 1f))
}
