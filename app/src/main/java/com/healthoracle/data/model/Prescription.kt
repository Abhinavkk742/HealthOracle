package com.healthoracle.data.model

data class Prescription(
    val id: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val imageUrl: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)