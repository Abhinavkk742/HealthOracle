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

    // Expose the current user ID to the UI so we know which icons to fill in
    val currentUserId: String?
        get() = auth.currentUser?.uid

    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = firestore.collection("posts")
                    .orderBy("timestampMillis", Query.Direction.DESCENDING)
                    .get().await()

                val rawPosts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                }

                val uniqueAuthorIds = rawPosts.map { it.authorId }.toSet()
                val latestAuthorNames = mutableMapOf<String, String>()

                for (id in uniqueAuthorIds) {
                    try {
                        val userDoc = firestore.collection("users").document(id).get().await()
                        latestAuthorNames[id] = userDoc.getString("name") ?: "Anonymous"
                    } catch (e: Exception) {}
                }

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

    // --- NEW: Toggle Upvote Logic ---
    fun toggleUpvote(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val postRef = firestore.collection("posts").document(postId)
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val post = snapshot.toObject(Post::class.java) ?: return@runTransaction

                    val currentUpvotes = post.upvotedBy.toMutableList()
                    if (currentUpvotes.contains(userId)) {
                        currentUpvotes.remove(userId) // Remove like
                    } else {
                        currentUpvotes.add(userId) // Add like
                    }

                    transaction.update(postRef, "upvotedBy", currentUpvotes)
                }.await()

                // Instantly update UI without reloading the whole feed
                _posts.value = _posts.value.map {
                    if (it.id == postId) {
                        val newUpvotes = it.upvotedBy.toMutableList()
                        if (newUpvotes.contains(userId)) newUpvotes.remove(userId) else newUpvotes.add(userId)
                        it.copy(upvotedBy = newUpvotes)
                    } else it
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- NEW: Toggle Bookmark Logic ---
    fun toggleBookmark(postId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val postRef = firestore.collection("posts").document(postId)
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val post = snapshot.toObject(Post::class.java) ?: return@runTransaction

                    val currentSaved = post.savedBy.toMutableList()
                    if (currentSaved.contains(userId)) {
                        currentSaved.remove(userId)
                    } else {
                        currentSaved.add(userId)
                    }

                    transaction.update(postRef, "savedBy", currentSaved)
                }.await()

                _posts.value = _posts.value.map {
                    if (it.id == postId) {
                        val newSaved = it.savedBy.toMutableList()
                        if (newSaved.contains(userId)) newSaved.remove(userId) else newSaved.add(userId)
                        it.copy(savedBy = newSaved)
                    } else it
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}