package com.healthoracle.presentation.profile // Adjust package if needed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject

@HiltViewModel
class MyPostsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _myPosts = MutableStateFlow<List<ForumPost>>(emptyList())
    val myPosts: StateFlow<List<ForumPost>> = _myPosts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchMyPosts()
    }

    private fun fetchMyPosts() {
        val currentUserId = Firebase.auth.currentUser?.uid
        if (currentUserId == null) {
            _isLoading.value = false
            return
        }

        // Only listen for posts where this specific user is the author
        firestore.collection("forum_posts")
            .whereEqualTo("authorId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    _myPosts.value = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ForumPost::class.java)
                    }
                }
            }
    }
}