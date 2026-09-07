package com.example.slipmat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Every M3 role, paired with its default (stock Roboto) style for comparison. */
private fun roles(typography: Typography): List<Pair<String, TextStyle>> = listOf(
    "displayLarge" to typography.displayLarge,
    "displayMedium" to typography.displayMedium,
    "displaySmall" to typography.displaySmall,
    "headlineLarge" to typography.headlineLarge,
    "headlineMedium" to typography.headlineMedium,
    "headlineSmall" to typography.headlineSmall,
    "titleLarge" to typography.titleLarge,
    "titleMedium" to typography.titleMedium,
    "titleSmall" to typography.titleSmall,
    "bodyLarge" to typography.bodyLarge,
    "bodyMedium" to typography.bodyMedium,
    "bodySmall" to typography.bodySmall,
    "labelLarge" to typography.labelLarge,
    "labelMedium" to typography.labelMedium,
    "labelSmall" to typography.labelSmall,
)

/** The three readouts that change while being watched — §3.3's tabular-figure requirement. */
private val TNUM_ROLES = setOf("displayMedium", "titleMedium", "bodySmall")

class TypeTest {

    @Test
    fun `no role is left at the platform default`() {
        val defaults = roles(Typography())
        val ours = roles(Typography)

        for ((name, default) in defaults) {
            val actual = ours.first { it.first == name }.second
            assertNotEquals("$name still matches the M3 default", default.fontFamily, actual.fontFamily)
            assertEquals("$name should render through Manrope", ManropeFamily, actual.fontFamily)
        }
    }

    @Test
    fun `exactly the three watched readouts carry tabular figures`() {
        for ((name, style) in roles(Typography)) {
            val hasTnum = style.fontFeatureSettings?.contains("tnum") == true
            if (name in TNUM_ROLES) {
                assertEquals("$name should carry tnum", true, hasTnum)
            } else {
                assertEquals("$name should not carry tnum", false, hasTnum)
            }
        }
    }
}
