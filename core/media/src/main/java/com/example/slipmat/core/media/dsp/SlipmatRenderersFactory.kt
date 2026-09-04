package com.example.slipmat.core.media.dsp

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * Installs the effect chain into ExoPlayer's audio pipeline.
 *
 * The only reason this class exists is [buildAudioSink]: a `DefaultAudioSink` built with our
 * processors rather than the stock ones.
 *
 * **Float output is forced off, deliberately ignoring the flag ExoPlayer passes in.** With float
 * output enabled, PCM is routed through `ToFloatPcmAudioProcessor` and the user
 * `AudioProcessorChain` is skipped entirely — speed adjustment included. The failure mode is
 * silent: the filter simply does nothing, and the logs are clean.
 */
@UnstableApi
class SlipmatRenderersFactory(
    context: Context,
    private val effects: AudioEffects,
) : DefaultRenderersFactory(context) {

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink = DefaultAudioSink.Builder(context)
        .setEnableFloatOutput(false)
        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
        .setAudioProcessors(effects.processors())
        .build()
}
