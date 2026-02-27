package com.healthoracle.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    fun deleteAccount(password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser
        if (user == null || user.email == null) {
            onError("User session not found.")
            return
        }

        viewModelScope.launch {
            _isDeleting.value = true
            try {
                // 1. Re-authenticate the user with their email and the password they typed
                val credential = EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential).await()

                // 2. Delete their personal profile document from Firestore
                val userId = user.uid
                firestore.collection("users").document(userId).delete().await()

                // 3. Delete their actual authentication account
                user.delete().await()

                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to delete account. Check your password.")
            } finally {
                _isDeleting.value = false
            }
        }
    }
}