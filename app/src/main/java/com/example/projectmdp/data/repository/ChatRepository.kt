package com.example.projectmdp.data.repository

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.source.dataclass.ChatMessage
import com.example.projectmdp.data.source.dataclass.ChatPagination
import com.example.projectmdp.data.source.dataclass.Conversation
import com.example.projectmdp.data.source.dataclass.User
import com.example.projectmdp.data.source.local.SessionManager
import com.example.projectmdp.data.source.remote.ChatApi
import com.example.projectmdp.data.source.remote.RetrofitInstance
import com.example.projectmdp.data.source.remote.VerifyTokenRequest
import com.example.projectmdp.data.source.response.*
import com.example.projectmdp.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// Data class to hold chat messages with pagination
data class ChatMessages(
    val messages: List<ChatMessage>,
    val pagination: ChatPagination,
    val otherUser: User? = null
)

// Extension functions to convert response objects to data classes
fun com.example.projectmdp.data.source.response.ChatMessage.toChatMessage(): ChatMessage {
    return ChatMessage(
        id = this.id,
        user_sender = this.user_sender,
        user_receiver = this.user_receiver,
        chat = this.chat,
        datetime = this.datetime,
        status = this.status,
        created_at = this.created_at,
        updated_at = this.updated_at,
        deleted_at = this.deleted_at
    )
}

fun com.example.projectmdp.data.source.response.ChatPagination.toChatPagination(): ChatPagination {
    return ChatPagination(
        currentPage = this.currentPage,
        totalPages = this.totalPages,
        totalMessages = this.totalMessages,
        hasNext = this.hasNext,
        hasPrev = this.hasPrev,
        limit = this.limit
    )
}

// This maps from the response.Conversation to your dataclass.Conversation
fun com.example.projectmdp.data.source.response.Conversation.toConversation(): Conversation {
    return Conversation(
        otherUserId = this.otherUserId,
        otherUser = this.otherUser?.let {
            User(
                id = it.id,
                email = it.email,
                username = it.name, // Maps 'name' from response to 'username' in your UI model
                address = "",
                phone_number = "",
                role = "buyer",
                firebase_uid = null,
                profile_picture = it.profile_picture,
                auth_provider = "local",
                created_at = "",
                deleted_at = null
            )
        },
        lastMessage = this.lastMessage,
        lastMessageTime = this.lastMessageTime,
        lastMessageStatus = this.lastMessageStatus,
        lastMessageSender = this.lastMessageSender
    )
}

fun com.example.projectmdp.data.source.response.OtherUser.toChatUser(): User {
    return User(
        id = this.id,
        email = this.email,
        username = this.name,
        address = "",
        phone_number = "",
        role = "buyer",
        firebase_uid = null,
        profile_picture = this.profile_picture,
        auth_provider = "local",
        created_at = "",
        deleted_at = null
    )
}

