package com.healthoracle.presentation.forum

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.healthoracle.data.model.Comment
import com.healthoracle.data.model.ForumPost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cloudinaryUploadPreset = "krfgajle" // <-- PASTE YOUR PRESET HERE

    private val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _post = MutableStateFlow<ForumPost?>(null)
    val post: StateFlow<ForumPost?> = _post.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCommenting = MutableStateFlow(false)
    val isCommenting: StateFlow<Boolean> = _isCommenting.asStateFlow()

    init {
        loadPostAndComments()
    }

    private fun loadPostAndComments() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val postDoc: DocumentSnapshot = firestore.collection("forum_posts")
                    .document(postId)
                    .get()
                    .await()

                val rawPost = postDoc.toObject(ForumPost::class.java)
                if (rawPost != null) {
                    _post.value = rawPost
                }

                val direction: Query.Direction = Query.Direction.ASCENDING
                val commentsSnapshot: QuerySnapshot = firestore.collection("forum_posts")
                    .document(postId)
                    .collection("comments")
                    .orderBy("timestamp", direction)
                    .get()
                    .await()

                val rawComments = commentsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)
                }
                _comments.value = rawComments

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePost(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("forum_posts").document(postId).delete().await()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun editPost(newTitle: String, newContent: String, retainedUrls: List<String>, newUris: List<Uri>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val finalUrls = retainedUrls.toMutableList()

                // Upload any newly selected images to Cloudinary
                for (uri in newUris) {
                    val downloadUrl = uploadToCloudinary(uri)
                    finalUrls.add(downloadUrl)
                }

                firestore.collection("forum_posts").document(postId)
                    .update(
                        "title", newTitle,
                        "content", newContent,
                        "imageUrls", finalUrls
                    ).await()

                loadPostAndComments()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
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

    // FIXED: Correctly pulls the name from the "users" collection now!
    fun addComment(content: String, replyToCommentId: String? = null, replyToAuthorName: String? = null) {
        val currentUser = auth.currentUser ?: return
        val authorId = currentUser.uid
        if (content.isBlank()) return

        viewModelScope.launch {
            _isCommenting.value = true
            try {
                // THE FIX: Looking up the fresh profile name from Firestore
                val userDoc = firestore.collection("users").document(authorId).get().await()
                val profileName = userDoc.getString("name")

                val authorName = if (!profileName.isNullOrBlank()) {
                    "u/$profileName"
                } else if (!currentUser.displayName.isNullOrBlank()) {
                    "u/${currentUser.displayName}"
                } else {
                    "u/Anonymous"
                }

                val newComment = Comment(
                    postId = postId,
                    authorId = authorId,
                    authorName = authorName,
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    replyToCommentId = replyToCommentId,
                    replyToAuthorName = replyToAuthorName
                )

                firestore.runBatch { batch ->
                    val postRef = firestore.collection("forum_posts").document(postId)
                    val commentRef = postRef.collection("comments").document()
                    batch.set(commentRef, newComment)
                    batch.update(postRef, "commentCount", com.google.firebase.firestore.FieldValue.increment(1))
                }.addOnSuccessListener {
                    loadPostAndComments()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isCommenting.value = false
            }
        }
    }
}