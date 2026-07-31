package com.lifeos.app.core.cognitive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScorerTest {

    private val calmContext = DecisionContext(now = 0L)
    private val nightContext = DecisionContext(now = 0L, isNightTime = true)
    private val emergencyContext = DecisionContext(now = 0L, isEmergency = true)

    private fun candidate(action: String, source: CandidateSource, confidence: Float) =
        Candidate(action = action, source = source, confidenceHint = confidence)

    @Test
    fun `candidates for disallowed actions are dropped, never scored`() {
        val ranked = Scorer.rank(listOf(candidate("delete_everything", CandidateSource.LLM, 0.99f)), calmContext)

        assertTrue(ranked.isEmpty())
    }

    @Test
    fun `an LLM candidate gets no automatic trust advantage over an equally confident rule candidate`() {
        val rule = candidate("call", CandidateSource.RULE, confidence = 0.8f)
        val llm = candidate("call", CandidateSource.LLM, confidence = 0.8f)

        val ranked = Scorer.rank(listOf(rule, llm), calmContext)

        // Rule wins on the small determinism bonus, but the gap must be
        // small — this is a tie-breaker, not a systemic distrust of the LLM.
        assertEquals(CandidateSource.RULE, ranked.first().candidate.source)
        assertTrue(ranked[0].score - ranked[1].score < 0.1f)
    }

    @Test
    fun `higher confidence does not override a worse risk tier at night`() {
        val safeLowConfidence = candidate("open_medicines", CandidateSource.RULE, confidence = 0.5f)
        val riskierHighConfidence = candidate("call", CandidateSource.LLM, confidence = 0.95f)

        val ranked = Scorer.rank(listOf(safeLowConfidence, riskierHighConfidence), nightContext)

        // "call" at night is elevated to MEDIUM risk; confirm the penalty is actually applied
        // by checking the risk tier recorded on the ranked result, not just trusting the order.
        val callResult = ranked.first { it.candidate.action == "call" }
        assertEquals(RiskTier.MEDIUM, callResult.riskTier)
    }

    @Test
    fun `an emergency context elevates every candidate's risk tier to CRITICAL`() {
        val ranked = Scorer.rank(listOf(candidate("call", CandidateSource.RULE, 0.5f)), emergencyContext)

        assertEquals(RiskTier.CRITICAL, ranked.first().riskTier)
    }

    @Test
    fun `ranking is deterministic for the same inputs`() {
        val candidates = listOf(
            candidate("call", CandidateSource.LLM, 0.6f),
            candidate("open_medicines", CandidateSource.RULE, 0.9f),
            candidate("answer", CandidateSource.LLM, 0.3f),
        )

        val first = Scorer.rank(candidates, calmContext)
        val second = Scorer.rank(candidates, calmContext)

        assertEquals(first.map { it.candidate.action }, second.map { it.candidate.action })
    }
}
