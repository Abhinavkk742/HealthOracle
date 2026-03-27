package com.healthoracle.data.model

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "Anonymous",
    val authorRole: String = "patient", // NEW: For doctor tick
    val authorProfileUrl: String? = null, // NEW: For profile pic
    val title: String = "",
    val description: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    val upvotedBy: List<String> = emptyList(),
    val savedBy: List<String> = emptyList()
)