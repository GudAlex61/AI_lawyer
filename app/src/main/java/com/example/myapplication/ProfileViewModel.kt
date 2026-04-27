package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val CURRENT_USER_ID = "demo_user"
private const val PHOTO_FILE_NAME = "profile_photo.jpg"

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Content(val profile: UserProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val repository: ProfileRepository,
    private val errorProfileNotFound: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val profile = repository.getProfile(CURRENT_USER_ID)
            if (profile != null) {
                _uiState.value = ProfileUiState.Content(profile)
            } else {
                _uiState.value = ProfileUiState.Error(errorProfileNotFound)
            }
        }
    }

    fun generateAndSavePassport() {
        viewModelScope.launch {
            val newPassport = "8090${(100000..999999).random()}"
            repository.updatePassport(CURRENT_USER_ID, newPassport)
            loadProfile()
        }
    }

    fun saveAvatarFromBitmap(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            val path = withContext(IO) {
                savePhotoToStorage(context, bitmap)
            }
            if (path != null) {
                repository.updateAvatar(CURRENT_USER_ID, path)
            }
        }
    }

    fun loadSavedAvatar(context: Context): Bitmap? {
        val file = File(context.filesDir, PHOTO_FILE_NAME)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    private fun savePhotoToStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val file = File(context.filesDir, PHOTO_FILE_NAME)
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}