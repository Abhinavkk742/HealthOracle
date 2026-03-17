package com.healthoracle.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.healthoracle.data.model.ChatMessage
import com.healthoracle.data.model.UserAccount
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Generates a consistent Thread ID for a doctor-patient pair
    private fun getThreadId(patientId: String, doctorId: String): String {
        // Sort IDs to ensure the thread ID is the same regardless of who initiates
        val ids = listOf(patientId, doctorId).sorted()
        return "${ids[0]}_${ids[1]}"
    }

    /**
     * Sends a message to Firestore.
     */
    suspend fun sendMessage(
        patientId: String,
        doctorId: String,
        senderId: String,
        receiverId: String,
        messageText: String
    ): Boolean {
        return try {
            val threadId = getThreadId(patientId, doctorId)
            val messageId = UUID.randomUUID().toString()

            val chatMessage = ChatMessage(
                messageId = messageId,
                senderId = senderId,
                receiverId = receiverId,
                messageText = messageText,
                timestamp = System.currentTimeMillis()
            )

            // Save message in the subcollection of the thread
            firestore.collection("chats")
                .document(threadId)
                .collection("messages")
                .document(messageId)
                .set(chatMessage)
                .await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Listens to messages in real-time using Kotlin Flow.
     */
    fun getMessages(patientId: String, doctorId: String): Flow<List<ChatMessage>> = callbackFlow {
        val threadId = getThreadId(patientId, doctorId)

        val listenerRegistration = firestore.collection("chats")
            .document(threadId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Get newest first for the UI
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)
                    }
                    trySend(messages)
                }
            }

        // Clean up the listener when the Flow is cancelled (e.g., leaving the screen)
        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Fetches all patients assigned to a specific doctor.
     * Useful for the Doctor's Dashboard.
     */
    fun getPatientsForDoctor(doctorId: String): Flow<List<UserAccount>> = callbackFlow {
        val listenerRegistration = firestore.collection("users")
            .whereEqualTo("role", "patient")
            .whereEqualTo("assignedDoctorId", doctorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val patients = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserAccount::class.java)
                    }
                    trySend(patients)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }
}