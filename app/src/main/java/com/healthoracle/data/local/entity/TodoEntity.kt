package com.healthoracle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appointmentId: Int,
    val title: String,
    val time: String,
    val date: String,
    val category: String = "Checkup",
    val isDone: Boolean = false
)