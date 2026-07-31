package com.lifeos.app.feature.voice

import com.lifeos.app.core.ai.AiIntent
import com.lifeos.app.core.ai.AiProvider
import com.lifeos.app.core.ai.UnderstandRequest
import com.lifeos.app.core.cognitive.DecisionEngine
import com.lifeos.app.core.cognitive.DecisionTraceLogger
import com.lifeos.app.core.cognitive.LlmCandidateGenerator
import com.lifeos.app.core.cognitive.RuleBasedCandidateGenerator
import com.lifeos.app.core.cognitive.SafetyValidator
import com.lifeos.app.core.common.Outcome
import com.lifeos.app.core.interaction.ClarificationEngine
import com.lifeos.app.core.interaction.DialogueContext
import com.lifeos.app.core.interaction.DialogueResult
import com.lifeos.app.core.interaction.NamedEntity
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceDialogueManagerTest {

    private val aiProvider = mockk<AiProvider>()
    private val clarificationEngine = mockk<ClarificationEngine>()
    private val traceLogger = mockk<DecisionTraceLogger>()

    private fun manager(): VoiceDialogueManager {
        coJustRun { traceLogger.log(any(), any(), any(), any(), any(), any(), any()) }
        val decisionEngine = DecisionEngine(
            RuleBasedCandidateGenerator(),
            LlmCandidateGenerator(aiProvider),
            SafetyValidator(),
            traceLogger,
        )
        return VoiceDialogueManager(decisionEngine, clarificationEngine)
    }

    @Test
    fun `an action the LLM never even needs to weigh in on still resolves via the rule floor tier`() = runTest {
        // The rule-based generator alone recognizes "medicine" — no AI call needed to win.
        coEvery { aiProvider.understand(any()) } returns Outcome.Failure(IllegalStateException("no key configured"))

        val result = manager().handle("show my medicines", DialogueContext())

        assertTrue(result is DialogueResult.Act)
        assertEquals("open_medicines", (result as DialogueResult.Act).plan.action)
    }

    @Test
    fun `a needs_contact resolution routes to the ClarificationEngine, not a generic apology`() = runTest {
        coEvery { aiProvider.understand(any()) } returns Outcome.Failure(IllegalStateException("no key"))
        coEvery { clarificationEngine.clarify(null, emptyList()) } returns
            DialogueResult.Clarify("Who did you mean?", emptyList())

        val result = manager().handle("call someone", DialogueContext(knownEntities = emptyList()))

        assertTrue(result is DialogueResult.Clarify)
    }

    @Test
    fun `an AI-only answer surfaces as a Respond, using its reply text`() = runTest {
        coEvery { aiProvider.understand(any()) } returns Outcome.Success(
            AiIntent(action = "answer", reply = "It's a lovely day today."),
        )

        val result = manager().handle("what's the weather like", DialogueContext())

        assertTrue(result is DialogueResult.Respond)
        assertEquals("It's a lovely day today.", (result as DialogueResult.Respond).text)
    }

    @Test
    fun `nothing either generator can make sense of resolves to Unhandled, never a fabricated action`() = runTest {
        coEvery { aiProvider.understand(any()) } returns Outcome.Failure(IllegalStateException("no key"))

        val result = manager().handle("xyzzy plugh", DialogueContext())

        assertEquals(DialogueResult.Unhandled, result)
    }

    @Test
    fun `an entity known in this conversation is passed through to the LLM as a known person`() = runTest {
        val requestSlot = mutableListOf<UnderstandRequest>()
        coEvery { aiProvider.understand(capture(requestSlot)) } returns
            Outcome.Success(AiIntent(action = "none"))

        manager().handle("hello", DialogueContext(knownEntities = listOf(NamedEntity("1", "Beta", "contact"))))

        assertEquals(listOf("Beta"), requestSlot.first().knownPeople)
    }
}
