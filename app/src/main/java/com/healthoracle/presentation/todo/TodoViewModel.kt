package com.healthoracle.presentation.todo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthoracle.data.local.dao.AppointmentDao
import com.healthoracle.data.local.dao.TodoDao
import com.healthoracle.data.local.entity.TodoEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoDao: TodoDao,
    private val appointmentDao: AppointmentDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    val todos: StateFlow<List<TodoEntity>> = todoDao
        .getTodosForDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            todoDao.deleteStaleTodays(today)

            val seeds = appointmentDao.getAppointmentsList()
                .filter { it.date == today }
                .map { appt ->
                    TodoEntity(
                        appointmentId = appt.id,
                        title = appt.title,
                        time = appt.time,
                        date = appt.date,
                        category = appt.category,
                        isDone = false
                    )
                }
            if (seeds.isNotEmpty()) todoDao.insertTodos(seeds)
        }
    }

    fun toggleDone(todo: TodoEntity) {
        viewModelScope.launch {
            todoDao.setDone(todo.id, !todo.isDone)
        }
    }

    fun refreshTodos() {
        viewModelScope.launch {
            todoDao.deleteTodosForDate(today)
            val seeds = appointmentDao.getAppointmentsList()
                .filter { it.date == today }
                .map { appt ->
                    TodoEntity(
                        appointmentId = appt.id,
                        title = appt.title,
                        time = appt.time,
                        date = appt.date,
                        category = appt.category,
                        isDone = false
                    )
                }
            if (seeds.isNotEmpty()) todoDao.insertTodos(seeds)
        }
    }
}