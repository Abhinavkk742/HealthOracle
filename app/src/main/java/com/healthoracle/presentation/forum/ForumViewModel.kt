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
                val snapshot = firestore.collection("posts")
                    .orderBy("timestampMillis", Query.Direction.DESCENDING)
                    .get().await()

                val postList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                }
                _posts.value = postList
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
                // Fetch the user's real name from their profile!
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

                // Save to the global 'posts' collection
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