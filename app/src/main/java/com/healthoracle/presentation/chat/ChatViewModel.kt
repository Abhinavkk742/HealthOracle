package com.healthoracle.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.healthoracle.data.model.ChatMessage
import com.healthoracle.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle // Used to grab arguments passed during navigation
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // We assume these are passed via Jetpack Compose Navigation routes
    // e.g., "chat_screen/{patientId}/{doctorId}"
    val patientId: String = checkNotNull(savedStateHandle["patientId"]) { "patientId is required" }
    val doctorId: String = checkNotNull(savedStateHandle["doctorId"]) { "doctorId is required" }

    // Get the currently logged-in user
    val currentUserId = Firebase.auth.currentUser?.uid ?: ""

    init {
        listenForMessages()
    }

    private fun listenForMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.getMessages(patientId, doctorId)
                .catch { e ->
                    // Log error or update an error state if needed
                    e.printStackTrace()
                    _isLoading.value = false
                }
                .collect { messageList ->
                    _messages.value = messageList
                    _isLoading.value = false
                }
        }
    }

    fun sendMessage(messageText: String) {
        if (messageText.isBlank() || currentUserId.isEmpty()) return

        // The receiver is whoever the current user is NOT
        val receiverId = if (currentUserId == patientId) doctorId else patientId

        viewModelScope.launch {
            chatRepository.sendMessage(
                patientId = patientId,
                doctorId = doctorId,
                senderId = currentUserId,
                receiverId = receiverId,
                messageText = messageText.trim()
            )
        }
    }
}