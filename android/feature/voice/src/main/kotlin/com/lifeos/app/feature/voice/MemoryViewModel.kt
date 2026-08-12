package com.lifeos.app.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.data.MemoryNodeEntity
import com.lifeos.app.core.memory.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val repository: MemoryRepository,
) : ViewModel() {

    val memories: StateFlow<List<MemoryNodeEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** [ConfidenceModel.correct] always resets confidence to USER_STATED — a person corrects, they don't have to argue with the system. */
    fun correct(node: MemoryNodeEntity, newValue: String) {
        if (newValue.isBlank()) return
        viewModelScope.launch {
            repository.correct(node.id, newValue.trim(), System.currentTimeMillis())
        }
    }

    fun forget(node: MemoryNodeEntity) {
        viewModelScope.launch {
            repository.forget(node.id, reason = "Deleted from Memory screen", now = System.currentTimeMillis())
        }
    }
}
