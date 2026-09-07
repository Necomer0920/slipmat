package com.example.slipmat.nowplaying

import com.example.slipmat.core.data.eq.EqPreset

/**
 * The fixed curves the design names outright (§4.3: "Flat, Bass Boost, Vocal"). Hardcoded rather
 * than seeded into `EqPresetRepository` - nothing here is ever saved, renamed, or deletable the way
 * a row in that store is, so there is no state to persist. Order matches R0.8's own listing: Flat,
 * Bass Boost, Vocal, then Custom, then the user's saved presets (R3.13).
 *
 * Gains are for the eight [com.example.slipmat.core.media.dsp.EQ_BANDS], in the same order.
 */
val BUILT_IN_EQ_PRESETS: List<EqPreset> = listOf(
    EqPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    EqPreset("Bass Boost", listOf(6f, 5f, 3f, 1f, 0f, 0f, 0f, 0f)),
    EqPreset("Vocal", listOf(-2f, -1f, 0f, 3f, 4f, 3f, 0f, -1f)),
)
