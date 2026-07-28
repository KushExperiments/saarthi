package com.saarthi.app.core.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "saarthi.db"

/**
 * Room generates [SaarthiDatabase]'s implementation itself — there is no
 * class to give an `@Inject constructor()`, so this is `@Provides`, not
 * `@Binds` (unlike every other Hilt module in this project so far).
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SaarthiDatabase =
        Room.databaseBuilder(context, SaarthiDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideMedicineDao(database: SaarthiDatabase): MedicineDao = database.medicineDao()

    @Provides
    fun provideContactDao(database: SaarthiDatabase): ContactDao = database.contactDao()
}
