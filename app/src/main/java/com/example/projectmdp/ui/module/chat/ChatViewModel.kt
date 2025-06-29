package com.example.projectmdp.ui.module.chat

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.repository.ChatMessages
import com.example.projectmdp.data.repository.ChatRepository
import com.example.projectmdp.data.source.dataclass.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {

    var messages = mutableStateListOf<ChatMessage>()
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isSending by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var otherUserName by mutableStateOf<String>("")
        private set

    var paginationInfo by mutableStateOf<ChatMessages?>(null)
        private set

    private var currentUserId: String = "CURRENT_USER_ID" // replace this with real one

    fun loadConversation(receiverId: String, page: Int = 1, limit: Int = 50) {
        isLoading = true
        errorMessage = null

        viewModelScope.launch {
            chatRepository.getConversation(receiverId, page, limit).collectLatest { result ->
                isLoading = false
                result.onSuccess { chatMessages ->
                    messages.clear()
                    messages.addAll(chatMessages.messages)
                    otherUserName = chatMessages.otherUser?.username ?: "Unknown"
                    paginationInfo = chatMessages
                }.onFailure {
                    errorMessage = it.localizedMessage ?: "Failed to load conversation"
                    Log.e("ChatViewModel", "Error loading conversation", it)
                }
            }
        }
    }

    fun sendMessage(receiverId: String, messageText: String) {
        if (messageText.isBlank()) return

        val tempMessage = ChatMessage(
            id = "temp_${System.currentTimeMillis()}",
            user_sender = currentUserId,
            user_receiver = receiverId,
            chat = messageText,
            datetime = getCurrentIsoTime(),
            status = "sent",
            created_at = getCurrentIsoTime()
        )

        messages.add(tempMessage)
        isSending = true

        viewModelScope.launch {
            chatRepository.startChat(receiverId, messageText).collectLatest { result ->
                isSending = false
                result.onSuccess { sentMessage ->
                    // Remove temp and insert real message
                    messages.remove(tempMessage)
                    messages.add(sentMessage)
                }.onFailure {
                    errorMessage = it.localizedMessage ?: "Failed to send message"
                    Log.e("ChatViewModel", "Error sending message", it)
                }
            }
        }
    }

    fun getCurrentIsoTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    fun setCurrentUserId(id: String) {
        viewModelScope.launch {

        }
    }
}
