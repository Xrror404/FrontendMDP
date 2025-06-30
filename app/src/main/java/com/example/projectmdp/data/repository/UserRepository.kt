package com.example.projectmdp.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.projectmdp.data.model.auth.LoginDto
import com.example.projectmdp.data.source.dataclass.User
import com.example.projectmdp.data.source.local.dao.UserDao
import com.example.projectmdp.data.source.remote.RetrofitInstance
import com.example.projectmdp.data.source.remote.UserApi
import com.example.projectmdp.data.source.response.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

// Extension functions untuk mapping response ke data class
fun LoginDto.toUser(): User {
    return User(
        id = this.id,
        email = this.email,
        username = this.username ?: "",
        address = this.address ?: "",
        phone_number = this.phone_number ?: "",
        role = this.role ?: "buyer",
        firebase_uid = this.firebase_uid,
        profile_picture = this.profile_picture ?: "",
        auth_provider = this.auth_provider,
        created_at = this.created_at,
        deleted_at = this.deleted_at
    )
}

fun UserSearchResult.toUser(): User {
    return User(
        id = this.id,
        email = this.email,
        username = this.username,
        address = "",
        phone_number = "",
        role = "buyer",
        firebase_uid = null,
        profile_picture = this.profile_picture,
        auth_provider = "local",
        created_at = this.created_at,
        deleted_at = null
    )
}

