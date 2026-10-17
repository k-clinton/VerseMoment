package com.versemoment.app.di

import android.content.Context
import com.versemoment.app.data.PreferencesRepository
import com.versemoment.app.data.VerseMomentDatabase
import com.versemoment.app.data.VerseRepository
import com.versemoment.app.scheduling.AutomationScheduler
import com.versemoment.app.wallpaper.WallpaperRenderer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VerseMomentDatabase =
        VerseMomentDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideRepository(database: VerseMomentDatabase): VerseRepository =
        VerseRepository(database.verseDao())

    @Provides
    @Singleton
    fun providePreferencesRepository(@ApplicationContext context: Context): PreferencesRepository =
        PreferencesRepository(context)

    @Provides
    @Singleton
    fun provideWallpaperRenderer(@ApplicationContext context: Context): WallpaperRenderer =
        WallpaperRenderer(context)

    @Provides
    @Singleton
    fun provideAutomationScheduler(@ApplicationContext context: Context): AutomationScheduler =
        AutomationScheduler(context)
}
