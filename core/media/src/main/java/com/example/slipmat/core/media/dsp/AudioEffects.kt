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

    val eq = BandEqProcessor()

    val delay = DelayAudioProcessor()

    val filter = BiquadAudioProcessor()

    /**
     * The signal path, left to right — and the order is a decision about how the app plays, not
     * an incidental array order.
     *
     * EQ first: it is tone shaping of the *track*, the thing you set once because a record is dull
     * or boomy, so everything downstream should hear the corrected version — including the echoes,
     * which are that track a moment later.
     *
     * Filter last, so it governs everything the listener hears including the tails: cut the track
     * out, leave the echo ringing, and sweep the whole wash away. The other order freezes each echo
     * with the tone it was captured at, and sweeping the filter then leaves the existing repeats
     * untouched, which reads as the filter being broken.
     */
    fun processors(): Array<AudioProcessor> = arrayOf(eq, delay, filter)
}

/**
 * The delay as the UI sees it.
 *
 * Mirrored rather than read back from the processor, for the same reason [FilterState] is: the
 * processor's fields are `@Volatile` singles on the audio thread, not something a recomposition
 * should poll.
 */
data class DelayState(
    val enabled: Boolean = false,
    val timeMs: Float = DEFAULT_DELAY_MS,
    val feedback: Float = DEFAULT_FEEDBACK,
    val mix: Float = DEFAULT_MIX,
) {
    /**
     * Time as 0f..1f, linear in milliseconds.
     *
     * Linear, unlike the filter's cutoff: a delay is set by note length, and the useful musical
     * values are spread evenly through the range rather than piled into the bottom octave.
     */
    val timeSlider: Float get() = (timeMs - MIN_DELAY_MS) / (MAX_DELAY_MS - MIN_DELAY_MS)

    /** Feedback as 0f..1f of the usable range, so the top of the slider is the ceiling. */
    val feedbackSlider: Float get() = feedback / MAX_FEEDBACK
}

/** Enough repeats to hear it as an effect rather than a doubling. */
const val DEFAULT_FEEDBACK = 0.35f

fun sliderToDelayMs(position: Float): Float =
    MIN_DELAY_MS + position.coerceIn(0f, 1f) * (MAX_DELAY_MS - MIN_DELAY_MS)

fun sliderToFeedback(position: Float): Float = position.coerceIn(0f, 1f) * MAX_FEEDBACK

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
