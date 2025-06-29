package com.example.projectmdp.data.source.response

import com.example.projectmdp.data.model.auth.LoginDto

data class UserApiResponse<T>(
    val status: Int,
    val message: String,
    val data: T?
)

data class UserProfileResponse (
    val status : Int,
    val message: String,
    val data: VerifyData?
)

data class SearchUsersResponse(
    val status: Int,
    val message: String,
    val data: SearchUsersData
)

data class GetUserByIdResponse(
    val status: Int,
    val message: String,
    val data: GetUserByIdData
)

data class UpdateProfileResponse(
    val status: Int,
    val message: String,
    val data: UpdateProfileData
)

//=====================================================
//=====================================================
data class SearchUsersData(
    val users: List<UserSearchResult>,
    val query: String,
    val count: Int
)

data class UserSearchResult(
    val id: String,
    val username: String,
    val email: String,
    val profile_picture: String?,
    val created_at: String
)
//=====================================================
data class GetUserByIdData(
    val user: PublicUserProfile,
    val isOwnProfile: Boolean
)

data class PublicUserProfile(
    val id: String,
    val username: String,
    val email: String,
    val profile_picture: String? = null,
    val auth_provider: String = "password",
    val created_at: String,
    // Fields berikut hanya ada jika isOwnProfile = true
    val phone_number: String? = null,
    val address: String? = null,
    val role: String? = null
)
//====================================================
data class UpdateProfileData(
    val user: LoginDto  // Reuse UserProfile dari getUserProfile
)
// Request body untuk update profile
data class UpdateProfileRequest(
    val username: String? = null,
    val address: String? = null,
    val phone_number: String? = null
)
//====================================================
data class ProfilePictureData(
    val profile_picture: String
)