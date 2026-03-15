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

    init {
        // Automatically fetch cloud appointments when the ViewModel initializes
        downloadFromCloud { _, _ -> }
    }

    fun addAppointment(title: String, time: String, date: LocalDate, category: String, description: String, onSaved: (Int) -> Unit) {
        viewModelScope.launch {
            // 1. Insert locally and get the auto-generated ID
            val generatedId = appointmentDao.insertAppointment(
                AppointmentEntity(title = title, time = time, date = date.toString(), category = category, description = description)
            )

            // 2. Automatically sync this new appointment to Firebase
            val userId = Firebase.auth.currentUser?.uid
            if (userId != null) {
                val appointmentMap = mapOf(
                    "title" to title,
                    "time" to time,
                    "date" to date.toString(),
                    "category" to category,
                    "description" to description
                )
                firestore.collection("users").document(userId)
                    .collection("appointments").document(generatedId.toString())
                    .set(appointmentMap)
            }

            // 3. Return alarm ID for notification
            val alarmId = (title + time + date.toString()).hashCode()
            onSaved(alarmId)
        }
    }

    fun deleteAppointment(appointment: AppointmentEntity, onDeleted: (Int) -> Unit) {
        viewModelScope.launch {
            appointmentDao.deleteAppointment(appointment)

            val userId = Firebase.auth.currentUser?.uid
            if (userId != null) {
                firestore.collection("users").document(userId)
                    .collection("appointments").document(appointment.id.toString())
                    .delete()
            }

            val alarmId = (appointment.title + appointment.time + appointment.date).hashCode()
            onDeleted(alarmId)
        }
    }

    // Kept for manual force-sync if you still want a button, but no longer strictly required
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

                firestore.collection("users").document(userId)
                    .collection("appointments")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val batch = firestore.batch()

                        for (doc in snapshot.documents) {
                            if (!localIds.contains(doc.id)) {
                                batch.delete(doc.reference)
                            }
                        }

                        localAppointments.forEach { appt ->
                            val docRef = firestore.collection("users").document(userId)
                                .collection("appointments").document(appt.id.toString())

                            val data = mapOf(
                                "title" to appt.title,
                                "time" to appt.time,
                                "date" to appt.date,
                                "category" to appt.category,
                                "description" to appt.description
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
                            val category = document.getString("category") ?: "Checkup"
                            val description = document.getString("description") ?: ""

                            val id = document.id.toIntOrNull() ?: 0

                            appointmentDao.insertAppointment(
                                AppointmentEntity(id = id, title = title, time = time, date = date, category = category, description = description)
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