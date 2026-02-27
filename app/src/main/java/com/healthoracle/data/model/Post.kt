package com.healthoracle.data.model

data class Post(
    val id: String = "", // Firestore document ID
    val authorId: String = "",
    val authorName: String = "Anonymous",
    val title: String = "",
    val description: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)