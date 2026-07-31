package com.lifeos.app.core.cognitive

import com.lifeos.app.core.ai.AiProvider
import com.lifeos.app.core.ai.UnderstandRequest
import com.lifeos.app.core.common.Outcome
import javax.inject.Inject

/**
 * Wraps core:ai's [AiProvider] as a second candidate source. This is a
 * generator only — [DecisionEngine] never treats an LLM candidate as
 * automatically approved, [Scorer]/[SafetyValidator] apply the exact same
 * scrutiny to it as to a rule-based candidate. A failed/unavailable AI call
 * degrades to zero candidates, never an exception the caller must catch.
 */
class LlmCandidateGenerator @Inject constructor(
    private val aiProvider: AiProvider,
) : CandidateGenerator {

    override suspend fun generate(input: DecisionInput): List<Candidate> {
        val result = aiProvider.understand(
            UnderstandRequest(
                transcript = input.transcript,
                knownPeople = input.knownPeople,
                languageHint = input.languageHint,
            ),
        )
        return when (result) {
            is Outcome.Success -> {
                val intent = result.value
                val params = buildMap {
                    if (intent.person.isNotBlank()) put("person", intent.person)
                    if (intent.query.isNotBlank()) put("query", intent.query)
                    if (intent.message.isNotBlank()) put("message", intent.message)
                    if (intent.reply.isNotBlank()) put("reply", intent.reply)
                }
                listOf(Candidate(action = intent.action, params = params, source = CandidateSource.LLM, confidenceHint = 0.7f))
            }
            is Outcome.Failure -> emptyList()
            Outcome.Loading -> emptyList()
        }
    }
}
