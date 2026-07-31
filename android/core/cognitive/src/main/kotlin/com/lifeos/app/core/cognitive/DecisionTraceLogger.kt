package com.lifeos.app.core.cognitive

interface DecisionTraceLogger {
    suspend fun log(
        timestamp: Long,
        reasoningTypesFired: List<String>,
        contextSummary: String,
        memoryProvenanceIds: List<String>,
        rejectedAlternatives: List<Candidate>,
        chosenAction: String,
        chosenConfidence: Float,
    )
}
