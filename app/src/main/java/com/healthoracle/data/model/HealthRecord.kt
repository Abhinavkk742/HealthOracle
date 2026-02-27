package com.healthoracle.data.model

data class HealthRecord(
    val id: String = "", // Firestore document ID
    val title: String = "",
    val description: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val type: String = "AI Plan"
)