fun PublicUserProfile.toUser(): User {
    return User(
        id = this.id,
        email = this.email,
        username = this.username,
        address = this.address ?: "",
        phone_number = this.phone_number ?: "",
        role = this.role ?: "buyer",
        firebase_uid = null,
        profile_picture = this.profile_picture,
        auth_provider = this.auth_provider,
        created_at = this.created_at,
        deleted_at = null
    )
}

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
//    private val userApi: UserApi
) {

    // Current User Profile Management (Main User yang Login)
    
    suspend fun getCurrentUserProfile(forceRefresh: Boolean = false): Flow<Result<User>> = flow {
        try {
            // Emit cached current user first
            if (!forceRefresh) {
                userDao.getCurrentUser()?.let { cachedUser ->
                    emit(Result.success(User.fromUserEntity(cachedUser)))
                }
            }

            // Fetch from remote
            val response = RetrofitInstance.Userapi.getProfile()
            if (response.status == 200) {
                response.data?.let { loginDto ->
                    val user = loginDto.user.toUser()
                    
                    // Cache current user dengan role khusus
                    val userEntity = user.toUserEntity().copy(role = "buyer")
                    userDao.insertUser(userEntity)
                    
                    emit(Result.success(user))
                } ?: emit(Result.failure(Exception("No profile data received")))
            } else {
                // Return cached data if remote fails
                userDao.getCurrentUser()?.let { cachedUser ->
                    if (!forceRefresh) {
                        // Already emitted above
                    } else {
                        emit(Result.success(User.fromUserEntity(cachedUser)))
                    }
                } ?: emit(Result.failure(Exception(response.message)))
            }
        } catch (e: Exception) {
            // Fallback to cached data
            userDao.getCurrentUser()?.let { cachedUser ->
                emit(Result.success(User.fromUserEntity(cachedUser)))
            } ?: emit(Result.failure(e))
        }
    }

    suspend fun updateUserProfile(
        username: String? = null,
        address: String? = null,
        phoneNumber: String? = null
    ): Flow<Result<User>> = flow {
        try {
            val request = UpdateProfileRequest(
                username = username,
                address = address,
                phone_number = phoneNumber
            )
            
            val response = RetrofitInstance.Userapi.updateUserProfile(request)
            if (response.status == 200) {
                response.data?.let { updateData ->
                    val updatedUser = updateData.user.toUser()
                    
                    // Update cache
                    val userEntity = updatedUser.toUserEntity().copy(role = "current_user")
                    userDao.insertUser(userEntity)
                    
                    emit(Result.success(updatedUser))
                } ?: emit(Result.failure(Exception("Failed to update profile")))
            } else {
                emit(Result.failure(Exception(response.message)))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun updateProfilePicture(context: Context, imageUri: Uri): Flow<Result<String>> = flow {
        var tempFile: File? = null
        
        try {
            Log.d("UserRepository", "Starting profile picture update with URI: $imageUri")
            
            // Create temp file from URI (similar to ProductRepository)
            tempFile = createTempFileFromUri(context, imageUri)
            
            if (tempFile == null || !tempFile.exists()) {
                Log.e("UserRepository", "Failed to create temp file from URI")
                emit(Result.failure(Exception("Failed to prepare image file for upload. Please try again.")))
                return@flow
            }

            Log.d("UserRepository", "Temp file created successfully: ${tempFile.name}, size: ${tempFile.length()} bytes")

            val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            val fileName = getFileName(context, imageUri) ?: "profile_picture.jpg"
            val imagePart = MultipartBody.Part.createFormData("image", fileName, requestFile)

            Log.d("UserRepository", "Uploading profile picture to server...")
            val response = RetrofitInstance.Userapi.updateProfilePicture(imagePart)
            
            Log.d("UserRepository", "Server response status: ${response.status}")
            
            if (response.status == 200) {
                response.data?.let { profileData ->
                    val profilePictureUrl = profileData.profile_picture
                    Log.d("UserRepository", "Profile picture URL received: $profilePictureUrl")
                    
                    // Update cache
                    userDao.getCurrentUser()?.let { currentUser ->
                        userDao.updateProfilePicture(currentUser.id, profilePictureUrl)
                        Log.d("UserRepository", "Profile picture URL cached successfully")
                    }
                    
                    emit(Result.success(profilePictureUrl))
                } ?: run {
                    Log.e("UserRepository", "No profile data received from server")
                    emit(Result.failure(Exception("Failed to update profile picture")))
                }
            } else {
                Log.e("UserRepository", "Server error: ${response.message}")
                emit(Result.failure(Exception(response.message)))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Exception during profile picture update", e)
            emit(Result.failure(e))
        } finally {
            // Clean up temp file
            tempFile?.let { file ->
                try {
                    if (file.exists()) {
                        file.delete()
                        Log.d("UserRepository", "Temp file deleted successfully")
                    }
                } catch (e: Exception) {
                    Log.w("UserRepository", "Failed to delete temp file: ${e.message}")
                }
            }
        }
    }

    // User Search & Discovery (untuk chat, contact, dll)
    
    suspend fun searchUsers(query: String): Flow<Result<List<User>>> = flow {
        try {
            // Search in cache first untuk quick results
            val cachedResults = userDao.searchUsers(query).map { User.fromUserEntity(it) }
            if (cachedResults.isNotEmpty()) {
                emit(Result.success(cachedResults))
            }

            // Search in remote
            val response = RetrofitInstance.Userapi.searchUsers(query)
            if (response.status == 200) {
                response.data?.let { searchData ->
                    val users = searchData.users.map { it.toUser() }
                    
                    // Cache search results (tapi jangan sebagai current_user)
                    val userEntities = users.map { it.toUserEntity() }
                    userDao.insertUsers(userEntities)
                    
                    emit(Result.success(users))
                } ?: emit(Result.failure(Exception("No users found")))
            } else {
                // Return cached results if remote fails
                if (cachedResults.isEmpty()) {
                    emit(Result.failure(Exception(response.message)))
                }
            }
        } catch (e: Exception) {
            // Return cached results on error
            try {
                val cachedResults = userDao.searchUsers(query).map { User.fromUserEntity(it) }
                if (cachedResults.isNotEmpty()) {
                    emit(Result.success(cachedResults))
                } else {
                    emit(Result.failure(e))
                }
            } catch (cacheError: Exception) {
                emit(Result.failure(e))
            }
        }
    }

    suspend fun getUserById(userId: String): Flow<Result<User>> = flow {
        try {
            // Check cache first
            userDao.getUserById(userId)?.let { cachedUser ->
                emit(Result.success(User.fromUserEntity(cachedUser)))
            }

            // Fetch from remote
            val response = RetrofitInstance.Userapi.getUserById(userId)
            if (response.status == 200) {
                response.data?.let { userData ->
                    val user = userData.user.toUser()
                    
                    // Cache user data
                    userDao.insertUser(user.toUserEntity())
                    
                    emit(Result.success(user))
                } ?: emit(Result.failure(Exception("User not found")))
            } else {
                // If remote fails but we have cache, that's OK
                userDao.getUserById(userId)?.let { cachedUser ->
                    // Already emitted above
                } ?: emit(Result.failure(Exception(response.message)))
            }
        } catch (e: Exception) {
            // Try cache on error
            userDao.getUserById(userId)?.let { cachedUser ->
                emit(Result.success(User.fromUserEntity(cachedUser)))
            } ?: emit(Result.failure(e))
        }
    }

    // Local-only operations for offline support
    
    fun getCurrentUserFlow(): Flow<User?> {
        return userDao.getCurrentUserFlow().map { entity ->
            entity?.let { User.fromUserEntity(it) }
        }
    }

    suspend fun getRecentUsers(limit: Int = 10): List<User> {
        return userDao.getRecentUsers(limit).map { User.fromUserEntity(it) }
    }

    suspend fun getAllCachedUsers(): List<User> {
        return userDao.getAllCachedUsers().map { User.fromUserEntity(it) }
    }

    // Cache Management
    
    suspend fun clearUserCache() {
        userDao.clearCachedUsers() // Keep current user, clear others
    }

    suspend fun logout() {
        userDao.clearAllUsers() // Clear everything including current user
    }

    // Quick profile updates (optimistic updates)
    
    suspend fun updateUsernameLocally(username: String) {
        userDao.getCurrentUser()?.let { currentUser ->
            userDao.updateUsername(currentUser.id, username)
        }
    }

    suspend fun updateAddressLocally(address: String) {
        userDao.getCurrentUser()?.let { currentUser ->
            userDao.updateAddress(currentUser.id, address)
        }
    }

    suspend fun updatePhoneNumberLocally(phoneNumber: String) {
        userDao.getCurrentUser()?.let { currentUser ->
            userDao.updatePhoneNumber(currentUser.id, phoneNumber)
        }
    }

    // Helper methods
    
    suspend fun isUserCached(userId: String): Boolean {
        return userDao.getUserById(userId) != null
    }

    suspend fun getCurrentUserId(): String? {
        return userDao.getCurrentUser()?.id
    }

    suspend fun saveCurrentUser(user: User) {
        val userEntity = user.toUserEntity().copy(role = "current_user")
        userDao.insertUser(userEntity)
    }

    // Helper functions for file handling (similar to ProductRepository)
    private fun createTempFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = getFileName(context, uri)
            val tempFile = File(context.cacheDir, "upload_profile_${System.currentTimeMillis()}_${fileName ?: "image.jpg"}")
            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e("UserRepository", "Error creating temp file from URI: ${e.message}", e)
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    result = cursor.getString(nameIndex)
                }
            }
        }
        // Fallback to last path segment if content resolver fails or it's a file URI
        if (result == null) {
            result = uri.lastPathSegment
        }
        return result
    }
}