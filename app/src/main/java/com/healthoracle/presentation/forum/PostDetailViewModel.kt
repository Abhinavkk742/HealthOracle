package com.healthoracle.presentation.forum

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.healthoracle.data.model.Comment
import com.healthoracle.data.model.ForumPost // Updated to match our main model
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

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
                // --- 1. Fetch and update the main Post (using "forum_posts") ---
                val postDoc: DocumentSnapshot = firestore.collection("forum_posts")
                    .document(postId)
                    .get()
                    .await()

                val rawPost = postDoc.toObject(ForumPost::class.java)

                if (rawPost != null) {
                    _post.value = rawPost
                }

                // --- 2. Fetch and update the Comments ---
                // Added explicit type for direction and snapshot to fix the inference error
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

    fun addComment(content: String) { // Changed 'text' to 'content' to match Comment model
        val userId = auth.currentUser?.uid ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            _isCommenting.value = true
            try {
                val currentUser = auth.currentUser
                val authorName = if (currentUser?.displayName.isNullOrBlank()) "u/Anonymous" else "u/${currentUser?.displayName}"

                val newComment = Comment(
                    postId = postId,
                    authorName = authorName,
                    content = content, // Ensure this matches your Comment.kt field name
                    timestamp = System.currentTimeMillis()
                )

                // Add comment to the subcollection
                firestore.collection("forum_posts")
                    .document(postId)
                    .collection("comments")
                    .add(newComment)
                    .await()

                // Increment comment count on the main post
                firestore.collection("forum_posts")
                    .document(postId)
                    .update("commentCount", com.google.firebase.firestore.FieldValue.increment(1))
                    .await()

                loadPostAndComments() // Refresh list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isCommenting.value = false
            }
        }
    }
}