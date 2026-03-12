package com.healthoracle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val time: String,
    val date: String,
    val category: String = "Checkup",
    val description: String = "" // NEW: Stores the specific AI advice or user notes
)