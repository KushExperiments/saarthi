package com.lifeos.app.core.cognitive

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CognitiveModule {
    @Binds
    @Singleton
    abstract fun bindDecisionTraceLogger(impl: RoomDecisionTraceLogger): DecisionTraceLogger

    @Binds
    @Singleton
    abstract fun bindDecisionTraceReader(impl: RoomDecisionTraceReader): DecisionTraceReader

    @Binds
    @Singleton
    abstract fun bindAdaptiveParamsStore(impl: SharedPrefsAdaptiveParamsStore): AdaptiveParamsStore

    @Binds
    @RuleGenerator
    abstract fun bindRuleGenerator(impl: RuleBasedCandidateGenerator): CandidateGenerator

    @Binds
    @LlmGenerator
    abstract fun bindLlmGenerator(impl: LlmCandidateGenerator): CandidateGenerator
}
