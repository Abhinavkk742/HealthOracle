package com.healthoracle.data.model

import com.google.firebase.firestore.DocumentId

data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    // NEW: Fields to handle threaded replies
    val replyToCommentId: String? = null,
    val replyToAuthorName: String? = null
)