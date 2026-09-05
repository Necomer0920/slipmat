package com.example.slipmat.core.data.di

import android.content.Context
import androidx.room.Room
import com.example.slipmat.core.data.db.SlipmatDatabase
import com.example.slipmat.core.data.db.PlaybackPositionDao
import com.example.slipmat.core.data.db.TrackDao
import com.example.slipmat.core.data.db.EqPresetDao
import com.example.slipmat.core.data.db.WaveformDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SlipmatDatabase =
        Room.databaseBuilder(context, SlipmatDatabase::class.java, SlipmatDatabase.NAME)
            .build()

    @Provides
    fun provideTrackDao(database: SlipmatDatabase): TrackDao = database.trackDao()

    @Provides
    fun provideWaveformDao(database: SlipmatDatabase): WaveformDao = database.waveformDao()

    @Provides
    fun provideEqPresetDao(database: SlipmatDatabase): EqPresetDao = database.eqPresetDao()

    @Provides
    fun providePlaybackPositionDao(database: SlipmatDatabase): PlaybackPositionDao =
        database.playbackPositionDao()
}
