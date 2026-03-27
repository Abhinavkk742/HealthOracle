package com.healthoracle.presentation.todo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthoracle.data.local.dao.AppointmentDao
import com.healthoracle.data.local.dao.TodoDao
import com.healthoracle.data.local.entity.TodoEntity
import com.healthoracle.widget.TodoWidgetUpdater
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
            // 1. Remove tasks from previous days to keep the list clean
            todoDao.deleteStaleTodays(today)

            // 2. Sync today's tasks with appointments without duplicating
            val appointments = appointmentDao.getAppointmentsList().filter { it.date == today }
            val currentTodos = todoDao.getTodosForDateSync(today)
            val currentApptIds = currentTodos.map { it.appointmentId }.toSet()

            // Only add appointments that don't already have a corresponding task
            val newTasks = appointments
                .filter { it.id !in currentApptIds }
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

            if (newTasks.isNotEmpty()) {
                todoDao.insertTodos(newTasks)
            }
        }
    }

    fun toggleDone(todo: TodoEntity) {
        viewModelScope.launch {
            todoDao.setDone(todo.id, !todo.isDone)
            // Refresh widgets so the change is reflected immediately on the home screen
            TodoWidgetUpdater.enqueue(context)
        }
    }

    fun refreshTodos() {
        viewModelScope.launch {
            // Re-sync with appointments while preserving completion status of existing tasks
            val appointments = appointmentDao.getAppointmentsList().filter { it.date == today }
            val currentTodos = todoDao.getTodosForDateSync(today)
            val currentApptIds = currentTodos.map { it.appointmentId }.toSet()

            val tasksToAdd = appointments
                .filter { it.id !in currentApptIds }
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

            if (tasksToAdd.isNotEmpty()) {
                todoDao.insertTodos(tasksToAdd)
            }
        }
    }
}