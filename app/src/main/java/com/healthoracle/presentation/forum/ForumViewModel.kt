package com.healthoracle.presentation.forum

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.healthoracle.data.model.ForumPost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject

@HiltViewModel
class ForumViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // IMPORTANT: Paste your Unsigned Upload Preset name here!
    private val cloudinaryUploadPreset = "krfgajle"

    private val _posts = MutableStateFlow<List<ForumPost>>(emptyList())
    val posts: StateFlow<List<ForumPost>> = _posts.asStateFlow()

    init {
        fetchRealtimePosts()
    }

    private fun fetchRealtimePosts() {
        firestore.collection("forum_posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val postList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ForumPost::class.java)
                    }
                    _posts.value = postList
                }
            }
    }

    fun incrementViewCount(postId: String) {
        viewModelScope.launch {
            val postRef = firestore.collection("forum_posts").document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val currentViews = snapshot.getLong("viewCount") ?: 0L
                transaction.update(postRef, "viewCount", currentViews + 1)
            }.addOnFailureListener { e ->
                e.printStackTrace()
            }
        }
    }

    fun upvotePost(postId: String, currentUpvotes: Int) {
        firestore.collection("forum_posts").document(postId)
            .update("upvotes", currentUpvotes + 1)
    }

    fun downvotePost(postId: String, currentUpvotes: Int) {
        firestore.collection("forum_posts").document(postId)
            .update("upvotes", currentUpvotes - 1)
    }

    fun createPost(title: String, content: String, imageUris: List<Uri>, onComplete: (Boolean, String) -> Unit) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            onComplete(false, "You must be logged in to post.")
            return
        }

        val authorName = if (currentUser.displayName.isNullOrBlank()) "u/Anonymous" else "u/${currentUser.displayName}"

        viewModelScope.launch {
            try {
                val uploadedUrls = mutableListOf<String>()

                // Upload each image to Cloudinary and wait for the secure URL
                for (uri in imageUris) {
                    val downloadUrl = uploadToCloudinary(uri)
                    uploadedUrls.add(downloadUrl)
                }

                // Save the text and the Cloudinary links to your Firestore database
                savePostToFirestore(title, content, authorName, uploadedUrls, onComplete)
            } catch (e: Exception) {
                onComplete(false, "Cloudinary upload failed: ${e.message}")
            }
        }
    }

    // Helper function to turn Cloudinary's callback into a modern Kotlin Coroutine
    private suspend fun uploadToCloudinary(uri: Uri): String = suspendCancellableCoroutine { continuation ->
        MediaManager.get().upload(uri)
            .unsigned(cloudinaryUploadPreset)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val url = resultData?.get("secure_url") as? String ?: ""
                    continuation.resume(url)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    continuation.resumeWithException(Exception(error?.description ?: "Unknown error"))
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }

    private fun savePostToFirestore(title: String, content: String, authorName: String, imageUrls: List<String>, onComplete: (Boolean, String) -> Unit) {
        val docRef = firestore.collection("forum_posts").document()

        val newPost = ForumPost(
            id = docRef.id,
            authorName = authorName,
            timestamp = System.currentTimeMillis(),
            timeAgo = "Just now",
            title = title,
            content = content,
            imageUrls = imageUrls,
            upvotes = 1,
            commentCount = 0,
            viewCount = 0,
            userVote = 1
        )

        docRef.set(newPost)
            .addOnSuccessListener { onComplete(true, "Post created successfully!") }
            .addOnFailureListener { e -> onComplete(false, "Failed to save post data: ${e.message}") }
    }
}