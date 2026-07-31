package com.lifeos.app.core.interaction

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InteractionModule {
    @Binds
    @Singleton
    abstract fun bindClarificationEngine(impl: KnowledgeGraphClarificationEngine): ClarificationEngine

    @Binds
    @Singleton
    abstract fun bindEmotionDetector(impl: TextHeuristicEmotionDetector): EmotionDetector
}
