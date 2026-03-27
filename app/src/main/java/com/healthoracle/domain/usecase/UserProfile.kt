package com.healthoracle.domain.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val dob: String = "",
    val age: Int = 0,
    val gender: String = "Male",
    val heightCm: Float = 0f,
    val weightKg: Float = 0f,
    val role: String = "patient", // "doctor" gets the verified tick
    val assignedDoctorId: String? = null,
    val profilePictureUrl: String? = null // NEW: Profile picture URL
)