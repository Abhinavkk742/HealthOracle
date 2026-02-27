package com.healthoracle.data.model

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "Anonymous",
    val title: String = "",
    val description: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    // NEW: Track who interacts with the post
    val upvotedBy: List<String> = emptyList(),
    val savedBy: List<String> = emptyList()
)