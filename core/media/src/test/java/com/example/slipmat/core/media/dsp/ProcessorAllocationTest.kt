package com.example.slipmat.core.media.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.lang.management.ManagementFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

private const val RATE = 44_100
private const val CHANNELS = 2
private const val FRAMES = 1_024

/** Enough to average out JIT noise; a per-buffer allocation shows up thousands of times over. */
private const val BUFFERS = 500

/**
 * How much the whole run may allocate, in bytes per buffer.
 *
 * Not zero: the measurement itself and any late JIT activity land on this thread too. But a single
 * `ByteArray(byteCount)` or `FloatArray(channelCount)` inside `queueInput` costs four kilobytes a
 * buffer, twenty times this, so the gap between "clean" and "allocating" is not a close call.
 */
private const val BUDGET_PER_BUFFER = 200L

/**
 * *(REQ-DSP-4)* Neither processor may allocate while audio is flowing.
 *
 * A garbage collection during playback is an audible dropout, not a profiler curiosity, and the
 * usual way one appears is a well-meaning edit — a temporary array, a lambda that captures, a boxed
 * float — inside a loop nobody thought of as hot. The allocation tracker in Android Studio catches
 * that once, by hand, on a good day. This catches it on every build.
 */
@UnstableApi
class ProcessorAllocationTest {

    @Test
    fun `the filter allocates nothing while processing`() {
        val filter = BiquadAudioProcessor().apply {
            configure(format())
            flush()
            setEnabled(true)
            setCutoff(800f)
        }

        assertNoAllocation("filter") { buffer -> filter.queueInput(buffer); filter.getOutput() }
    }

    @Test
    fun `the filter allocates nothing while switched off either`() {
        // The bypass path copies buffer to buffer, which is exactly where an easy `ByteArray` goes.
        val filter = BiquadAudioProcessor().apply {
            configure(format())
            flush()
            setEnabled(false)
        }

        assertNoAllocation("bypassed filter") { buffer -> filter.queueInput(buffer); filter.getOutput() }
    }

    @Test
    fun `the delay allocates nothing while processing`() {
        val delay = DelayAudioProcessor().apply {
            configure(format())
            flush()
            setEnabled(true)
            setFeedback(0.5f)
        }

        assertNoAllocation("delay") { buffer -> delay.queueInput(buffer); delay.getOutput() }
    }

    @Test
    fun `sweeping a parameter allocates nothing`() {
        // Sweeping is the case that matters: it is the only time these are touched from two
        // threads, and the tempting way to hand a new value across is to allocate a small object.
        val filter = BiquadAudioProcessor().apply {
            configure(format())
            flush()
            setEnabled(true)
        }
        val delay = DelayAudioProcessor().apply {
            configure(format())
            flush()
            setEnabled(true)
        }
        var step = 0

        assertNoAllocation("sweep") { buffer ->
            step++
            filter.setCutoff(200f + step)
            delay.setDelayMs(100f + step)
            filter.queueInput(buffer)
            filter.getOutput()
            delay.queueInput(buffer.rewind() as ByteBuffer)
            delay.getOutput()
        }
    }
}

private fun format() = AudioProcessor.AudioFormat(RATE, CHANNELS, C.ENCODING_PCM_16BIT)

/**
 * Runs [work] many times and asserts the thread barely allocated.
 *
 * Warms up first, so the buffer growth inside `replaceOutputBuffer` and the JIT's own allocations
 * are not counted against the steady state we actually care about.
 */
private fun assertNoAllocation(what: String, work: (ByteBuffer) -> Unit) {
    val bean = ManagementFactory.getThreadMXBean()
    assumeTrue(
        "this JVM does not report per-thread allocation",
        bean is com.sun.management.ThreadMXBean && bean.isThreadAllocatedMemoryEnabled,
    )
    val allocation = bean as com.sun.management.ThreadMXBean
    val threadId = Thread.currentThread().threadId()
    val buffer = pcm()

    repeat(BUFFERS) { work(buffer.rewind() as ByteBuffer) }

    val before = allocation.getThreadAllocatedBytes(threadId)
    repeat(BUFFERS) { work(buffer.rewind() as ByteBuffer) }
    val perBuffer = (allocation.getThreadAllocatedBytes(threadId) - before) / BUFFERS

    assertTrue("$what allocated $perBuffer bytes per buffer", perBuffer <= BUDGET_PER_BUFFER)
}

private fun pcm(): ByteBuffer {
    val buffer = ByteBuffer.allocate(FRAMES * CHANNELS * 2).order(ByteOrder.nativeOrder())
    for (frame in 0 until FRAMES) {
        val sample = ((frame % 200) * 100 - 10_000).toShort()
        repeat(CHANNELS) { buffer.putShort(sample) }
    }
    return buffer.flip() as ByteBuffer
}
