package com.saarthi.app.feature.contacts

import com.saarthi.app.core.navigation.FeatureNavigation
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object ContactsNavigationModule {
    @Provides
    @IntoSet
    fun provideContactsNavigation(impl: ContactsNavigation): FeatureNavigation = impl
}
