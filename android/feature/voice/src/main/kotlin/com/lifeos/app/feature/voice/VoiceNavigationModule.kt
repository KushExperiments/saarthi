package com.lifeos.app.feature.voice

import com.lifeos.app.core.navigation.FeatureNavigation
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object VoiceNavigationModule {
    @Provides
    @IntoSet
    fun provideVoiceNavigation(impl: VoiceNavigation): FeatureNavigation = impl
}
