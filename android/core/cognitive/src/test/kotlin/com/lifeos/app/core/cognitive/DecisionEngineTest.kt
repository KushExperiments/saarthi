package com.lifeos.app.core.cognitive

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DecisionEngineTest {

    private fun engine(
        rule: List<Candidate> = emptyList(),
        llm: List<Candidate> = emptyList(),
        trace: FakeDecisionTraceLogger = FakeDecisionTraceLogger(),
    ) = DecisionEngine(FakeCandidateGenerator(rule), FakeCandidateGenerator(llm), SafetyValidator(), trace) to trace

    private fun candidate(action: String, source: CandidateSource, confidence: Float) =
        Candidate(action = action, source = source, confidenceHint = confidence)

    @Test
    fun `no candidates from either generator yields a none action, never a crash`() = runTest {
        val (engine, _) = engine()

        val plan = engine.decide(DecisionInput(transcript = "gibberish", context = DecisionContext(now = 0L)))

        assertEquals("none", plan.action)
        assertEquals(0f, plan.confidence)
        assertEquals(Verdict.REJECT, plan.verdict)
    }

    @Test
    fun `both generators run and their candidates are merged into one pool`() = runTest {
        val rule = listOf(candidate("open_medicines", CandidateSource.RULE, 0.6f))
        val llm = listOf(candidate("call", CandidateSource.LLM, 0.6f))
        val (engine, _) = engine(rule = rule, llm = llm)

        val plan = engine.decide(DecisionInput(transcript = "x", context = DecisionContext(now = 0L)))

        // open_medicines (HEALTH goal, weight 0.8) should outrank call (SOCIAL, weight 0.4) at equal confidence.
        assertEquals("open_medicines", plan.action)
    }

    @Test
    fun `a rejected candidate never becomes the chosen action, even if it was the only one offered`() = runTest {
        val rule = listOf(candidate("not_a_real_action", CandidateSource.RULE, 0.99f))
        val (engine, _) = engine(rule = rule)

        val plan = engine.decide(DecisionInput(transcript = "x", context = DecisionContext(now = 0L)))

        assertEquals("none", plan.action)
    }

    @Test
    fun `an escalated candidate is still chosen as the plan, with the escalation on a field a caller must check`() = runTest {
        val rule = listOf(candidate("whatsapp", CandidateSource.RULE, 0.8f))
        val (engine, _) = engine(rule = rule)

        val plan = engine.decide(
            DecisionInput(transcript = "whatsapp beta", context = DecisionContext(now = 0L)),
        )

        assertEquals("whatsapp", plan.action)
        // The structured field is what a caller must gate on — the text is audit trail, not the contract.
        assertEquals(Verdict.ESCALATE, plan.verdict)
        assertTrue(plan.explanation.contains("confirmation"))
    }

    @Test
    fun `every decision is traced exactly once, including a none outcome`() = runTest {
        val trace = FakeDecisionTraceLogger()
        val (engine, _) = engine(trace = trace)

        engine.decide(DecisionInput(transcript = "x", context = DecisionContext(now = 0L)))

        assertEquals(1, trace.callCount)
        assertEquals("none", trace.lastChosenAction)
    }

    @Test
    fun `runners-up are surfaced as alternatives on the plan, not silently dropped`() = runTest {
        val rule = listOf(
            candidate("open_medicines", CandidateSource.RULE, 0.9f),
            candidate("call", CandidateSource.RULE, 0.1f),
        )
        val (engine, _) = engine(rule = rule)

        val plan = engine.decide(DecisionInput(transcript = "x", context = DecisionContext(now = 0L)))

        assertEquals("open_medicines", plan.action)
        assertEquals(1, plan.alternatives.size)
        assertEquals("call", plan.alternatives.first().action)
    }

    @Test
    fun `the trace logs candidates SafetyValidator actually rejected, not just runners-up that were merely outscored`() = runTest {
        val rule = listOf(
            candidate("open_medicines", CandidateSource.RULE, 0.6f),
            candidate("not_a_real_action", CandidateSource.RULE, 0.99f),
        )
        val trace = FakeDecisionTraceLogger()
        val (engine, _) = engine(rule = rule, trace = trace)

        val plan = engine.decide(DecisionInput(transcript = "x", context = DecisionContext(now = 0L)))

        assertEquals("open_medicines", plan.action)
        assertTrue(plan.alternatives.isEmpty()) // no runner-up survived validation to rank against
        assertEquals(listOf("not_a_real_action"), trace.lastRejectedAlternatives.map { it.action })
    }
}
