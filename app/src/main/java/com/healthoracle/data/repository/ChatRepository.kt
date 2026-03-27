package com.healthoracle.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.healthoracle.data.model.ChatMessage
import com.healthoracle.data.model.UserAccount
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    private fun getThreadId(patientId: String, doctorId: String): String {
        val ids = listOf(patientId, doctorId).sorted()
        return "${ids[0]}_${ids[1]}"
    }

    suspend fun uploadChatImageToCloudinary(imageUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("Cloudinary", "Starting image upload...")
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val tempFile = File(context.cacheDir, "upload_temp_image_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                val client = OkHttpClient()
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", tempFile.name, tempFile.asRequestBody("image/*".toMediaTypeOrNull()))
                    .addFormDataPart("upload_preset", "krfgajle") // TODO: YOUR PRESET
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/dpj8tzdte/image/upload") // TODO: YOUR CLOUD NAME
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                tempFile.delete()

                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    return@withContext jsonObject.getString("secure_url")
                } else {
                    Log.e("Cloudinary", "Upload failed: $responseBody")
                }
            } catch (e: Exception) {
                Log.e("Cloudinary", "Exception during upload", e)
            }
            return@withContext null
        }
    }

    suspend fun sendMessage(
        patientId: String,
        doctorId: String,
        senderId: String,
        receiverId: String,
        messageText: String,
        imageUrl: String? = null,
        replyToMessageId: String? = null,
        replyToMessageText: String? = null,
        replyToMessageSender: String? = null
    ): Boolean {
        return try {
            val threadId = getThreadId(patientId, doctorId)
            val messageId = UUID.randomUUID().toString()

            val chatMessage = ChatMessage(
                messageId = messageId,
                senderId = senderId,
                receiverId = receiverId,
                messageText = messageText,
                timestamp = System.currentTimeMillis(),
                status = "sent",
                imageUrl = imageUrl,
                replyToMessageId = replyToMessageId,
                replyToMessageText = replyToMessageText,
                replyToMessageSender = replyToMessageSender,
                isDeleted = false
            )

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

    suspend fun deleteMessageForEveryone(patientId: String, doctorId: String, messageId: String) {
        try {
            val threadId = getThreadId(patientId, doctorId)
            val messagesRef = firestore.collection("chats")
                .document(threadId)
                .collection("messages")

            // 1. Delete the actual message
            messagesRef.document(messageId).update(
                mapOf(
                    "isDeleted" to true,
                    "messageText" to "",
                    "imageUrl" to null
                )
            ).await()

            // 2. Cascade Update: Find any messages that replied to this exact message ID
            val repliesQuery = messagesRef.whereEqualTo("replyToMessageId", messageId).get().await()
            if (!repliesQuery.isEmpty) {
                val batch = firestore.batch()
                for (doc in repliesQuery.documents) {
                    batch.update(doc.reference, "replyToMessageText", "Deleted Message")
                }
                batch.commit().await()
                Log.d("ChatRepo", "Successfully updated ${repliesQuery.size()} child replies.")
            }

        } catch (e: Exception) {
            Log.e("ChatRepo", "Failed to delete message", e)
        }
    }

    suspend fun markMessagesAsSeen(patientId: String, doctorId: String, currentUserId: String) {
        try {
            val threadId = getThreadId(patientId, doctorId)
            val unreadMessages = firestore.collection("chats")
                .document(threadId)
                .collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .get().await()

            if (unreadMessages.isEmpty) return

            val batch = firestore.batch()
            var hasUpdates = false
            for (doc in unreadMessages.documents) {
                if (doc.getString("status") != "seen") {
                    batch.update(doc.reference, "status", "seen")
                    hasUpdates = true
                }
            }
            if (hasUpdates) {
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e("ChatRepo", "Failed to mark as seen", e)
        }
    }

    fun getMessages(patientId: String, doctorId: String): Flow<List<ChatMessage>> = callbackFlow {
        val threadId = getThreadId(patientId, doctorId)

        val listenerRegistration = firestore.collection("chats")
            .document(threadId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
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

        awaitClose {
            listenerRegistration.remove()
        }
    }

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