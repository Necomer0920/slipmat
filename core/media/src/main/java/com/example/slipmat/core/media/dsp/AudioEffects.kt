package com.example.slipmat.core.media.dsp

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The effect chain, owned in one place.
 *
 * A singleton because two very different things need the same instances: the renderers factory,
 * which installs them into the audio pipeline, and the UI, which turns their knobs. Constructing
 * them twice would give the UI a filter that is not in the signal path — and nothing would report
 * an error, the sliders would simply do nothing.
 */
@UnstableApi
@Singleton
class AudioEffects @Inject constructor() {

    val filter = BiquadAudioProcessor()

    /** Order matters: this is the signal path, left to right. */
    fun processors(): Array<AudioProcessor> = arrayOf(filter)
}

/**
 * The filter as the UI sees it.
 *
 * Mirrored rather than read back from the processor: the processor lives on the audio thread and
 * its fields are `@Volatile` singles, not something a Compose recomposition should be polling.
 */
data class FilterState(
    val enabled: Boolean = false,
    val cutoffHz: Float = FILTER_MAX_HZ,
    val mode: FilterMode = FilterMode.LowPass,
) {
    /**
     * Cutoff as 0f..1f on a logarithmic scale.
     *
     * Frequency is perceived logarithmically: a linear slider spends most of its travel in the top
     * octave, where nothing audible happens, and crosses the entire bass region in a few pixels.
     */
    val sliderPosition: Float
        get() = frequencyToSlider(cutoffHz)
}

/** Maps a slider position to a frequency, spacing octaves evenly across the travel. */
fun sliderToFrequency(position: Float): Float {
    val clamped = position.coerceIn(0f, 1f)
    val logMin = kotlin.math.ln(FILTER_MIN_HZ.toDouble())
    val logMax = kotlin.math.ln(FILTER_MAX_HZ.toDouble())
    return kotlin.math.exp(logMin + clamped * (logMax - logMin)).toFloat()
}

/** The inverse of [sliderToFrequency]. */
fun frequencyToSlider(hz: Float): Float {
    val clamped = hz.coerceIn(FILTER_MIN_HZ, FILTER_MAX_HZ)
    val logMin = kotlin.math.ln(FILTER_MIN_HZ.toDouble())
    val logMax = kotlin.math.ln(FILTER_MAX_HZ.toDouble())
    return ((kotlin.math.ln(clamped.toDouble()) - logMin) / (logMax - logMin)).toFloat()
}
