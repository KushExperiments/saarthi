package com.lifeos.app.feature.medicines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.app.core.common.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MedicinesViewModel @Inject constructor(
    private val repository: MedicineRepository,
    private val scheduler: ReminderScheduler,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    val medicines: StateFlow<List<Medicine>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addMedicine(name: String, times: List<String>) {
        if (name.isBlank() || times.isEmpty()) return
        val medicine = Medicine(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            times = times.sorted(),
            confirmedDate = null,
            confirmedTimes = emptyList(),
        )
        viewModelScope.launch(dispatchers.io) {
            repository.save(medicine)
            scheduler.scheduleAll(medicine)
        }
    }

    fun markTaken(medicine: Medicine) {
        viewModelScope.launch(dispatchers.io) {
            medicine.times.forEach { time ->
                repository.markTaken(medicine.id, time)
                scheduler.confirmedStopNagging(medicine.id, time)
            }
        }
    }

    fun delete(medicine: Medicine) {
        viewModelScope.launch(dispatchers.io) {
            scheduler.cancelAll(medicine)
            repository.delete(medicine)
        }
    }
}
