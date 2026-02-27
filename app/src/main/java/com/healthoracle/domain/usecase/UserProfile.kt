package com.healthoracle.domain.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val age: Int = 0,
    val gender: String = "Not Specified",
    val heightCm: Float = 0f,
    val weightKg: Float = 0f
)