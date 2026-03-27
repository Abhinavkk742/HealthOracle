package com.healthoracle.data.repository

import com.healthoracle.data.local.dao.WalkSessionDao
import com.healthoracle.data.local.entity.WalkSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalkRepository @Inject constructor(
    private val walkSessionDao: WalkSessionDao
) {
    fun getAllSessions(): Flow<List<WalkSession>> = walkSessionDao.getAllSessions()

    suspend fun saveSession(session: WalkSession): Long = walkSessionDao.insert(session)

    suspend fun getSessionById(id: Long): WalkSession? = walkSessionDao.getSessionById(id)

    suspend fun deleteSession(session: WalkSession) = walkSessionDao.delete(session)
}
