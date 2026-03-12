package com.healthoracle.presentation.forum

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.healthoracle.data.model.ForumPost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ForumViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

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

    // NEW: Function to handle image uploads and creating the actual post
    fun createPost(title: String, content: String, imageUri: Uri?, onComplete: (Boolean, String) -> Unit) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            onComplete(false, "You must be logged in to post.")
            return
        }

        // Match the Reddit naming convention
        val authorName = if (currentUser.displayName.isNullOrBlank()) "u/Anonymous" else "u/${currentUser.displayName}"

        if (imageUri != null) {
            // 1. Upload the image to Firebase Storage first
            val storageRef = Firebase.storage.reference.child("forum_images/${UUID.randomUUID()}.jpg")
            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // 2. Save the post with the generated Image URL
                        savePostToFirestore(title, content, authorName, downloadUrl.toString(), onComplete)
                    }
                }
                .addOnFailureListener { e ->
                    onComplete(false, "Image upload failed: ${e.message}")
                }
        } else {
            // Save post without an image
            savePostToFirestore(title, content, authorName, null, onComplete)
        }
    }

    private fun savePostToFirestore(title: String, content: String, authorName: String, imageUrl: String?, onComplete: (Boolean, String) -> Unit) {
        val docRef = firestore.collection("forum_posts").document()

        val newPost = ForumPost(
            id = docRef.id,
            authorName = authorName,
            timestamp = System.currentTimeMillis(),
            timeAgo = "Just now",
            title = title,
            content = content,
            imageUrl = imageUrl,
            upvotes = 1, // Start with 1 default upvote from the creator
            commentCount = 0,
            viewCount = 0,
            userVote = 1
        )

        docRef.set(newPost)
            .addOnSuccessListener { onComplete(true, "Post created successfully!") }
            .addOnFailureListener { e -> onComplete(false, "Failed to create post: ${e.message}") }
    }
}