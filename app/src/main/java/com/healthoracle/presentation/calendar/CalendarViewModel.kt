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
            // 1. Delete locally from Room
            appointmentDao.deleteAppointment(appointment)

            // 2. Proactively delete from Firebase if logged in
            val userId = Firebase.auth.currentUser?.uid
            if (userId != null) {
                firestore.collection("users").document(userId)
                    .collection("appointments").document(appointment.id.toString())
                    .delete()
            }

            // 3. Cancel the local alarm notification
            val alarmId = (appointment.title + appointment.time + appointment.date).hashCode()
            onDeleted(alarmId)
        }
    }

    // Upgraded true-mirror cloud sync
    fun syncToCloud(onComplete: (Boolean, String) -> Unit) {
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
            onComplete(false, "Please log in to sync appointments to the cloud.")
            return
        }

        viewModelScope.launch {
            try {
                val localAppointments = appointmentDao.getAppointmentsList()
                val localIds = localAppointments.map { it.id.toString() }

                // Fetch the current cloud state first
                firestore.collection("users").document(userId)
                    .collection("appointments")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val batch = firestore.batch()

                        // Step 1: Delete appointments from the cloud that no longer exist locally
                        for (doc in snapshot.documents) {
                            if (!localIds.contains(doc.id)) {
                                batch.delete(doc.reference)
                            }
                        }

                        // Step 2: Upload or update all current local appointments
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

                        // Execute the batch (deletes and sets together)
                        batch.commit()
                            .addOnSuccessListener {
                                onComplete(true, "Successfully synced to Firebase!")
                            }
                            .addOnFailureListener { e ->
                                onComplete(false, "Sync failed: ${e.message}")
                            }
                    }
                    .addOnFailureListener { e ->
                        onComplete(false, "Failed to fetch cloud data: ${e.message}")
                    }
            } catch (e: Exception) {
                onComplete(false, "An error occurred: ${e.message}")
            }
        }
    }

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