package com.healthoracle.data.model

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "Anonymous",
    val text: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)