package com.example.slipmat.core.media.di

import com.example.slipmat.core.media.MediaControllerHolder
import com.example.slipmat.core.media.PlaybackController
import com.example.slipmat.core.media.waveform.WaveformRepository
import com.example.slipmat.core.media.waveform.WaveformSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the only implementation behind the Media3-free interface, so `:app` injects
 * [PlaybackController] and never sees [MediaControllerHolder]'s Media3 internals.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ControllerModule {

    @Binds
    @Singleton
    abstract fun bindPlaybackController(impl: MediaControllerHolder): PlaybackController

    @Binds
    @Singleton
    abstract fun bindWaveformSource(impl: WaveformRepository): WaveformSource
}
