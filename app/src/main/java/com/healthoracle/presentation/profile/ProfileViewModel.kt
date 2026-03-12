package com.healthoracle.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.healthoracle.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val error: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "User not logged in")
                return@launch
            }

            try {
                val document = firestore.collection("users").document(userId).get().await()
                if (document.exists()) {
                    val profile = document.toObject(UserProfile::class.java) ?: UserProfile(uid = userId)
                    _uiState.value = _uiState.value.copy(isLoading = false, profile = profile)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, profile = UserProfile(uid = userId))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun saveProfile(name: String, dob: String, age: Int, gender: String, height: Float, weight: Float) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveSuccess = false, error = null)

            val currentUser = auth.currentUser
            val userId = currentUser?.uid ?: return@launch

            try {
                // NEW: Update the core Firebase Auth display name so new posts grab the right name!
                val profileUpdates = userProfileChangeRequest {
                    displayName = name
                }
                currentUser.updateProfile(profileUpdates).await()

                // Save to Firestore Database
                val profileToSave = UserProfile(userId, name, dob, age, gender, height, weight)
                firestore.collection("users").document(userId).set(profileToSave).await()

                // Sweep the forum for old posts
                syncNewNameToForum(name)

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    profile = profileToSave
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.localizedMessage)
            }
        }
    }

    private fun syncNewNameToForum(newName: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val userPosts = firestore.collection("forum_posts")
                    .whereEqualTo("authorId", userId)
                    .get().await()

                if (!userPosts.isEmpty) {
                    firestore.runBatch { batch ->
                        userPosts.documents.forEach { doc ->
                            batch.update(doc.reference, "authorName", "u/$newName")
                        }
                    }.await()
                }

                val userComments = firestore.collectionGroup("comments")
                    .whereEqualTo("authorId", userId)
                    .get().await()

                if (!userComments.isEmpty) {
                    firestore.runBatch { batch ->
                        userComments.documents.forEach { doc ->
                            batch.update(doc.reference, "authorName", "u/$newName")
                        }
                    }.await()
                }
            }catch (e: Exception) {
                // Let's make the silent failure incredibly loud!
                android.util.Log.e("FIREBASE_ERROR", "NAME SYNC FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        auth.signOut()
        onLogoutComplete()
    }
}