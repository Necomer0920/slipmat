package com.example.slipmat.core.media

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How far the slider can pull the tempo, as a percentage either side of normal.
 *
 * Wider ranges give coarser control over the same physical travel — ±8% is a turntable's fine
 * adjustment, ±50% is for pulling a track apart to learn it.
 */
enum class PitchRange(val percent: Float, val label: String) {
    Narrow(8f, "±8%"),
    Medium(16f, "±16%"),
    Wide(50f, "±50%"),
}

/**
 * What the player should be told to do, derived from the slider.
 *
 * [speed] and [pitch] are ExoPlayer multipliers where 1.0 is normal.
 */
data class SpeedPitch(val speed: Float, val pitch: Float) {
    companion object {
        val NORMAL = SpeedPitch(speed = 1f, pitch = 1f)
    }
}

/** Slider positions within this of centre snap back to exactly normal. */
const val DETENT_THRESHOLD = 0.02f

/**
 * Turns a slider position into player parameters.
 *
 * @param sliderValue -1f at the slowest end, 0f at centre, 1f at the fastest.
 * @param keyLock when true the key holds and only tempo moves; when false speed and pitch move
 *   together, which is what a turntable does — a slowed track drops in pitch.
 */
fun speedPitchFor(sliderValue: Float, range: PitchRange, keyLock: Boolean): SpeedPitch {
    val snapped = snapToDetent(sliderValue)
    val multiplier = 1f + (snapped * range.percent / 100f)
    return if (keyLock) {
        // Time-stretching: tempo shifts, key stays put.
        SpeedPitch(speed = multiplier, pitch = 1f)
    } else {
        // Resampling: the whole waveform is played faster or slower, so pitch follows tempo.
        SpeedPitch(speed = multiplier, pitch = multiplier)
    }
}

/**
 * Snaps near-centre positions to exactly 0.
 *
 * Without this, "normal speed" is almost impossible to hit by hand — the slider lands on 1.003x and
 * the track drifts against the original just slowly enough to be maddening.
 */
fun snapToDetent(sliderValue: Float): Float {
    val clamped = sliderValue.coerceIn(-1f, 1f)
    return if (abs(clamped) < DETENT_THRESHOLD) 0f else clamped
}

/** True when the slider is sitting in the detent, which is when the UI should fire a haptic. */
fun isAtDetent(sliderValue: Float): Boolean = snapToDetent(sliderValue) == 0f

/** The signed percentage change, for display. `+0.0%` at centre, never `-0.0%`. */
fun tempoPercent(sliderValue: Float, range: PitchRange): Float =
    snapToDetent(sliderValue) * range.percent

/**
 * The playing tempo of a track whose source tempo is known.
 *
 * Returns null when the source BPM is unknown, so the UI can hide the readout rather than invent a
 * number — nothing in this app detects BPM, and guessing would be worse than saying nothing.
 */
fun playingBpm(sourceBpm: Float?, sliderValue: Float, range: PitchRange): Float? {
    if (sourceBpm == null || sourceBpm <= 0f) return null
    val multiplier = 1f + (snapToDetent(sliderValue) * range.percent / 100f)
    return (sourceBpm * multiplier * 10).roundToInt() / 10f
}
