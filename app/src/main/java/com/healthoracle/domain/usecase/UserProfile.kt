package com.healthoracle.domain.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val dob: String = "", // NEW: Save the Date of Birth
    val age: Int = 0,     // We still keep age to auto-fill the Diabetes slider easily
    val gender: String = "Male",
    val heightCm: Float = 0f,
    val weightKg: Float = 0f
)