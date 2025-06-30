package com.example.projectmdp.data.source.remote

import com.example.projectmdp.data.source.response.ApiResponse
import com.example.projectmdp.data.source.response.DeleteMessageData
import com.example.projectmdp.data.source.response.DeleteMessageResponse
import com.example.projectmdp.data.source.response.GetConversationData
import com.example.projectmdp.data.source.response.GetConversationResponse
import com.example.projectmdp.data.source.response.GetUserConversationsData // This will be used directly
import com.example.projectmdp.data.source.response.StartChatData
import com.example.projectmdp.data.source.response.StartChatRequest
import com.example.projectmdp.data.source.response.StartChatResponse
import com.example.projectmdp.data.source.response.UpdateMessageStatusData
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
    ): ApiResponse<StartChatData>

    @GET("/chat/conversations")
    suspend fun getUserConversations(
    ): ApiResponse<GetUserConversationsData>

    @GET("/chat/conversation/{user_id}")
    suspend fun getConversation(
        @Path("user_id") userId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): ApiResponse<GetConversationData>

    @PUT("/chat/{chat_id}/status")
    suspend fun updateMessageStatus(
        @Path("chat_id") chatId: String,
        @Body request: UpdateMessageStatusRequest
    ): ApiResponse<UpdateMessageStatusData>

    @DELETE("/chat/{chat_id}")
    suspend fun deleteMessage(
        @Path("chat_id") chatId: String
    ): ApiResponse<DeleteMessageData>
}