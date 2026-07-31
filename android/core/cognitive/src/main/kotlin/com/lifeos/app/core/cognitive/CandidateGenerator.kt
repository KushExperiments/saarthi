package com.lifeos.app.core.cognitive

interface CandidateGenerator {
    suspend fun generate(input: DecisionInput): List<Candidate>
}
