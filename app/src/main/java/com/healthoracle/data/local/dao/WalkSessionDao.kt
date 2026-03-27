package com.healthoracle.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.healthoracle.data.local.entity.WalkSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkSessionDao {
    @Query("SELECT * FROM walk_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WalkSession>>

    // CRITICAL FIX: Add OnConflictStrategy.REPLACE and 'suspend'
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WalkSession): Long

    @Delete
    suspend fun delete(session: WalkSession)

    @Query("SELECT * FROM walk_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): WalkSession?
}