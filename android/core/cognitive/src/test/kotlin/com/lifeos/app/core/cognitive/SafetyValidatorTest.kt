package com.lifeos.app.core.cognitive

import org.junit.Assert.assertEquals
import org.junit.Test

class SafetyValidatorTest {

    private val validator = SafetyValidator()
    private val calmContext = DecisionContext(now = 0L)

    private fun candidate(action: String) =
        Candidate(action = action, source = CandidateSource.RULE, confidenceHint = 0.9f)

    @Test
    fun `a disallowed action is rejected outright`() {
        val result = validator.validate(candidate("format_phone"), calmContext)

        assertEquals(Verdict.REJECT, result.verdict)
    }

    @Test
    fun `a blank action is rejected by the schema stage before reaching policy`() {
        val result = validator.validate(candidate(""), calmContext)

        assertEquals(Verdict.REJECT, result.verdict)
    }

    @Test
    fun `whatsapp always escalates for human confirmation, per Ethical Policy, even at full confidence`() {
        val result = validator.validate(candidate("whatsapp"), calmContext)

        assertEquals(Verdict.ESCALATE, result.verdict)
    }

    @Test
    fun `an emergency context escalates any non-help action`() {
        val result = validator.validate(candidate("call"), DecisionContext(now = 0L, isEmergency = true))

        assertEquals(Verdict.ESCALATE, result.verdict)
    }

    @Test
    fun `help during an emergency is approved, not escalated, so it's never blocked`() {
        val result = validator.validate(candidate("help"), DecisionContext(now = 0L, isEmergency = true))

        assertEquals(Verdict.APPROVE, result.verdict)
    }

    @Test
    fun `an ordinary low-risk action with no special policy is approved outright`() {
        val result = validator.validate(candidate("torch_on"), calmContext)

        assertEquals(Verdict.APPROVE, result.verdict)
    }

    @Test
    fun `sms also always escalates, matching whatsapp's Ethical Policy treatment`() {
        val result = validator.validate(candidate("sms"), calmContext)

        assertEquals(Verdict.ESCALATE, result.verdict)
    }

    @Test
    fun `medicine confirmation at night reaches HIGH risk and must escalate, not silently approve`() {
        val result = validator.validate(candidate("medicine_taken"), DecisionContext(now = 0L, isNightTime = true))

        assertEquals(Verdict.ESCALATE, result.verdict)
    }

    @Test
    fun `a late-night call is context-inappropriate and escalates, matching Cognitive OS's own example`() {
        val result = validator.validate(candidate("call"), DecisionContext(now = 0L, isNightTime = true))

        assertEquals(Verdict.ESCALATE, result.verdict)
    }

    @Test
    fun `the same action during the day does not escalate`() {
        val result = validator.validate(candidate("call"), calmContext)

        assertEquals(Verdict.APPROVE, result.verdict)
    }
}
