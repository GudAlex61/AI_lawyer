package com.example.myapplication

data class UserProfile(
    val userId: String,
    val fullName: String,
    val birthDate: String,
    val passportNumber: String? = null,
    val avatarPath: String? = null
)