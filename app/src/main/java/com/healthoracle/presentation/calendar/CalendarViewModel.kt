package com.healthoracle.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
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
    private val appointmentDao: AppointmentDao,
    private val firestore: FirebaseFirestore
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

    fun syncToCloud(onComplete: (Boolean, String) -> Unit) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
            onComplete(false, "Please log in to sync appointments to the cloud.")
            return
        }

        viewModelScope.launch {
            try {
                val localAppointments = appointmentDao.getAppointmentsList()
                val batch = firestore.batch()

                localAppointments.forEach { appt ->
                    val docRef = firestore.collection("users").document(userId)
                        .collection("appointments").document(appt.id.toString())

                    val data = mapOf(
                        "title" to appt.title,
                        "time" to appt.time,
                        "date" to appt.date
                    )
                    batch.set(docRef, data)
                }

                batch.commit()
                    .addOnSuccessListener {
                        onComplete(true, "Successfully synced to Firebase!")
                    }
                    .addOnFailureListener { e ->
                        onComplete(false, "Sync failed: ${e.message}")
                    }
            } catch (e: Exception) {
                onComplete(false, "An error occurred: ${e.message}")
            }
        }
    }

    // NEW: Download appointments from Firestore and save them locally
    fun downloadFromCloud(onComplete: (Boolean, String) -> Unit) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
            onComplete(false, "Please log in to restore appointments.")
            return
        }

        firestore.collection("users").document(userId)
            .collection("appointments")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onComplete(true, "No appointments found in the cloud to restore.")
                    return@addOnSuccessListener
                }

                viewModelScope.launch {
                    try {
                        for (document in result) {
                            val title = document.getString("title") ?: continue
                            val time = document.getString("time") ?: continue
                            val date = document.getString("date") ?: continue

                            // Parse the document ID back to an Int so Room can replace duplicates correctly
                            val id = document.id.toIntOrNull() ?: 0

                            appointmentDao.insertAppointment(
                                AppointmentEntity(id = id, title = title, time = time, date = date)
                            )
                        }
                        onComplete(true, "Successfully restored appointments from Firebase!")
                    } catch (e: Exception) {
                        onComplete(false, "Error saving restored appointments: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                onComplete(false, "Failed to restore: ${e.message}")
            }
    }
}