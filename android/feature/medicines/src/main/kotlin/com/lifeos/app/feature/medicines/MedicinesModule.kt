package com.lifeos.app.feature.medicines

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MedicinesModule {
    @Binds
    @Singleton
    abstract fun bindMedicineRepository(impl: MedicineRepositoryImpl): MedicineRepository
}
