package com.healthoracle.data.model

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val messageText: String = "",
    val timestamp: Long = System.currentTimeMillis(),

    val status: String = "sent", // Can be "sent", "delivered", "seen"
    val imageUrl: String? = null,
    val replyToMessageText: String? = null,
    val replyToMessageSender: String? = null,

    // NEW: Track if the message was deleted
    val isDeleted: Boolean = false
)

data class ChatThread(
    val threadId: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis()
)

data class UserAccount(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "patient",
    val assignedDoctorId: String? = null
)