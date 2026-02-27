package com.healthoracle.presentation.forum

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.healthoracle.data.model.Comment
import com.healthoracle.data.model.Post
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

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

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
                // --- 1. Fetch and update the main Post ---
                val postDoc = firestore.collection("posts").document(postId).get().await()
                val rawPost = postDoc.toObject(Post::class.java)?.copy(id = postDoc.id)

                if (rawPost != null) {
                    try {
                        val userDoc = firestore.collection("users").document(rawPost.authorId).get().await()
                        val latestName = userDoc.getString("name") ?: rawPost.authorName
                        _post.value = rawPost.copy(authorName = latestName)
                    } catch (e: Exception) {
                        _post.value = rawPost
                    }
                }

                // --- 2. Fetch and update the Comments ---
                val commentsSnapshot = firestore.collection("posts").document(postId)
                    .collection("comments")
                    .orderBy("timestampMillis", Query.Direction.ASCENDING)
                    .get().await()

                val rawComments = commentsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)?.copy(id = doc.id)
                }

                val uniqueAuthorIds = rawComments.map { it.authorId }.toSet()
                val latestAuthorNames = mutableMapOf<String, String>()

                for (id in uniqueAuthorIds) {
                    try {
                        val userDoc = firestore.collection("users").document(id).get().await()
                        latestAuthorNames[id] = userDoc.getString("name") ?: "Anonymous"
                    } catch (e: Exception) {}
                }

                val updatedComments = rawComments.map { comment ->
                    comment.copy(authorName = latestAuthorNames[comment.authorId] ?: comment.authorName)
                }

                _comments.value = updatedComments
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addComment(text: String) {
        val userId = auth.currentUser?.uid ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            _isCommenting.value = true
            try {
                var authorName = "Anonymous"
                val userDoc = firestore.collection("users").document(userId).get().await()
                if (userDoc.exists()) {
                    authorName = userDoc.getString("name") ?: "Anonymous"
                }

                val newComment = Comment(
                    postId = postId,
                    authorId = userId,
                    authorName = authorName,
                    text = text
                )

                firestore.collection("posts").document(postId)
                    .collection("comments").add(newComment).await()

                loadPostAndComments()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isCommenting.value = false
            }
        }
    }
}