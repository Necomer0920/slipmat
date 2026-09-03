package com.example.slipmat.core.media.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

/**
 * The player is service-scoped on purpose: exactly one instance, owned by the playback service and
 * torn down with it. Binding it any wider would let something outside the service hold a player.
 */
@Module
@InstallIn(ServiceComponent::class)
object PlaybackModule {

    @Provides
    @ServiceScoped
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer =
        ExoPlayer.Builder(context).build()
}
