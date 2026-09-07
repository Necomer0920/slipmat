package com.example.slipmat.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The two fixed surface ramps, §3.1. Named by M3 role rather than by hue, because — unlike accent
 * colour (§3.2, [artworkColorScheme]) — nothing here is ever derived from an album. Any track's
 * artwork retints [ArtworkColors]'s primary/secondary/tertiary roles; it never touches these.
 */

// Dark ramp.
val SurfaceDark = Color(0xFF141316)
val SurfaceContainerLowestDark = Color(0xFF161518)
val SurfaceContainerLowDark = Color(0xFF1B1A1D)
val SurfaceContainerDark = Color(0xFF201F22)
val SurfaceContainerHighDark = Color(0xFF2B2A2D)
val SurfaceContainerHighestDark = Color(0xFF363539)

/** Pinned to [SurfaceContainerHighestDark] — the value `WaveformSeekBar`'s unplayed bars use. */
val SurfaceVariantDark = SurfaceContainerHighestDark

val OnSurfaceDark = Color(0xFFECE7EA)
val OnSurfaceVariantDark = Color(0xFFB0ABAE)
val OutlineDark = Color(0xFF4A484B)
val OutlineVariantDark = Color(0xFF2B2A2D)

val BackgroundDark = SurfaceDark
val OnBackgroundDark = OnSurfaceDark
val SurfaceDimDark = SurfaceDark
val SurfaceBrightDark = Color(0xFF3A383C)

// Light ramp.
val SurfaceLight = Color(0xFFFBF9FA)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF4F2F3)
val SurfaceContainerLight = Color(0xFFEEECED)
val SurfaceContainerHighLight = Color(0xFFE8E6E7)
val SurfaceContainerHighestLight = Color(0xFFE2E0E1)

/** Pinned to [SurfaceContainerHighestLight], mirroring [SurfaceVariantDark]. */
val SurfaceVariantLight = SurfaceContainerHighestLight

val OnSurfaceLight = Color(0xFF1B1A1D)
val OnSurfaceVariantLight = Color(0xFF48464A)
val OutlineLight = Color(0xFF78767A)
val OutlineVariantLight = Color(0xFFDAD7D9)

val BackgroundLight = SurfaceLight
val OnBackgroundLight = OnSurfaceLight
val SurfaceDimLight = Color(0xFFDBD9DA)
val SurfaceBrightLight = SurfaceLight

// Inverse roles — each ramp's surface/onSurface, swapped in from the other.
val InverseSurfaceDark = SurfaceLight
val InverseOnSurfaceDark = OnSurfaceLight
val InverseSurfaceLight = SurfaceDark
val InverseOnSurfaceLight = OnSurfaceDark

// `scrim` (#000000 @ 60% dark / @ 40% light) is applied at the use site, per §3.1 — not a token.
