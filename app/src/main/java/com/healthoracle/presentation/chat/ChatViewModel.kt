package com.healthoracle.presentation.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.healthoracle.data.model.ChatMessage
import com.healthoracle.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val firestore: FirebaseFirestore, // Injecting Firestore to fetch contact details
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<ChatMessage?>(null)
    val replyingToMessage: StateFlow<ChatMessage?> = _replyingToMessage.asStateFlow()

    // NEW: StateFlow to hold the contact's profile picture URL
    private val _contactProfileUrl = MutableStateFlow<String?>(null)
    val contactProfileUrl: StateFlow<String?> = _contactProfileUrl.asStateFlow()

    val patientId: String = checkNotNull(savedStateHandle["patientId"]) { "patientId is required" }
    val doctorId: String = checkNotNull(savedStateHandle["doctorId"]) { "doctorId is required" }

    val currentUserId = Firebase.auth.currentUser?.uid ?: ""

    init {
        listenForMessages()
        fetchContactProfile() // Fetch the profile picture on load
    }

    private fun fetchContactProfile() {
        viewModelScope.launch {
            val contactId = if (currentUserId == patientId) doctorId else patientId
            try {
                val doc = firestore.collection("users").document(contactId).get().await()
                _contactProfileUrl.value = doc.getString("profilePictureUrl")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun listenForMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.getMessages(patientId, doctorId)
                .catch { e ->
                    e.printStackTrace()
                    _isLoading.value = false
                }
                .collect { messageList ->
                    _messages.value = messageList
                    _isLoading.value = false
                    markMessagesAsSeen()
                }
        }
    }

    fun markMessagesAsSeen() {
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            chatRepository.markMessagesAsSeen(patientId, doctorId, currentUserId)
        }
    }

    fun setReplyTo(message: ChatMessage?) {
        _replyingToMessage.value = message
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessageForEveryone(patientId, doctorId, messageId)
        }
    }

    fun sendMessage(messageText: String, imageUri: Uri? = null) {
        val text = messageText.trim()
        if (text.isBlank() && imageUri == null) return
        if (currentUserId.isEmpty()) return

        val receiverId = if (currentUserId == patientId) doctorId else patientId

        val replyTo = _replyingToMessage.value
        _replyingToMessage.value = null

        viewModelScope.launch {
            try {
                var uploadedImageUrl: String? = null
                if (imageUri != null) {
                    uploadedImageUrl = chatRepository.uploadChatImageToCloudinary(imageUri)
                }

                chatRepository.sendMessage(
                    patientId = patientId,
                    doctorId = doctorId,
                    senderId = currentUserId,
                    receiverId = receiverId,
                    messageText = text,
                    imageUrl = uploadedImageUrl,
                    replyToMessageId = replyTo?.messageId,
                    replyToMessageText = replyTo?.messageText,
                    replyToMessageSender = replyTo?.senderId
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}