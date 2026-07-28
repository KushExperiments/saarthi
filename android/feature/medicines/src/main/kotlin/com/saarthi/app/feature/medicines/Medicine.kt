package com.saarthi.app.feature.medicines

import com.saarthi.app.core.data.MedicineEntity
import java.time.LocalDate

data class Medicine(
    val id: String,
    val name: String,
    val times: List<String>, // "HH:mm", 24h
    val confirmedDate: String?,
    val confirmedTimes: List<String>,
) {
    fun isConfirmed(time: String): Boolean =
        confirmedDate == today() && confirmedTimes.contains(time)

    companion object {
        fun today(): String = LocalDate.now().toString()
    }
}

internal fun MedicineEntity.toDomain(): Medicine = Medicine(
    id = id,
    name = name,
    times = timesCsv.split(",").filter { it.isNotBlank() },
    confirmedDate = confirmedDate,
    confirmedTimes = confirmedTimesCsv.split(",").filter { it.isNotBlank() },
)

internal fun Medicine.toEntity(): MedicineEntity = MedicineEntity(
    id = id,
    name = name,
    timesCsv = times.joinToString(","),
    confirmedDate = confirmedDate,
    confirmedTimesCsv = confirmedTimes.joinToString(","),
)
