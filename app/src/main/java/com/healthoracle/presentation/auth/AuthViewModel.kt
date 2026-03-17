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
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, pass: String, onSuccess: (isDoctor: Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
                val user = authResult.user

                var isDoctor = false
                if (user != null) {
                    val userDoc = firestore.collection("users").document(user.uid).get().await()
                    isDoctor = userDoc.getString("role") == "doctor"
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess(isDoctor)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Login failed")
            }
        }
    }

    // NEW: Added the 'role' parameter here
    fun signUp(email: String, pass: String, role: String, onSuccess: (isDoctor: Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
                val user = authResult.user

                if (user != null) {
                    val newUserProfile = hashMapOf(
                        "name" to "New User",
                        "email" to (user.email ?: ""),
                        "role" to role, // NEW: Save the selected role!
                        "age" to 0,
                        "gender" to "",
                        "heightCm" to 0f,
                        "weightKg" to 0f,
                        "dob" to ""
                    )
                    firestore.collection("users").document(user.uid).set(newUserProfile).await()
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess(role == "doctor") // Route them correctly based on their choice
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Sign up failed")
            }
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: (isDoctor: Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user

                var isDoctor = false
                if (user != null) {
                    val userDoc = firestore.collection("users").document(user.uid).get().await()

                    if (!userDoc.exists()) {
                        val newUserProfile = hashMapOf(
                            "name" to (user.displayName ?: "New User"),
                            "email" to (user.email ?: ""),
                            "role" to "patient",
                            "age" to 0,
                            "gender" to "",
                            "heightCm" to 0f,
                            "weightKg" to 0f,
                            "dob" to ""
                        )
                        firestore.collection("users").document(user.uid).set(newUserProfile).await()
                    } else {
                        isDoctor = userDoc.getString("role") == "doctor"
                    }
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess(isDoctor)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Google Sign-In failed")
            }
        }
    }
}