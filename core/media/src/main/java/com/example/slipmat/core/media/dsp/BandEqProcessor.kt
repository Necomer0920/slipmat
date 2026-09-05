package com.example.slipmat.core.media.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A multiband EQ: [EQ_BANDS] peaking filters in series.
 *
 * **One processor holding N bands, not N processors in the chain.** Independent biquads in series
 * commute, so the cascade is a detail of the arithmetic rather than of the pipeline; keeping it
 * inside one processor means one `isActive` contract, one `onFlush`, and one place where the
 * per-buffer enablement rule from [BiquadAudioProcessor] has to be got right.
 *
 * **Runs on the audio thread.** Nothing here allocates once configured. Band gains arrive from the
 * UI thread, which designs the coefficients and publishes them as a single immutable [Design]
 * through one `@Volatile` write — the audio thread reads that reference once per buffer, so it can
 * never see half of an update. Designing costs a handful of small objects, on the UI thread, where
 * that is unremarkable; the processing loop allocates nothing at all.
 *
 * Bands at zero gain are still run rather than skipped. Their coefficients are
 * [BiquadCoefficients.BYPASS], which passes the sample through untouched while keeping the band's
 * filter memory current — so pushing a band up mid-track picks up from the signal that was actually
 * there, instead of clicking from a history that stopped when the band was flattened.
 */
@UnstableApi
class BandEqProcessor : BaseAudioProcessor() {

    /** One published unit, so the audio thread cannot read a half-applied change. */
    private class Design(val coefficients: Array<BiquadCoefficients>)

    @Volatile
    private var enabled: Boolean = false

    @Volatile
    private var design: Design = Design(Array(EQ_BANDS.size) { BiquadCoefficients.BYPASS })

    /** Touched only from the UI thread; the audio thread reads [design], never this. */
    private val gainsDb = FloatArray(EQ_BANDS.size)

    private var sampleRate: Int = C.RATE_UNSET_INT
    private var channelCount: Int = 0

    // Filter memory, indexed band-major: [band * channelCount + channel].
    private var x1: FloatArray = FloatArray(0)
    private var x2: FloatArray = FloatArray(0)
    private var y1: FloatArray = FloatArray(0)
    private var y2: FloatArray = FloatArray(0)

    fun setEnabled(enabled: Boolean) {
        if (this.enabled == enabled) return
        this.enabled = enabled
        clearMemory()
    }

    /** Gains as the UI holds them, for mirroring into state and for drawing the curve. */
    fun gains(): FloatArray = gainsDb.copyOf()

    fun setGain(band: Int, gainDb: Float) {
        if (band !in gainsDb.indices) return
        gainsDb[band] = gainDb.coerceIn(-EQ_MAX_GAIN_DB, EQ_MAX_GAIN_DB)
        redesign()
    }

    fun setGains(gains: FloatArray) {
        for (band in gainsDb.indices) {
            gainsDb[band] = (gains.getOrNull(band) ?: 0f).coerceIn(-EQ_MAX_GAIN_DB, EQ_MAX_GAIN_DB)
        }
        redesign()
    }

    /** Designed off the audio thread; the loop only ever reads the finished coefficients. */
    private fun redesign() {
        if (sampleRate == C.RATE_UNSET_INT) return
        design = Design(
            Array(EQ_BANDS.size) { band ->
                peakingCoefficients(EQ_BANDS[band], gainsDb[band], sampleRate)
            },
        )
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount

        val size = EQ_BANDS.size * channelCount
        x1 = FloatArray(size)
        x2 = FloatArray(size)
        y1 = FloatArray(size)
        y2 = FloatArray(size)

        redesign()
        return inputAudioFormat
    }

    /** Active for any supported format; the switch is read per buffer. See [BiquadAudioProcessor]. */
    override fun isActive(): Boolean = super.isActive()

