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

    // Automatically pulls appointments and groups them by their LocalDate
    val appointments: StateFlow<Map<LocalDate, List<AppointmentEntity>>> = appointmentDao.getAllAppointments()
        .map { list ->
            list.groupBy { LocalDate.parse(it.date) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun addAppointment(title: String, time: String, date: LocalDate) {
        viewModelScope.launch {
            appointmentDao.insertAppointment(
                AppointmentEntity(title = title, time = time, date = date.toString())
            )
        }
    }

    fun deleteAppointment(appointment: AppointmentEntity) {
        viewModelScope.launch {
            appointmentDao.deleteAppointment(appointment)
        }
    }
}