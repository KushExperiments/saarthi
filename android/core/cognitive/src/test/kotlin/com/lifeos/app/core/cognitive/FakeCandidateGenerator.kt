package com.lifeos.app.core.cognitive

/** No network, fully deterministic — returns whatever was configured regardless of input. */
class FakeCandidateGenerator(private val result: List<Candidate> = emptyList()) : CandidateGenerator {
    override suspend fun generate(input: DecisionInput): List<Candidate> = result
}
