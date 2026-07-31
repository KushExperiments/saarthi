package com.lifeos.app.core.cognitive

class FakeDecisionTraceLogger : DecisionTraceLogger {
    var lastChosenAction: String? = null
        private set
    var lastRejectedAlternatives: List<Candidate> = emptyList()
        private set
    var callCount: Int = 0
        private set

    override suspend fun log(
        timestamp: Long,
        reasoningTypesFired: List<String>,
        contextSummary: String,
        memoryProvenanceIds: List<String>,
        rejectedAlternatives: List<Candidate>,
        chosenAction: String,
        chosenConfidence: Float,
    ) {
        lastChosenAction = chosenAction
        lastRejectedAlternatives = rejectedAlternatives
        callCount++
    }
}
