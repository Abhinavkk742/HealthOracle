package com.healthoracle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "walk_sessions")
data class WalkSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val distanceMeters: Float = 0f,
    val steps: Int = 0,
    val durationSeconds: Long = 0L,
    val routePointsJson: String = "" // JSON array of lat/lng points
)