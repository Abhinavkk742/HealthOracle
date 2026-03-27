package com.healthoracle.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "walk_sessions")
data class WalkSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val distanceMeters: Float,
    val steps: Int,
    val durationSeconds: Long,
    val routePointsJson: String // JSON array of lat/lng points
)
