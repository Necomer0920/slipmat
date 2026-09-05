package com.example.slipmat.core.data.di

import com.example.slipmat.core.data.eq.EqPresetRepository
import com.example.slipmat.core.data.eq.EqPresetStore
import com.example.slipmat.core.data.scan.AudioSource
import com.example.slipmat.core.data.settings.DataStorePlaybackSettings
import com.example.slipmat.core.data.settings.PlaybackSettings
import com.example.slipmat.core.data.scan.MediaStoreScanner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ScanModule {

    @Binds
    abstract fun bindAudioSource(impl: MediaStoreScanner): AudioSource

    @Binds
    abstract fun bindPlaybackSettings(impl: DataStorePlaybackSettings): PlaybackSettings

    @Binds
    abstract fun bindEqPresetStore(impl: EqPresetRepository): EqPresetStore
}
