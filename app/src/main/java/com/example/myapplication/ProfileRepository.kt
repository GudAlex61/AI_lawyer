package com.example.myapplication

import kotlinx.coroutines.delay

interface ProfileRepository {
    suspend fun getProfile(userId: String): UserProfile?
    suspend fun updatePassport(userId: String, passportNumber: String)
    suspend fun updateAvatar(userId: String, avatarPath: String)
}

class FakeProfileRepository(
    defaultFullName: String,
    defaultBirthDate: String
) : ProfileRepository {

    private var profile = UserProfile(
        userId = "demo_user",
        fullName = defaultFullName,
        birthDate = defaultBirthDate,
        passportNumber = null,
        avatarPath = null
    )

    override suspend fun getProfile(userId: String): UserProfile? {
        return if (profile.userId == userId) profile else null
    }

    override suspend fun updatePassport(userId: String, passportNumber: String) {
        if (profile.userId == userId) {
            profile = profile.copy(passportNumber = passportNumber)
        }
    }

    override suspend fun updateAvatar(userId: String, avatarPath: String) {
        if (profile.userId == userId) {
            profile = profile.copy(avatarPath = avatarPath)
        }
    }
}