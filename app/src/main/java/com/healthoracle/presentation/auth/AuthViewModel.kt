package com.healthoracle.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore // NEW: We need the database to save the profile!
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                auth.signInWithEmailAndPassword(email.trim(), pass).await()
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Login failed")
            }
        }
    }

    fun signUp(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                auth.createUserWithEmailAndPassword(email.trim(), pass).await()
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Sign up failed")
            }
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 1. Log into Firebase Auth using the Google Token
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user

                // 2. Automatically create their Firestore Profile if it's their first time!
                if (user != null) {
                    val userDoc = firestore.collection("users").document(user.uid).get().await()

                    if (!userDoc.exists()) {
                        val newUserProfile = hashMapOf(
                            "name" to (user.displayName ?: "New User"),
                            "email" to (user.email ?: ""),
                            "age" to 0,
                            "gender" to "",
                            "heightCm" to 0f,
                            "weightKg" to 0f,
                            "dob" to ""
                        )
                        firestore.collection("users").document(user.uid).set(newUserProfile).await()
                    }
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Google Sign-In failed")
            }
        }
    }
}