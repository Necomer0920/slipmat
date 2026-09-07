package com.example.slipmat.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG AA for normal text. Below this, a theme is not "a bit off" — it is unreadable. */
private const val MIN_CONTRAST = 4.5f

/**
 * WCAG AA for interface components, which is the check that actually bites.
 *
 * The foreground/background pairs below are readable more or less by construction, because
 * `onColorFor` picks whichever of black and white reads better and the worst case that leaves is
 * about 4.6:1. What is *not* automatic is the accent standing out from the surface behind it — a
 * dark blue sleeve yielding a dark blue button on a dark surface is a button nobody can see, and
 * every pair test in the world passes while it happens.
 */
private const val MIN_UI_CONTRAST = 3f

/** A spread of sleeve colours, including the awkward ones. */
private val SEEDS = listOf(
    "red" to Color(0xFFD32F2F),
    "orange" to Color(0xFFFF6D00),
    "yellow" to Color(0xFFFFEB3B),
    "green" to Color(0xFF2E7D32),
    "cyan" to Color(0xFF00BCD4),
    "blue" to Color(0xFF1A237E),
    "violet" to Color(0xFF6A1B9A),
    "magenta" to Color(0xFFC2185B),
    "near-black" to Color(0xFF101010),
    "near-white" to Color(0xFFF5F5F5),
    "pale pastel" to Color(0xFFFFCDD2),
    // §3.2's two worked examples, re-verified against the redesign's fixed surface ramp (R0.5).
    "red sleeve (§3.2 fixture)" to Color(0xFFFFB4A8),
    "purple sleeve (§3.2 fixture)" to Color(0xFFC9B8FF),
)

/**
 * A generated theme fails in a way that is invisible while you build it.
 *
 * It looks fine for the album you happened to test with, and puts unreadable text on a card for the
 * two records in someone's library whose sleeve is that particular colour. These check every
 * foreground/background pair the generator can produce, across every hue.
 */
class ArtworkColorsTest {

    @Test
    fun `every generated pair is readable, in both themes`() {
        for ((name, seed) in SEEDS) {
            for (dark in listOf(false, true)) {
                val base = if (dark) DarkColorScheme else LightColorScheme
                val scheme = artworkColorScheme(base, seed, dark)

                for ((role, pair) in scheme.accentPairs()) {
                    val ratio = contrastRatio(pair.first, pair.second)
                    assertTrue(
                        "$name ${if (dark) "dark" else "light"} $role: ${"%.2f".format(ratio)}:1",
                        ratio >= MIN_CONTRAST,
                    )
                }
            }
        }
    }

    @Test
    fun `accents stand out from the surface behind them`() {
        for ((name, seed) in SEEDS) {
            for (dark in listOf(false, true)) {
                val base = if (dark) DarkColorScheme else LightColorScheme
                val scheme = artworkColorScheme(base, seed, dark)

                val accents = listOf(
                    "primary" to scheme.primary,
                    "secondary" to scheme.secondary,
                    "tertiary" to scheme.tertiary,
                )
                for ((role, color) in accents) {
                    val ratio = contrastRatio(color, scheme.surface)
                    assertTrue(
                        "$name ${if (dark) "dark" else "light"} $role on surface: " +
                            "${"%.2f".format(ratio)}:1",
                        ratio >= MIN_UI_CONTRAST,
                    )
                }
            }
        }
    }

    @Test
    fun `the artwork changes the accents but never the surfaces`() {
        val base = LightColorScheme

        val scheme = artworkColorScheme(base, Color(0xFF2E7D32), dark = false)

        // Surfaces stay the app's own, so no sleeve can make the app unusable.
        assertEquals(base.surface, scheme.surface)
        assertEquals(base.background, scheme.background)
        assertEquals(base.onSurface, scheme.onSurface)
        assertNotEquals(base.primary, scheme.primary)
    }

    @Test
    fun `greyscale artwork is left alone rather than guessed at`() {
        val base = LightColorScheme

        // A monochrome sleeve carries no hue; deriving one produces mud from noise.
        val scheme = artworkColorScheme(base, Color(0xFF808080), dark = false)

        assertEquals(base.primary, scheme.primary)
    }

    @Test
    fun `setting lightness keeps the hue`() {
        val seed = Color(0xFF1A237E)
        val (hue, _, _) = seed.toHsl()

        val lightened = seed.withLightness(0.8f)

        assertEquals(hue, lightened.toHsl().first, 0.5f)
        assertTrue(lightened.toHsl().third > seed.toHsl().third)
    }

    @Test
    fun `hsl survives a round trip`() {
        for ((name, seed) in SEEDS) {
            val (hue, saturation, lightness) = seed.toHsl()
            val back = hslToColor(hue, saturation, lightness)

            assertEquals("$name red", seed.red, back.red, 0.01f)
            assertEquals("$name green", seed.green, back.green, 0.01f)
            assertEquals("$name blue", seed.blue, back.blue, 0.01f)
        }
    }

    @Test
    fun `the on-colour is whichever of black and white reads better`() {
        assertEquals(Color.Black, onColorFor(Color.White))
        assertEquals(Color.White, onColorFor(Color.Black))
        assertEquals(Color.White, onColorFor(Color(0xFF1A237E)))
        assertEquals(Color.Black, onColorFor(Color(0xFFFFEB3B)))
    }

    @Test
    fun `sleeves are downsampled, never upsampled`() {
        // Sample size is a divisor, so anything below 1 would be a no-op that reads as a bug later.
        assertEquals(1, sampleSizeFor(64))
        assertEquals(1, sampleSizeFor(128))
        assertEquals(1, sampleSizeFor(200))
        assertEquals(2, sampleSizeFor(256))
        assertEquals(4, sampleSizeFor(512))
        assertEquals(8, sampleSizeFor(1024))
        // A 3000px sleeve still lands near the target rather than being decoded whole.
        assertTrue(3000 / sampleSizeFor(3000) in 128..255)
    }

    @Test
    fun `contrast is symmetric and bounded`() {
        assertEquals(21f, contrastRatio(Color.Black, Color.White), 0.1f)
        assertEquals(21f, contrastRatio(Color.White, Color.Black), 0.1f)
        assertEquals(1f, contrastRatio(Color.Red, Color.Red), 0.001f)
    }
}

/** Every foreground/background pair this generator is responsible for. */
private fun ColorScheme.accentPairs(): List<Pair<String, Pair<Color, Color>>> = listOf(
    "primary" to (primary to onPrimary),
    "primaryContainer" to (primaryContainer to onPrimaryContainer),
    "secondary" to (secondary to onSecondary),
    "secondaryContainer" to (secondaryContainer to onSecondaryContainer),
    "tertiary" to (tertiary to onTertiary),
    "tertiaryContainer" to (tertiaryContainer to onTertiaryContainer),
)
