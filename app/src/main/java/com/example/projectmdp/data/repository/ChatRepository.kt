package com.example.projectmdp.data.repository

import com.example.projectmdp.data.source.dataclass.ChatMessage
import com.example.projectmdp.data.source.dataclass.ChatPagination
import com.example.projectmdp.data.source.dataclass.Conversation
import com.example.projectmdp.data.source.dataclass.User
import com.example.projectmdp.data.source.remote.ChatApi
import com.example.projectmdp.data.source.remote.RetrofitInstance
import com.example.projectmdp.data.source.response.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

fun com.example.projectmdp.data.source.response.Conversation.toConversation(): Conversation {
    return Conversation(
        otherUserId = this.otherUserId,
        otherUser = this.otherUser?.let { 
            User(
                id = it.id,
                email = it.email,
                username = it.name,
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
//    private val chatApi: ChatApi
    // Note: Chat biasanya real-time, jadi tidak perlu local storage
) {

    suspend fun getUserConversations(): Flow<Result<List<Conversation>>> = flow {
        try {
            val response = RetrofitInstance.Chatapi.getUserConversations()
            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val conversations = responseData.data.conversations.map { it.toConversation() }
                    emit(Result.success(conversations))
                } ?: emit(Result.failure(Exception("No data received")))
            } else {
                emit(Result.failure(Exception(response.error ?: "Unknown error")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun getConversation(userId: String, page: Int = 1, limit: Int = 50): Flow<Result<ChatMessages>> = flow {
        try {
            val response = RetrofitInstance.Chatapi.getConversation(userId, page, limit)
            if (response.isSuccess()) {
                val data = response.data?.data
                if (data != null) {
                    val messages = data.messages.map { it.toChatMessage() }
                    val pagination = data.pagination.toChatPagination()
                    val otherUser = data.otherUser.toChatUser()
                    val chatMessages = ChatMessages(messages, pagination, otherUser)
                    emit(Result.success(chatMessages))
                } else {
                    // 🔁 If no conversation data, start a new one with empty message
                    val startResponse = RetrofitInstance.Chatapi.startChat(StartChatRequest(userId, ""))
                    if (startResponse.isSuccess()) {
                        val startData = startResponse.data?.data
                        if (startData != null) {
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
                            val otherUser = startData.receiver?.toChatUser() // optional, based on your API
                            val chatMessages = ChatMessages(
                                messages = listOf(chatMessage),
                                pagination = ChatPagination(1, 1, 1, false, false, limit),
                                otherUser = otherUser
                            )
                            emit(Result.success(chatMessages))
                        } else {
                            emit(Result.failure(Exception("Failed to start new chat.")))
                        }
                    } else {
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
                response.data?.let { responseData ->
                    val chatMessage = ChatMessage(
                        id = responseData.data.chat_id,
                        user_sender = responseData.data.sender_id,
                        user_receiver = responseData.data.receiver_id,
                        chat = responseData.data.message,
                        datetime = responseData.data.datetime,
                        status = responseData.data.status,
                        created_at = responseData.data.datetime,
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