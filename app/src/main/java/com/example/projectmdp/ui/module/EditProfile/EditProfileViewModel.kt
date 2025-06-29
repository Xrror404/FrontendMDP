package com.example.projectmdp.ui.module.EditProfile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.repository.UserRepository
import com.example.projectmdp.data.source.dataclass.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val user: User? = null,
    val username: String = "",
    val address: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String? = null,
    val isLoading: Boolean = false,
    val isUpdatingProfile: Boolean = false,
    val isUpdatingPicture: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            userRepository.getCurrentUserProfile(forceRefresh = true).collectLatest { result ->
                result.onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        username = user.username!!,
                        address = user.address,
                        phoneNumber = user.phone_number,
                        profilePictureUrl = user.profile_picture,
                        errorMessage = null
                    )
                    Log.d("EditProfileViewModel", "Profile loaded: ${user.username}")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load profile"
                    )
                    Log.e("EditProfileViewModel", "Error loading profile", error)
                }
            }
        }
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun updateAddress(address: String) {
        _uiState.value = _uiState.value.copy(address = address)
    }

    fun updatePhoneNumber(phoneNumber: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = phoneNumber)
    }

    fun updateProfilePicture(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingPicture = true, errorMessage = null)
            
            userRepository.updateProfilePicture(imageUri).collectLatest { result ->
                result.onSuccess { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        isUpdatingPicture = false,
                        profilePictureUrl = updatedUser,
                        saveSuccess = true,
                        errorMessage = null
                    )
                    Log.d("EditProfileViewModel", "Profile picture updated successfully")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isUpdatingPicture = false,
                        saveSuccess = false,
                        errorMessage = error.localizedMessage ?: "Failed to update profile picture"
                    )
                    Log.e("EditProfileViewModel", "Error updating profile picture", error)
                }
            }
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingProfile = true, errorMessage = null)
            
            userRepository.updateUserProfile(
                username = _uiState.value.username.takeIf { it.isNotBlank() },
                address = _uiState.value.address.takeIf { it.isNotBlank() },
                phoneNumber = _uiState.value.phoneNumber.takeIf { it.isNotBlank() }
            ).collectLatest { result ->
                result.onSuccess { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        isUpdatingProfile = false,
                        user = updatedUser,
                        saveSuccess = true,
                        errorMessage = null
                    )
                    Log.d("EditProfileViewModel", "Profile updated successfully")
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isUpdatingProfile = false,
                        saveSuccess = false,
                        errorMessage = error.localizedMessage ?: "Failed to update profile"
                    )
                    Log.e("EditProfileViewModel", "Error updating profile", error)
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    fun refreshProfile() {
        loadUserProfile()
    }
}
