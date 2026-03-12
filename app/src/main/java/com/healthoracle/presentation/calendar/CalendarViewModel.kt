package com.healthoracle.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthoracle.data.local.dao.AppointmentDao
import com.healthoracle.data.local.entity.AppointmentEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val appointmentDao: AppointmentDao
) : ViewModel() {

    val appointments: StateFlow<Map<LocalDate, List<AppointmentEntity>>> = appointmentDao.getAllAppointments()
        .map { list ->
            list.groupBy { LocalDate.parse(it.date) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun addAppointment(title: String, time: String, date: LocalDate, onSaved: (Int) -> Unit) {
        viewModelScope.launch {
            appointmentDao.insertAppointment(
                AppointmentEntity(title = title, time = time, date = date.toString())
            )
            // Generate a unique ID to tie this database entry to the AlarmManager
            val alarmId = (title + time + date.toString()).hashCode()
            onSaved(alarmId)
        }
    }

    fun deleteAppointment(appointment: AppointmentEntity, onDeleted: (Int) -> Unit) {
        viewModelScope.launch {
            appointmentDao.deleteAppointment(appointment)
            val alarmId = (appointment.title + appointment.time + appointment.date).hashCode()
            onDeleted(alarmId)
        }
    }
}