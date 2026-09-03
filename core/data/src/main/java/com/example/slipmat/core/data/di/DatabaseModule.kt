package com.example.slipmat.core.data.di

import android.content.Context
import androidx.room.Room
import com.example.slipmat.core.data.db.SlipmatDatabase
import com.example.slipmat.core.data.db.TrackDao
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
}
