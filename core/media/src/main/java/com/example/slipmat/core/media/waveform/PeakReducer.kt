package com.example.slipmat.core.media.waveform

import kotlin.math.abs

/** How many peaks a stored waveform holds. Enough detail for a phone-width canvas, small to store. */
const val WAVEFORM_BUCKETS = 400

/**
 * Reduces a PCM stream to a fixed number of peak values.
 *
 * Streaming rather than array-at-once: a five-minute stereo track is about 50 MB of 16-bit PCM, and
 * holding that to compute 400 numbers would be absurd. Samples are folded in as they decode and
 * discarded immediately.
 *
 * Peak rather than RMS because the result is drawn, not measured — peaks preserve the transients
 * that make a waveform recognisable as *that* track.
 *
 * @param totalSampleCount expected total samples across all channels; used to map samples onto
 *   buckets. An estimate is fine, and [peaks] copes with the stream running short or long.
 */
class PeakReducer(
    private val bucketCount: Int = WAVEFORM_BUCKETS,
    private val totalSampleCount: Long,
) {
    private val peaks = FloatArray(bucketCount)
    private var samplesSeen = 0L

    /** Folds [length] samples from [buffer] into the running peaks. */
    fun add(buffer: ShortArray, length: Int) {
        for (i in 0 until length) {
            val bucket = bucketFor(samplesSeen)
            val magnitude = abs(buffer[i].toInt()) / Short.MAX_VALUE.toFloat()
            if (magnitude > peaks[bucket]) peaks[bucket] = magnitude
            samplesSeen++
        }
    }

    /**
     * The finished waveform, normalised so the loudest point is 1.0.
     *
     * Without normalising, a quiet recording draws as a flat line — technically accurate and
     * useless for scrubbing against.
     */
    fun peaks(): FloatArray {
        val loudest = peaks.max()
        if (loudest <= 0f) return peaks.copyOf()
        return FloatArray(bucketCount) { (peaks[it] / loudest).coerceIn(0f, 1f) }
    }

    private fun bucketFor(sampleIndex: Long): Int {
        if (totalSampleCount <= 0L) return 0
        // A stream that runs longer than predicted piles into the last bucket rather than crashing.
        val bucket = (sampleIndex * bucketCount / totalSampleCount).toInt()
        return bucket.coerceIn(0, bucketCount - 1)
    }
}
