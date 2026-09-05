package com.example.slipmat.core.media.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Which way the filter cuts. */
enum class FilterMode {
    LowPass,
    HighPass,
}

/**
 * Butterworth Q. Gives the flattest passband and exactly −3 dB at the cutoff, which is what makes a
 * sweep feel even rather than resonant.
 */
const val BUTTERWORTH_Q = 0.70710678f

/**
 * Normalised biquad coefficients, already divided through by `a0`.
 *
 * Storing them normalised means the processing loop is five multiplies and four adds per sample
 * with no division — division in a per-sample loop is the kind of thing that shows up as audio
 * dropouts rather than as a slow benchmark.
 */
data class BiquadCoefficients(
    val b0: Float,
    val b1: Float,
    val b2: Float,
    val a1: Float,
    val a2: Float,
) {
    companion object {
        /** Passes everything through untouched. */
        val BYPASS = BiquadCoefficients(b0 = 1f, b1 = 0f, b2 = 0f, a1 = 0f, a2 = 0f)
    }
}

/**
 * Designs a filter using the standard bilinear-transform formulas (the "RBJ cookbook").
 *
 * [cutoffHz] is clamped below Nyquist: at or above it the design degenerates and the filter can go
 * unstable, which sounds like a loud burst of noise rather than a wrong frequency response.
 */
fun biquadCoefficients(
    mode: FilterMode,
    cutoffHz: Float,
    sampleRate: Int,
    q: Float = BUTTERWORTH_Q,
): BiquadCoefficients {
    if (sampleRate <= 0) return BiquadCoefficients.BYPASS

    val nyquist = sampleRate / 2f
    val cutoff = cutoffHz.coerceIn(MIN_CUTOFF_HZ, nyquist * MAX_CUTOFF_FRACTION)
    val safeQ = if (q <= 0f) BUTTERWORTH_Q else q

    val w0 = 2.0 * PI * cutoff / sampleRate
    val cosW0 = cos(w0)
    val alpha = sin(w0) / (2.0 * safeQ)

    val a0 = 1.0 + alpha
    val a1 = -2.0 * cosW0
    val a2 = 1.0 - alpha

    val (b0, b1, b2) = when (mode) {
        FilterMode.LowPass -> Triple((1.0 - cosW0) / 2.0, 1.0 - cosW0, (1.0 - cosW0) / 2.0)
        FilterMode.HighPass -> Triple((1.0 + cosW0) / 2.0, -(1.0 + cosW0), (1.0 + cosW0) / 2.0)
    }

    return BiquadCoefficients(
        b0 = (b0 / a0).toFloat(),
        b1 = (b1 / a0).toFloat(),
        b2 = (b2 / a0).toFloat(),
        a1 = (a1 / a0).toFloat(),
        a2 = (a2 / a0).toFloat(),
    )
}

/**
 * Centre frequencies for the EQ, log-spaced so each band covers the same musical interval.
 *
 * Even spacing in hertz would put half the bands above 8 kHz, where there is little to adjust, and
 * leave the bass — which is what anyone actually reaches for — covered by one.
 */
val EQ_BANDS: List<Float> = listOf(60f, 130f, 300f, 650f, 1500f, 3200f, 7000f, 16_000f)

/** Ratio between neighbouring band centres: roughly 2.2x, or about 1.15 octaves. */
const val EQ_BAND_SPACING = 2.2f

/**
 * Q matched to [EQ_BAND_SPACING], not chosen by ear.
 *
 * For a bandwidth of N octaves, Q = sqrt(2^N) / (2^N - 1). At 1.15 octaves that is about 1.22.
 * Narrower and the bands leave untouched gaps between them, so the curve on screen stops matching
 * what is heard; wider and every band drags its neighbours with it.
 */
const val EQ_BAND_Q = 1.22f

/** How far a band can be pushed either way. */
const val EQ_MAX_GAIN_DB = 12f

/**
 * Designs one peaking band: [gainDb] at [centerHz], tapering back to flat either side.
 *
 * `A` is `10^(gainDb/40)` — **forty**. The peak gain of this filter is `A²`, so the exponent is
 * halved here to make that come out at the requested dB. Writing 20, which is the usual reflex for
 * a dB-to-linear conversion, gives a filter that boosts by exactly twice what was asked for and
 * looks entirely reasonable doing it.
 */
fun peakingCoefficients(
    centerHz: Float,
    gainDb: Float,
    sampleRate: Int,
    q: Float = EQ_BAND_Q,
): BiquadCoefficients {
    if (sampleRate <= 0) return BiquadCoefficients.BYPASS
    if (gainDb == 0f) return BiquadCoefficients.BYPASS

    val nyquist = sampleRate / 2f
    val center = centerHz.coerceIn(MIN_CUTOFF_HZ, nyquist * MAX_CUTOFF_FRACTION)
    val safeQ = if (q <= 0f) EQ_BAND_Q else q

    val a = Math.pow(10.0, gainDb.toDouble() / 40.0)
    val w0 = 2.0 * PI * center / sampleRate
    val cosW0 = cos(w0)
    val alpha = sin(w0) / (2.0 * safeQ)

    val a0 = 1.0 + alpha / a
    return BiquadCoefficients(
        b0 = ((1.0 + alpha * a) / a0).toFloat(),
        b1 = ((-2.0 * cosW0) / a0).toFloat(),
        b2 = ((1.0 - alpha * a) / a0).toFloat(),
        a1 = ((-2.0 * cosW0) / a0).toFloat(),
        a2 = ((1.0 - alpha / a) / a0).toFloat(),
    )
}

/**
 * Magnitude of the filter's response at [frequencyHz].
 *
 * Exists so the tests can assert what the filter *does* — passes lows, rejects highs, −3 dB at
 * cutoff — rather than that five numbers match five other numbers. A coefficient typo passes an
 * arithmetic test and fails this one.
 */
fun BiquadCoefficients.magnitudeAt(frequencyHz: Float, sampleRate: Int): Float {
    val w = 2.0 * PI * frequencyHz / sampleRate
    // Evaluate H(z) on the unit circle, z = e^(jw).
    val cos1 = cos(w)
    val sin1 = sin(w)
    val cos2 = cos(2 * w)
    val sin2 = sin(2 * w)

    val numReal = b0 + b1 * cos1 + b2 * cos2
    val numImag = -(b1 * sin1 + b2 * sin2)
    val denReal = 1.0 + a1 * cos1 + a2 * cos2
    val denImag = -(a1 * sin1 + a2 * sin2)

    val numMag = sqrt(numReal * numReal + numImag * numImag)
    val denMag = sqrt(denReal * denReal + denImag * denImag)
    return if (denMag == 0.0) 0f else (numMag / denMag).toFloat()
}

/** Below this the filter is inaudible anyway, and the design loses precision. */
const val MIN_CUTOFF_HZ = 20f

/** Kept clear of Nyquist, where the bilinear transform degenerates. */
private const val MAX_CUTOFF_FRACTION = 0.99f
