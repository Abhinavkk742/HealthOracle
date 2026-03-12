package com.healthoracle.presentation.forum

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.healthoracle.data.model.Comment
import com.healthoracle.data.model.ForumPost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject

@HiltViewModel
class ForumViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val cloudinaryUploadPreset = "krfgajle" // Keep your preset here!

    // NEW: Manage sorting state
    private val _sortBy = MutableStateFlow("New")
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    // Store the raw list from Firebase
    private val _rawPosts = MutableStateFlow<List<ForumPost>>(emptyList())

    // Combine the raw posts with the sort method to instantly arrange the feed
    val posts: StateFlow<List<ForumPost>> = combine(_rawPosts, _sortBy) { postList, sortMethod ->
        when (sortMethod) {
            "New" -> postList.sortedByDescending { it.timestamp }
            "Top" -> postList.sortedByDescending { it.upvotes }
            "Hot" -> postList.sortedByDescending {
                // A simple Reddit-style hotness formula: prioritizes engagement!
                (it.upvotes * 2) + (it.commentCount * 3) + it.viewCount
            }
            else -> postList
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    init {
        fetchRealtimePosts()
    }

    // Update the sort method
    fun setSortMethod(method: String) {
        _sortBy.value = method
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
                    _rawPosts.value = postList // Feed the raw posts
                }
            }
    }

    fun incrementViewCount(postId: String) {
        val userId = Firebase.auth.currentUser?.uid ?: return

        viewModelScope.launch {
            val postRef = firestore.collection("forum_posts").document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val viewedBy = snapshot.get("viewedBy") as? List<String> ?: emptyList()

                if (!viewedBy.contains(userId)) {
                    transaction.update(postRef, "viewedBy", FieldValue.arrayUnion(userId))
                    val currentViews = snapshot.getLong("viewCount") ?: 0L
                    transaction.update(postRef, "viewCount", currentViews + 1)
                }
            }.addOnFailureListener { it.printStackTrace() }
        }
    }

    fun toggleUpvote(postId: String) {
        val userId = Firebase.auth.currentUser?.uid ?: return

        viewModelScope.launch {
            val postRef = firestore.collection("forum_posts").document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val upvotedBy = snapshot.get("upvotedBy") as? List<String> ?: emptyList()
                val downvotedBy = snapshot.get("downvotedBy") as? List<String> ?: emptyList()
                val currentUpvotes = snapshot.getLong("upvotes") ?: 0L

                if (upvotedBy.contains(userId)) {
                    transaction.update(postRef, "upvotedBy", FieldValue.arrayRemove(userId))
                    transaction.update(postRef, "upvotes", currentUpvotes - 1)
                } else {
                    transaction.update(postRef, "upvotedBy", FieldValue.arrayUnion(userId))
                    var voteChange = 1L
                    if (downvotedBy.contains(userId)) {
                        transaction.update(postRef, "downvotedBy", FieldValue.arrayRemove(userId))
                        voteChange = 2L
                    }
                    transaction.update(postRef, "upvotes", currentUpvotes + voteChange)
                }
            }.addOnFailureListener { it.printStackTrace() }
        }
    }

    fun toggleDownvote(postId: String) {
        val userId = Firebase.auth.currentUser?.uid ?: return

        viewModelScope.launch {
            val postRef = firestore.collection("forum_posts").document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val upvotedBy = snapshot.get("upvotedBy") as? List<String> ?: emptyList()
                val downvotedBy = snapshot.get("downvotedBy") as? List<String> ?: emptyList()
                val currentUpvotes = snapshot.getLong("upvotes") ?: 0L

                if (downvotedBy.contains(userId)) {
                    transaction.update(postRef, "downvotedBy", FieldValue.arrayRemove(userId))
                    transaction.update(postRef, "upvotes", currentUpvotes + 1)
                } else {
                    transaction.update(postRef, "downvotedBy", FieldValue.arrayUnion(userId))
                    var voteChange = -1L
                    if (upvotedBy.contains(userId)) {
                        transaction.update(postRef, "upvotedBy", FieldValue.arrayRemove(userId))
                        voteChange = -2L
                    }
                    transaction.update(postRef, "upvotes", currentUpvotes + voteChange)
                }
            }.addOnFailureListener { it.printStackTrace() }
        }
    }

    fun createPost(title: String, content: String, imageUris: List<Uri>, onComplete: (Boolean, String) -> Unit) {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            onComplete(false, "You must be logged in to post.")
            return
        }

        val authorId = currentUser.uid

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(authorId).get().await()
                val profileName = userDoc.getString("name")

                val authorName = if (!profileName.isNullOrBlank()) {
                    "u/$profileName"
                } else if (!currentUser.displayName.isNullOrBlank()) {
                    "u/${currentUser.displayName}"
                } else {
                    "u/Anonymous"
                }

                val uploadedUrls = mutableListOf<String>()
                for (uri in imageUris) {
                    val downloadUrl = uploadToCloudinary(uri)
                    uploadedUrls.add(downloadUrl)
                }
                savePostToFirestore(title, content, authorName, authorId, uploadedUrls, onComplete)
            } catch (e: Exception) {
                onComplete(false, "Upload failed: ${e.message}")
            }
        }
    }

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

    private fun savePostToFirestore(title: String, content: String, authorName: String, authorId: String, imageUrls: List<String>, onComplete: (Boolean, String) -> Unit) {
        val docRef = firestore.collection("forum_posts").document()
        val newPost = ForumPost(
            id = docRef.id,
            authorName = authorName,
            authorId = authorId,
            timestamp = System.currentTimeMillis(),
            timeAgo = "Just now",
            title = title,
            content = content,
            imageUrls = imageUrls,
            upvotes = 1,
            commentCount = 0,
            viewCount = 0,
            upvotedBy = listOf(authorId)
        )

        docRef.set(newPost)
            .addOnSuccessListener { onComplete(true, "Post created successfully!") }
            .addOnFailureListener { e -> onComplete(false, "Failed to create post: ${e.message}") }
    }

    fun fetchCommentsForPost(postId: String) {
        firestore.collection("forum_posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _comments.value = snapshot.documents.mapNotNull { it.toObject(Comment::class.java) }
                }
            }
    }

    fun addComment(postId: String, content: String, onComplete: () -> Unit) {
        val currentUser = Firebase.auth.currentUser ?: return
        val authorId = currentUser.uid

        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(authorId).get().await()
                val profileName = userDoc.getString("name")

                val authorName = if (!profileName.isNullOrBlank()) {
                    "u/$profileName"
                } else if (!currentUser.displayName.isNullOrBlank()) {
                    "u/${currentUser.displayName}"
                } else {
                    "u/Anonymous"
                }

                val postRef = firestore.collection("forum_posts").document(postId)
                val commentRef = postRef.collection("comments").document()

                val comment = Comment(
                    id = commentRef.id,
                    postId = postId,
                    authorName = authorName,
                    content = content,
                    timestamp = System.currentTimeMillis()
                )

                firestore.runBatch { batch ->
                    batch.set(commentRef, comment)
                    batch.update(postRef, "commentCount", FieldValue.increment(1))
                }.addOnSuccessListener {
                    onComplete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}