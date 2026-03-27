package com.healthoracle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.healthoracle.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todos WHERE date = :date ORDER BY time ASC")
    fun getTodosForDate(date: String): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTodos(todos: List<TodoEntity>)

    @Query("UPDATE todos SET isDone = :isDone WHERE id = :id")
    suspend fun setDone(id: Int, isDone: Boolean)

    // Clears all todos for a specific date so we can reseed from appointments
    @Query("DELETE FROM todos WHERE date = :date")
    suspend fun deleteTodosForDate(date: String)

    // Called on app startup: removes todos for any date that is not today
    @Query("DELETE FROM todos WHERE date != :today")
    suspend fun deleteStaleTodays(today: String)
    // Add this to TodoDao.kt
    @Query("SELECT COUNT(*) FROM todos WHERE appointmentId = :appointmentId AND date = :date")
    suspend fun countByAppointmentAndDate(appointmentId: Int, date: String): Int
    @Query("SELECT * FROM todos WHERE date = :date ORDER BY time ASC")
    suspend fun getTodosForDateSync(date: String): List<TodoEntity>
}