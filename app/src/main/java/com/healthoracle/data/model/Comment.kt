package com.healthoracle.data.model

import com.google.firebase.firestore.DocumentId

data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "", // NEW: Ties the comment to the user's specific account
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val replyToCommentId: String? = null,
    val replyToAuthorName: String? = null
)