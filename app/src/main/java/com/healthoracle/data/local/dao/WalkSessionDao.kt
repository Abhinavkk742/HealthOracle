package com.healthoracle.data.local.dao

import androidx.room.*
import com.healthoracle.data.local.entity.WalkSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkSessionDao {
    @Insert
    suspend fun insert(session: WalkSession): Long

    @Query("SELECT * FROM walk_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WalkSession>>

    @Query("SELECT * FROM walk_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): WalkSession?

    @Delete
    suspend fun delete(session: WalkSession)
}