    override fun queueInput(inputBuffer: ByteBuffer) {
        val byteCount = inputBuffer.remaining()
        if (byteCount == 0) return

        val output = replaceOutputBuffer(byteCount).order(ByteOrder.nativeOrder())
        val input = inputBuffer.order(ByteOrder.nativeOrder())

        if (!enabled) {
            output.put(input)
            inputBuffer.position(inputBuffer.limit())
            output.flip()
            return
        }

        val coefficients = design.coefficients
        val bandCount = coefficients.size
        var channel = 0

        while (input.remaining() >= 2) {
            var sample = input.short.toFloat()

            var band = 0
            while (band < bandCount) {
                val i = band * channelCount + channel
                val c = coefficients[band]

                // Direct Form I, the output of each band feeding the next.
                val out = c.b0 * sample + c.b1 * x1[i] + c.b2 * x2[i] - c.a1 * y1[i] - c.a2 * y2[i]

                x2[i] = x1[i]
                x1[i] = sample
                y2[i] = y1[i]
                y1[i] = out

                sample = out
                band++
            }

            output.putShort(sample.coerceIn(EQ_SHORT_MIN, EQ_SHORT_MAX).toInt().toShort())

            channel++
            if (channel == channelCount) channel = 0
        }

        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        clearMemory()
    }

    private fun clearMemory() {
        x1.fill(0f)
        x2.fill(0f)
        y1.fill(0f)
        y2.fill(0f)
    }

    override fun onReset() {
        sampleRate = C.RATE_UNSET_INT
        channelCount = 0
        x1 = FloatArray(0)
        x2 = FloatArray(0)
        y1 = FloatArray(0)
        y2 = FloatArray(0)
        design = Design(Array(EQ_BANDS.size) { BiquadCoefficients.BYPASS })
    }
}

private const val EQ_SHORT_MIN = -32768f
private const val EQ_SHORT_MAX = 32767f

/**
 * The cascade's response at [frequencyHz], for drawing the curve.
 *
 * Filters in series **multiply**, so this multiplies. Adding the bands' magnitudes is the obvious
 * shortcut and it is wrong wherever two bands overlap — which, at 1.15 octaves apart, is everywhere
 * that matters. The drawn curve would then promise more boost than the ear gets.
 */
fun eqMagnitudeAt(gainsDb: FloatArray, frequencyHz: Float, sampleRate: Int): Float =
    eqMagnitude(designBands(gainsDb, sampleRate), frequencyHz, sampleRate)

/** Lowest frequency the curve is drawn from. Below this there is nothing to see or hear. */
const val CURVE_MIN_HZ = 20f

/** Highest. Above this the band centres have run out and so, mostly, has hearing. */
const val CURVE_MAX_HZ = 20_000f

/**
 * The cascade's response in decibels, sampled evenly on a **log** frequency axis.
 *
 * Log-spaced because that is how the curve is drawn and how frequency is heard: sampled linearly,
 * the bottom four bands would share the leftmost few pixels.
 *
 * Designs each band once and reuses it across every point. The obvious version calls
 * [eqMagnitudeAt] per point, which redesigns all eight bands per pixel column — hundreds of
 * designs per frame while a finger is moving.
 */
fun eqCurveDb(gainsDb: FloatArray, sampleRate: Int, points: Int): FloatArray {
    if (points <= 0) return FloatArray(0)
    val bands = designBands(gainsDb, sampleRate)
    val ratio = CURVE_MAX_HZ / CURVE_MIN_HZ

    return FloatArray(points) { point ->
        val fraction = if (points == 1) 0f else point.toFloat() / (points - 1)
        val hz = CURVE_MIN_HZ * Math.pow(ratio.toDouble(), fraction.toDouble()).toFloat()
        val magnitude = eqMagnitude(bands, hz, sampleRate)
        (20.0 * Math.log10(magnitude.toDouble().coerceAtLeast(1e-6))).toFloat()
    }
}

/** Where a frequency sits along the drawn axis, 0f at [CURVE_MIN_HZ] and 1f at [CURVE_MAX_HZ]. */
fun curveFractionFor(frequencyHz: Float): Float {
    val ratio = CURVE_MAX_HZ / CURVE_MIN_HZ
    val position = Math.log((frequencyHz / CURVE_MIN_HZ).toDouble()) / Math.log(ratio.toDouble())
    return position.toFloat().coerceIn(0f, 1f)
}

private fun designBands(gainsDb: FloatArray, sampleRate: Int): Array<BiquadCoefficients> =
    Array(EQ_BANDS.size) { band ->
        peakingCoefficients(EQ_BANDS[band], gainsDb.getOrNull(band) ?: 0f, sampleRate)
    }

private fun eqMagnitude(
    bands: Array<BiquadCoefficients>,
    frequencyHz: Float,
    sampleRate: Int,
): Float {
    var magnitude = 1f
    for (band in bands) magnitude *= band.magnitudeAt(frequencyHz, sampleRate)
    return magnitude
}
