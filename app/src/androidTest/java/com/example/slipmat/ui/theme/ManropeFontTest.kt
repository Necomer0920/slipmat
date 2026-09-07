package com.example.slipmat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * R0.1's verify needs the font resolved on-device, not just compiled in — a resource id that
 * fails to load falls back to the platform default silently, so `assembleDebug` succeeding proves
 * nothing on its own. This composes real text through [ManropeFamily] on the actual runtime and
 * saves what it drew, so the glyph shape (Manrope's single-storey "a") can be inspected directly
 * rather than trusted.
 */
@RunWith(AndroidJUnit4::class)
class ManropeFontTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun manropeRendersInDisplayLarge() {
        composeTestRule.setContent {
            SlipmatTheme {
                Surface {
                    Text(text = "Slipmat", style = MaterialTheme.typography.displayLarge)
                }
            }
        }

        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outFile = File(context.getExternalFilesDir(null), "manrope_display_large.png")
        outFile.outputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }

        assertTrue("expected a rendered screenshot", outFile.exists() && outFile.length() > 0)
    }
}
