package com.lifeos.app.core.cognitive

import javax.inject.Qualifier

/**
 * [DecisionEngine] depends on [CandidateGenerator] (the interface), not the
 * concrete Rule/Llm classes directly — otherwise nothing could substitute a
 * fake generator in a test. Two bindings of the same interface type need a
 * qualifier to disambiguate for Hilt.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RuleGenerator

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LlmGenerator
