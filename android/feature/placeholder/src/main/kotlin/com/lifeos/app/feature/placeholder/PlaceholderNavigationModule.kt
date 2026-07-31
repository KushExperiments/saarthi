package com.lifeos.app.feature.placeholder

import com.lifeos.app.core.navigation.FeatureNavigation
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Binds this feature's navigation into the app-wide `Set<FeatureNavigation>`
 * (Hilt multibinding) — the app module collects the whole set without ever
 * importing this feature module by name. Every future feature module
 * (Reminders, Memory, Emergency, ...) repeats this exact pattern.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlaceholderNavigationModule {
    @Provides
    @IntoSet
    fun providePlaceholderNavigation(impl: PlaceholderNavigation): FeatureNavigation = impl
}
