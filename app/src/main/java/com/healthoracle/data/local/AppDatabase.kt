package com.healthoracle.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.healthoracle.data.local.dao.AppointmentDao
import com.healthoracle.data.local.entity.AppointmentEntity

@Database(entities = [AppointmentEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val appointmentDao: AppointmentDao
}