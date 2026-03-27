package com.healthoracle.data.model

import com.google.firebase.firestore.DocumentId

data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorRole: String = "patient", // NEW: For doctor tick
    val authorProfileUrl: String? = null, // NEW: For profile pic
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val replyToCommentId: String? = null,
    val replyToAuthorName: String? = null
)