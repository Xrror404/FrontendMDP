package com.example.projectmdp.data.source.response

data class ChatMessage(
    val id: String,
    val user_sender: String,
    val user_receiver: String,
    val chat: String,
    val datetime: String,
    val status: String, // "sent", "delivered", "read"
    val created_at: String,
    val updated_at: String?,
    val deleted_at: String?
)

data class OtherUser(
    val id: String,
    val name: String,
    val email: String,
    val profile_picture: String?
)

data class Conversation(
    val otherUserId: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val lastMessageStatus: String, // "sent", "delivered", "read"
    val lastMessageSender: String,
    val otherUser: OtherUser?
)

data class ChatPagination(
    val currentPage: Int,
    val totalPages: Int,
    val totalMessages: Int,
    val hasNext: Boolean,
    val hasPrev: Boolean,
    val limit: Int
)

// Request DTOs
data class StartChatRequest(
    val receiver_id: String,
    val message: String
)

data class UpdateMessageStatusRequest(
    val status: String // "delivered" or "read"
)

// Response DTOs
data class StartChatResponse(
    val status: String,
    val message: String,
    val data: StartChatData
)

data class StartChatData(
    val chat_id: String,
    val sender_id: String,
    val receiver_id: String,
    val message: String,
    val datetime: String,
    val status: String,
    val receiver: OtherUser?
)


data class GetUserConversationsResponse(
    val status: String,
    val message: String,
    val data: GetUserConversationsData
)

data class GetUserConversationsData(
    val conversations: List<Conversation>,
    val total: Int
)

data class GetConversationResponse(
    val status: String,
    val message: String,
    val data: GetConversationData
)

data class GetConversationData(
    val messages: List<ChatMessage>,
    val pagination: ChatPagination,
    val otherUser: OtherUser
)

data class UpdateMessageStatusResponse(
    val status: String,
    val message: String,
    val data: UpdateMessageStatusData
)

data class UpdateMessageStatusData(
    val chat_id: String,
    val status: String,
    val updated_at: String
)

data class DeleteMessageResponse(
    val status: String,
    val message: String,
    val data: DeleteMessageData
)

data class DeleteMessageData(
    val chat_id: String,
    val deleted_at: String
)