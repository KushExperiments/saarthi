package com.lifeos.app.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.cognitive.DecisionTraceReader
import com.lifeos.app.core.ui.DecisionExplanation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DecisionExplanationViewModel @Inject constructor(
    private val decisionTraceReader: DecisionTraceReader,
) : ViewModel() {

    private val _explanation = MutableStateFlow<DecisionExplanation?>(null)
    val explanation: StateFlow<DecisionExplanation?> = _explanation

    private val _hasRecentDecision = MutableStateFlow(true)
    val hasRecentDecision: StateFlow<Boolean> = _hasRecentDecision

    fun show() {
        viewModelScope.launch {
            val recent = decisionTraceReader.mostRecent()
            _hasRecentDecision.value = recent != null
            _explanation.value = recent?.let {
                DecisionExplanation(
                    action = it.action,
                    confidence = it.confidence,
                    reasoning = listOf(it.contextSummary) + it.reasoningTypesFired.map { type -> "Considered a $type-generated option" },
                    alternatives = it.rejectedAlternatives,
                )
            }
        }
    }

    fun dismiss() {
        _explanation.value = null
    }
}
