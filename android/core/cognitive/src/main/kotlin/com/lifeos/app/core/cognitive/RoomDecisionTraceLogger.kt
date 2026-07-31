package com.lifeos.app.core.cognitive

import com.lifeos.app.core.data.DecisionTraceDao
import com.lifeos.app.core.data.DecisionTraceEntity
import java.util.UUID
import javax.inject.Inject

private const val DELIMITER = "|"

class RoomDecisionTraceLogger @Inject constructor(
    private val dao: DecisionTraceDao,
) : DecisionTraceLogger {

    override suspend fun log(
        timestamp: Long,
        reasoningTypesFired: List<String>,
        contextSummary: String,
        memoryProvenanceIds: List<String>,
        rejectedAlternatives: List<Candidate>,
        chosenAction: String,
        chosenConfidence: Float,
    ) {
        dao.insert(
            DecisionTraceEntity(
                id = UUID.randomUUID().toString(),
                timestamp = timestamp,
                reasoningTypesFiredCsv = reasoningTypesFired.joinToString(DELIMITER),
                contextSummary = contextSummary,
                memoryProvenanceIdsCsv = memoryProvenanceIds.joinToString(DELIMITER),
                rejectedAlternativesCsv = rejectedAlternatives.joinToString(DELIMITER) { "${it.action}:${it.source}" },
                chosenAction = chosenAction,
                chosenConfidence = chosenConfidence,
            ),
        )
    }
}
