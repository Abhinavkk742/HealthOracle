package com.healthoracle.presentation.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthoracle.data.local.dao.AppointmentDao
import com.healthoracle.data.local.dao.TodoDao
import com.healthoracle.data.local.entity.TodoEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoDao: TodoDao,
    private val appointmentDao: AppointmentDao
) : ViewModel() {

    private val today: String = LocalDate.now().toString()

    val todos: StateFlow<List<TodoEntity>> = todoDao
        .getTodosForDate(today)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            // 1. Remove stale todos from previous days
            todoDao.deleteStaleTodays(today)

            // 2. Seed today's todos from appointments (IGNORE conflict keeps existing isDone state)
            val todayAppointments = appointmentDao.getAppointmentsList()
                .filter { it.date == today }

            val seeds = todayAppointments.map { appt ->
                TodoEntity(
                    appointmentId = appt.id,
                    title = appt.title,
                    time = appt.time,
                    date = appt.date,
                    category = appt.category,
                    isDone = false
                )
            }
            if (seeds.isNotEmpty()) {
                todoDao.insertTodos(seeds)
            }
        }
    }

    fun toggleDone(todo: TodoEntity) {
        viewModelScope.launch {
            todoDao.setDone(todo.id, !todo.isDone)
        }
    }

    /** Called after a new appointment is added so the todo list updates immediately */
    fun refreshTodos() {
        viewModelScope.launch {
            todoDao.deleteTodosForDate(today)
            val todayAppointments = appointmentDao.getAppointmentsList()
                .filter { it.date == today }
            val seeds = todayAppointments.map { appt ->
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