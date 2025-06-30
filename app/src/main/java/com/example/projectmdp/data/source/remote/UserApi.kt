package com.example.projectmdp.data.source.remote

import com.example.projectmdp.data.source.response.GetUserByIdData
import com.example.projectmdp.data.source.response.ProfilePictureData
import com.example.projectmdp.data.source.response.PublicProfileData
import com.example.projectmdp.data.source.response.PublicUserProfile
import com.example.projectmdp.data.source.response.SearchUsersData
import com.example.projectmdp.data.source.response.UpdateProfileData
import com.example.projectmdp.data.source.response.UpdateProfileRequest
import com.example.projectmdp.data.source.response.UserApiResponse
import com.example.projectmdp.data.source.response.UserProfileResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApi {
    @GET("/me-profile")
    suspend fun getProfile(): UserProfileResponse

    @GET("/search-users")
    suspend fun searchUsers(
        @Query("query") query: String
    ): UserApiResponse<SearchUsersData>

    @GET("/user/{userId}")
    suspend fun getUserById(
        @Path("userId") userId: String
    ): UserApiResponse<GetUserByIdData>

    @PUT("/me-profile")
    suspend fun updateUserProfile(
        @Body request: UpdateProfileRequest
    ): UserApiResponse<UpdateProfileData>

    @Multipart
    @POST("/update-profile-picture")
    suspend fun updateProfilePicture(
        @Part image: MultipartBody.Part
    ): UserApiResponse<ProfilePictureData>

    @GET("/user/firebase/{firebase_uid}")
    suspend fun getUserByFirebaseUid(
        @Path("firebase_uid") firebaseUid: String
    ): UserApiResponse<PublicProfileData>
}