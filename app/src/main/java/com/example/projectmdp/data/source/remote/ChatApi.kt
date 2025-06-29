package com.example.projectmdp.data.source.remote

import com.example.projectmdp.data.source.response.ApiResponse
import com.example.projectmdp.data.source.response.DeleteMessageResponse
import com.example.projectmdp.data.source.response.GetConversationResponse
import com.example.projectmdp.data.source.response.GetUserConversationsResponse
import com.example.projectmdp.data.source.response.StartChatRequest
import com.example.projectmdp.data.source.response.StartChatResponse
import com.example.projectmdp.data.source.response.UpdateMessageStatusRequest
import com.example.projectmdp.data.source.response.UpdateMessageStatusResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    @POST("/chat")
    suspend fun startChat(
        @Body request: StartChatRequest
    ): ApiResponse<StartChatResponse>

    @GET("/chat/conversations")
    suspend fun getUserConversations(
    ): ApiResponse<GetUserConversationsResponse>

    @GET("/chat/conversation/{user_id}")
    suspend fun getConversation(
        @Path("user_id") userId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): ApiResponse<GetConversationResponse>

    @PUT("/chat/{chat_id}/status")
    suspend fun updateMessageStatus(
        @Path("chat_id") chatId: String,
        @Body request: UpdateMessageStatusRequest
    ): ApiResponse<UpdateMessageStatusResponse>

    @DELETE("/chat/{chat_id}")
    suspend fun deleteMessage(
        @Path("chat_id") chatId: String
    ): ApiResponse<DeleteMessageResponse>
}