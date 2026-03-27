package com.healthoracle.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.healthoracle.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploadingImage: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val linkSuccess: Boolean = false
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
                    _uiState.value = _uiState.value.copy(isLoading = false, profile = profile.copy(uid = userId))
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, profile = UserProfile(uid = userId))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun uploadProfilePicture(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingImage = true, error = null)
            val userId = auth.currentUser?.uid ?: return@launch

            try {
                // FIXED: Create a unique ID using the timestamp to bypass Cloudinary overwrite blocks and Coil cache
                val uniqueImageId = "${userId}_${System.currentTimeMillis()}"

                val downloadUrl = suspendCancellableCoroutine { continuation ->
                    MediaManager.get().upload(uri)
                        .unsigned("krfgajle") // Replace with your actual preset
                        .option("folder", "profile_pictures")
                        .option("public_id", uniqueImageId)
                        .callback(object : UploadCallback {
                            override fun onStart(requestId: String?) {}

                            override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                            override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                                val url = resultData?.get("secure_url") as? String
                                if (url != null) {
                                    continuation.resume(url)
                                } else {
                                    continuation.resumeWithException(Exception("Failed to get URL from Cloudinary"))
                                }
                            }

                            override fun onError(requestId: String?, error: ErrorInfo?) {
                                val errorMessage = error?.description ?: "Unknown Cloudinary upload error"
                                continuation.resumeWithException(Exception(errorMessage))
                            }

                            override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                        })
                        .dispatch()
                }

                // Update Firestore with the new Cloudinary URL
                firestore.collection("users").document(userId)
                    .set(mapOf("profilePictureUrl" to downloadUrl), SetOptions.merge())
                    .await()

                val updatedProfile = _uiState.value.profile.copy(profilePictureUrl = downloadUrl)
                _uiState.value = _uiState.value.copy(
                    isUploadingImage = false,
                    profile = updatedProfile
                )

                // Sync the new URL to forum posts and comments
                syncProfileDataToForum(updatedProfile.name, downloadUrl, updatedProfile.role)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isUploadingImage = false, error = e.localizedMessage)
            }
        }
    }

    fun linkDoctor(doctorId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val cleanDoctorId = doctorId.trim()

            try {
                firestore.collection("users").document(userId)
                    .set(mapOf("assignedDoctorId" to cleanDoctorId), SetOptions.merge())
                    .await()

                val currentProfile = _uiState.value.profile
                _uiState.value = _uiState.value.copy(
                    linkSuccess = true,
                    profile = currentProfile.copy(assignedDoctorId = cleanDoctorId)
                )
            } catch(e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage)
            }
        }
    }

    fun saveProfile(name: String, dob: String, age: Int, gender: String, height: Float, weight: Float) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveSuccess = false, linkSuccess = false, error = null)

            val currentUser = auth.currentUser
            val userId = currentUser?.uid ?: return@launch

            try {
                val profileUpdates = userProfileChangeRequest {
                    displayName = name
                }
                currentUser.updateProfile(profileUpdates).await()

                val currentProfile = _uiState.value.profile
                val profileToSave = currentProfile.copy(
                    uid = userId,
                    name = name,
                    dob = dob,
                    age = age,
                    gender = gender,
                    heightCm = height,
                    weightKg = weight
                )

                firestore.collection("users").document(userId).set(profileToSave, SetOptions.merge()).await()

                syncProfileDataToForum(name, currentProfile.profilePictureUrl, currentProfile.role)

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

    private fun syncProfileDataToForum(newName: String, newPicUrl: String?, role: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val userPosts = firestore.collection("forum_posts").whereEqualTo("authorId", userId).get().await()
                if (!userPosts.isEmpty) {
                    firestore.runBatch { batch ->
                        userPosts.documents.forEach { doc ->
                            batch.update(
                                doc.reference,
                                "authorName", "u/$newName",
                                "authorProfileUrl", newPicUrl,
                                "authorRole", role
                            )
                        }
                    }.await()
                }

                val userComments = firestore.collectionGroup("comments").whereEqualTo("authorId", userId).get().await()
                if (!userComments.isEmpty) {
                    firestore.runBatch { batch ->
                        userComments.documents.forEach { doc ->
                            batch.update(
                                doc.reference,
                                "authorName", "u/$newName",
                                "authorProfileUrl", newPicUrl,
                                "authorRole", role
                            )
                        }
                    }.await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        auth.signOut()
        onLogoutComplete()
    }
}