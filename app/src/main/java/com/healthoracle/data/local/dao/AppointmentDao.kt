package com.healthoracle.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.healthoracle.data.local.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    // Fetch a one-time list of appointments specifically for cloud syncing
    @Query("SELECT * FROM appointments")
    suspend fun getAppointmentsList(): List<AppointmentEntity>

    // Return Long so we can get the auto-generated ID for Firebase sync
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity): Long

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)
}