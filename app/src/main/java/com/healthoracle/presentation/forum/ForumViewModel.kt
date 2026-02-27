package com.healthoracle.presentation.forum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.healthoracle.data.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ForumViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Fetch the raw posts
                val snapshot = firestore.collection("posts")
                    .orderBy("timestampMillis", Query.Direction.DESCENDING)
                    .get().await()

                val rawPosts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                }

                // 2. Gather all unique author IDs from these posts
                val uniqueAuthorIds = rawPosts.map { it.authorId }.toSet()
                val latestAuthorNames = mutableMapOf<String, String>()

                // 3. Fetch the absolute latest profile name for each author
                for (id in uniqueAuthorIds) {
                    try {
                        val userDoc = firestore.collection("users").document(id).get().await()
                        latestAuthorNames[id] = userDoc.getString("name") ?: "Anonymous"
                    } catch (e: Exception) {
                        // Skip if network fails, we'll just use the old name
                    }
                }

                // 4. Update the posts with the fresh names before displaying!
                val updatedPosts = rawPosts.map { post ->
                    post.copy(authorName = latestAuthorNames[post.authorId] ?: post.authorName)
                }

                _posts.value = updatedPosts
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPost(title: String, description: String, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isCreating.value = true
            try {
                var authorName = "Anonymous"
                val userDoc = firestore.collection("users").document(userId).get().await()
                if (userDoc.exists()) {
                    authorName = userDoc.getString("name") ?: "Anonymous"
                }

                val newPost = Post(
                    authorId = userId,
                    authorName = authorName,
                    title = title,
                    description = description
                )

                firestore.collection("posts").add(newPost).await()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isCreating.value = false
            }
        }
    }
}