package com.saarthi.app.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * `timesCsv`/`confirmedTimesCsv` are simple comma-joined "HH:mm" lists —
 * a deliberate simplification for this pass rather than a full adherence
 * history table (Memory §6's Life Timeline design covers that properly;
 * this is just enough to drive the nag-until-confirmed loop today).
 */
@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val timesCsv: String,
    val confirmedDate: String? = null,
    val confirmedTimesCsv: String = "",
)
