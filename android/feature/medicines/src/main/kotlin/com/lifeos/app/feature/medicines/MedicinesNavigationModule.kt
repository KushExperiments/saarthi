package com.lifeos.app.feature.medicines

import com.lifeos.app.core.navigation.FeatureNavigation
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object MedicinesNavigationModule {
    @Provides
    @IntoSet
    fun provideMedicinesNavigation(impl: MedicinesNavigation): FeatureNavigation = impl
}