@Singleton
class ChatRepository @Inject constructor(
//    private val chatApi: ChatApi // Commented out
    private val sessionManager: SessionManager
) {

    suspend fun getUserConversations(): Flow<Result<List<Conversation>>> = flow { // Returns dataclass.Conversation
        try {
            val response = RetrofitInstance.Chatapi.getUserConversations()
            Log.d("ChatRepository", "API Response: $response")

            if (response.isSuccess()) {
                // response.data here refers to the GetUserConversationsData object (from ChatApi.kt)
                response.data?.let { getUserConversationsData -> // This is now GetUserConversationsData
                    Log.d("ChatRepository", "Parsed GetUserConversationsData: $getUserConversationsData")

                    // CORRECTED LINE: Access 'conversations' directly from getUserConversationsData
                    // The inner 'data' field has been removed from GetUserConversationsData.
                    getUserConversationsData.conversations.map { it.toConversation() }.let { conversationsList ->
                        Log.d("ChatRepository", "Found conversations list: $conversationsList")
                        emit(Result.success(conversationsList)) // Emit the list directly
                    }
                } ?: run {
                    val errorMessage = response.message ?: "API response data (GetUserConversationsData) is null."
                    Log.e("ChatRepository", "Error: $errorMessage")
                    emit(Result.failure(Exception(errorMessage)))
                }
            } else {
                val errorMessage = response.error ?: "Unknown error from API."
                Log.e("ChatRepository", "API call failed: $errorMessage")
                emit(Result.failure(Exception(errorMessage)))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Exception in getUserConversations: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    suspend fun getConversation(userId: String, page: Int = 1, limit: Int = 50): Flow<Result<ChatMessages>> = flow {
        try {
            val response = RetrofitInstance.Chatapi.getConversation(userId, page, limit)
            if (response.isSuccess()) {
                val data = response.data // Here, response.data is GetConversationData?
                if (data?.messages != null) {
                    Log.d("ChatRepository", "Received messages: ${data.messages}")
                    val messages = data.messages.map { it.toChatMessage() }
                    val pagination = data.pagination.toChatPagination()
                    val otherUser = data.otherUser.toChatUser()
                    val chatMessages = ChatMessages(messages, pagination, otherUser)
                    emit(Result.success(chatMessages))
                } else {
                    Log.d("ChatRepository", "No messages received")
                    val startResponse = RetrofitInstance.Chatapi.startChat(StartChatRequest(userId, ""))
                    Log.d("ChatRepository", "Start response: $startResponse")
                    if (startResponse.isSuccess()) {
                        Log.d("ChatRepository", "Starting new chat")
                        val startData = startResponse.data // Here, startResponse.data is StartChatData?
                        Log.d("ChatRepository", "Start data: $startData")
                        if (startData != null) {
                            Log.d("ChatRepository", "Starting new chat with ID: ${startData.chat_id}")
                            val chatMessage = ChatMessage(
                                id = startData.chat_id,
                                user_sender = startData.sender_id,
                                user_receiver = startData.receiver_id,
                                chat = startData.message,
                                datetime = startData.datetime,
                                status = startData.status,
                                created_at = startData.datetime,
                                updated_at = null,
                                deleted_at = null
                            )
                            val otherUser = startData.receiver?.toChatUser()
                            val chatMessages = ChatMessages(
                                messages = listOf(chatMessage),
                                pagination = ChatPagination(1, 1, 1, false, false, limit),
                                otherUser = otherUser
                            )
                            emit(Result.success(chatMessages))
                        } else {
                            val message = startResponse.error ?: "Unknown error occurred"
                            emit(Result.failure(Exception(message)))
                        }
                    } else {
                        Log.e("ChatRepository", "Failed to start new chat: ${startResponse.error}")
                        emit(Result.failure(Exception(startResponse.error ?: "Failed to create chat.")))
                    }
                }
            } else {
                emit(Result.failure(Exception(response.error ?: "Unknown error")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }


    suspend fun startChat(receiverId: String, message: String): Flow<Result<ChatMessage>> = flow {
        try {
            val request = StartChatRequest(receiverId, message)
            val response = RetrofitInstance.Chatapi.startChat(request)
            if (response.isSuccess()) {
                response.data?.let { responseData -> // Here, responseData is StartChatData?
                    val chatMessage = ChatMessage(
                        id = responseData.chat_id,
                        user_sender = responseData.sender_id,
                        user_receiver = responseData.receiver_id,
                        chat = responseData.message,
                        datetime = responseData.datetime,
                        status = responseData.status,
                        created_at = responseData.datetime,
                        updated_at = null,
                        deleted_at = null
                    )
                    emit(Result.success(chatMessage))
                } ?: emit(Result.failure(Exception("No data received")))
            } else {
                emit(Result.failure(Exception(response.error ?: "Unknown error")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun updateMessageStatus(chatId: String, status: String): Flow<Result<Boolean>> = flow {
        try {
            val request = UpdateMessageStatusRequest(status)
            val response = RetrofitInstance.Chatapi.updateMessageStatus(chatId, request)
            if (response.isSuccess()) {
                emit(Result.success(true))
            } else {
                emit(Result.failure(Exception(response.error ?: "Failed to update message status")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun deleteMessage(chatId: String): Flow<Result<Boolean>> = flow {
        try {
            val response = RetrofitInstance.Chatapi.deleteMessage(chatId)
            if (response.isSuccess()) {
                emit(Result.success(true))
            } else {
                emit(Result.failure(Exception(response.error ?: "Failed to delete message")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
        }
}