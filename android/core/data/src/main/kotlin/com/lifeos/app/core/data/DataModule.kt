package com.lifeos.app.core.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "lifeos.db"

/**
 * Room generates [LifeOSDatabase]'s implementation itself — there is no
 * class to give an `@Inject constructor()`, so this is `@Provides`, not
 * `@Binds` (unlike every other Hilt module in this project so far).
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LifeOSDatabase =
        Room.databaseBuilder(context, LifeOSDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideMedicineDao(database: LifeOSDatabase): MedicineDao = database.medicineDao()

    @Provides
    fun provideContactDao(database: LifeOSDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideMemoryNodeDao(database: LifeOSDatabase): MemoryNodeDao = database.memoryNodeDao()

    @Provides
    fun provideMemoryEdgeDao(database: LifeOSDatabase): MemoryEdgeDao = database.memoryEdgeDao()

    @Provides
    fun provideMemoryProvenanceDao(database: LifeOSDatabase): MemoryProvenanceDao = database.memoryProvenanceDao()

    @Provides
    fun provideMemoryAuditLogDao(database: LifeOSDatabase): MemoryAuditLogDao = database.memoryAuditLogDao()

    @Provides
    fun provideHabitCandidateDao(database: LifeOSDatabase): HabitCandidateDao = database.habitCandidateDao()

    @Provides
    fun provideConversationSummaryDao(database: LifeOSDatabase): ConversationSummaryDao =
        database.conversationSummaryDao()

    @Provides
    fun provideLifeTimelineDao(database: LifeOSDatabase): LifeTimelineDao = database.lifeTimelineDao()

    @Provides
    fun provideDecisionTraceDao(database: LifeOSDatabase): DecisionTraceDao = database.decisionTraceDao()
}
