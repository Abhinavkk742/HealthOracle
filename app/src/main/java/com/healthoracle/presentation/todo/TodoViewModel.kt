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
            // Only delete STALE days — never delete today's todos (preserves isDone state)
            todoDao.deleteStaleTodays(today)

            // Only seed if today has NO todos yet (first open of the day)
            val existing = todoDao.getTodosForDateSync(today)
            if (existing.isEmpty()) {
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

    fun toggleDone(todo: TodoEntity) {
        viewModelScope.launch {
            todoDao.setDone(todo.id, !todo.isDone)
            // Keep widget in sync when toggled from the app
            TodoWidgetUpdater.enqueue(context)
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