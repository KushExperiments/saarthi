package com.lifeos.app.core.cognitive

import com.lifeos.app.core.data.DecisionTraceDao
import javax.inject.Inject

private const val DELIMITER = "|"

class RoomDecisionTraceReader @Inject constructor(
    private val dao: DecisionTraceDao,
) : DecisionTraceReader {

    override suspend fun mostRecent(): RecentDecision? {
        val trace = dao.mostRecent() ?: return null
        return RecentDecision(
            action = trace.chosenAction,
            confidence = trace.chosenConfidence,
            contextSummary = trace.contextSummary,
            reasoningTypesFired = trace.reasoningTypesFiredCsv.split(DELIMITER).filter { it.isNotBlank() },
            rejectedAlternatives = trace.rejectedAlternativesCsv.split(DELIMITER).filter { it.isNotBlank() },
        )
    }
}
