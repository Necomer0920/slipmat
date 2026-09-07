package com.example.slipmat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.slipmat.R

/**
 * A single variable font file standing in for five static weights (400/500/600/700/800) — each
 * [Font] entry names the weight Compose should select it for, and pins the same axis value into
 * the file itself so the glyphs actually render at that weight rather than being faked with a
 * synthetic bold.
 */
@OptIn(ExperimentalTextApi::class)
val ManropeFamily = FontFamily(
    Font(
        R.font.manrope,
        FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.manrope,
        FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.manrope,
        FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.manrope,
        FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        R.font.manrope,
        FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800)),
    ),
)

/**
 * Tabular figures — every digit the same width, so a readout doesn't visibly jitter as it counts.
 * Reserved for the three slots that display a number changing while it's being watched: the filter
 * cutoff, the tempo/speed/knob value, and the elapsed-time clock (§3.3).
 */
private const val TNUM = "tnum"

/**
 * §3.3's scale names nine sizes, not the fifteen roles Material 3 exposes. Where a role has no
 * named counterpart, its size is interpolated from its neighbours rather than left at the M3
 * default — a role either carries the design's own number or one read off the scale around it,
 * never the stock Roboto value. Where §3.3 gives a range (e.g. "12–13/600–700"), this takes the
 * upper bound of both, consistently, rather than picking a different point in the range per role.
 */
val Typography = Typography(
    // Library title — §4.1.
    displayLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
    ),
    // Filter cutoff readout — §4.3.
    displayMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        fontFeatureSettings = TNUM,
    ),
    // Interpolated between the cutoff readout and the headline tier.
    displaySmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 23.sp,
    ),
    // Interpolated between displaySmall and the Now Playing title.
    headlineLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 21.sp,
    ),
    // Now Playing title — §4.2, the one role §3.3 gives an explicit line-height for.
    headlineSmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
    ),
    // Interpolated between the Now Playing title and the tempo/knob value.
    titleLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
    ),
    // Section value: tempo percentage, speed multiplier, knob readouts — §3.3/§4.3.
    titleMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        fontFeatureSettings = TNUM,
    ),
    // List headline — §4.1 track/artist/folder row titles.
    titleSmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
    ),
    // Interpolated between the list headline and list support text.
    bodyLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    // List support — "artist · duration" under a row title.
    bodyMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
    ),
    // Elapsed/total time under the waveform — §4.2, the clock that ticks while watched.
    bodySmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        fontFeatureSettings = TNUM,
    ),
    // Tab / chip label — pill tabs, chips, the filter LP/HP toggle.
    labelLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
    ),
    // Eyebrow — "TEMPO", "CUTOFF", "PERFORMANCE", the now-playing header label.
    labelMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp,
    ),
    // EQ frequency label — the smallest text in the design.
    labelSmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 9.5.sp,
    ),
)