package com.healthoracle.data.model

import com.google.firebase.firestore.DocumentId

data class ForumPost(
    @DocumentId val id: String = "",
    val authorName: String = "",
    val authorId: String = "", // Tracks who made the post
    val authorRole: String = "patient", // NEW: Tracks if the author is a doctor for the verified tick
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String = "Just now",
    val title: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(),
    val upvotes: Int = 0,
    val commentCount: Int = 0,
    val viewCount: Int = 0,

    // Security lists to prevent duplicate views and unlimited votes
    val viewedBy: List<String> = emptyList(),
    val upvotedBy: List<String> = emptyList(),
    val downvotedBy: List<String> = emptyList()
)