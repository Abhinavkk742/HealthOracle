package com.healthoracle.data.model

// Represents a single chat message
data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val messageText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// Represents a conversation thread between a doctor and a patient
data class ChatThread(
    val threadId: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis()
)

// Extended User Profile to handle Roles
data class UserAccount(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "patient", // Can be "patient" or "doctor"
    val assignedDoctorId: String? = null // If the user is a patient, this links them to their doctor
)