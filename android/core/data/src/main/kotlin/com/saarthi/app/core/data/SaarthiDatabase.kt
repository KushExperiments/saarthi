package com.saarthi.app.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MedicineEntity::class, ContactEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SaarthiDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun contactDao(): ContactDao
}
