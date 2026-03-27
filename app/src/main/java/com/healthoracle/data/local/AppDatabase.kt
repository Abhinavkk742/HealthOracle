package com.healthoracle.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.healthoracle.data.local.dao.AppointmentDao
import com.healthoracle.data.local.dao.WalkSessionDao
import com.healthoracle.data.local.entity.AppointmentEntity
import com.healthoracle.data.local.entity.WalkSession

@Database(
    entities = [
        AppointmentEntity::class,
        WalkSession::class          // ← THIS WAS MISSING
    ],
    version = 2,                    // ← INCREMENTED from 1 to 2
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appointmentDao(): AppointmentDao
    abstract fun walkSessionDao(): WalkSessionDao
}
