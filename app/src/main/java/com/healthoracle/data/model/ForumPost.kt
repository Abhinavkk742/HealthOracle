package com.healthoracle.data.model

import com.google.firebase.firestore.DocumentId

data class ForumPost(
    @DocumentId val id: String = "",
    val authorName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String = "Just now",
    val title: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(), // NEW: Stores multiple image links
    val upvotes: Int = 0,
    val commentCount: Int = 0,
    val viewCount: Int = 0,
    val userVote: Int = 0
)