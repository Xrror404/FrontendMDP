package com.example.projectmdp.ui.module.chat

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.repository.AuthRepository
import com.example.projectmdp.data.repository.ChatMessages
import com.example.projectmdp.data.repository.ChatRepository
import com.example.projectmdp.data.repository.UserRepository
import com.example.projectmdp.data.source.dataclass.ChatMessage
import com.example.projectmdp.data.source.dataclass.User
import com.example.projectmdp.data.source.remote.RetrofitInstance
import com.example.projectmdp.data.source.remote.VerifyTokenRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
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

    var otherUserProfile by mutableStateOf<String?>("")
        private set

    private var _currentUser = mutableStateOf<User?>(null)
    val currentUser: State<User?> = _currentUser

    var paginationInfo by mutableStateOf<ChatMessages?>(null)
        private set

    private var currentUserId: String = "CURRENT_USER_ID" // replace this with real one if not set by setCurrentUserId

    private var refreshJob: Job? = null

    private var activeReceiverId: String? = null

    fun fetchChatHistory(receiverId: String, page: Int = 1, limit: Int = 50) {
        isLoading = true
        errorMessage = null
        Log.d("ChatViewModel", "Fetching chat history with receiverId=$receiverId, page=$page, limit=$limit")

        viewModelScope.launch {
            chatRepository.getConversation(receiverId, page, limit).collectLatest { result ->
                isLoading = false // Set isLoading to false after the API call, regardless of success/failure
                result.onSuccess { chatMessages ->
                    Log.d("ChatViewModel", "Received ${chatMessages.messages.size} messages")
                    Log.d("ChatViewModel", "Current user ID when fetching messages: $currentUserId")

                    // Only update messages if it's the first load or if messages have changed
                    // This prevents unnecessary recompositions if data is identical.
                    if (messages != chatMessages.messages) {
                        messages.clear()
                        messages.addAll(chatMessages.messages)
                    }

                    otherUserName = chatMessages.otherUser?.username ?: "Unknown"
                    otherUserProfile = chatMessages.otherUser?.profile_picture
                    paginationInfo = chatMessages

                    // Debug: Print message senders
                    chatMessages.messages.forEach { msg ->
                        Log.d("ChatViewModel", "Message ${msg.id}: sender=${msg.user_sender}, content=${msg.chat.take(20)}")
                    }
                }.onFailure { error ->
                    errorMessage = when (error) {
                        is HttpException -> {
                            val errorBody = error.response()?.errorBody()?.string()
                            Log.e("ChatViewModel", "HTTP error body: $errorBody")
                            "Server error: ${error.code()} ${error.message()}"
                        }
                        else -> error.localizedMessage ?: "Unknown error"
                    }
                    Log.e("ChatViewModel", "Error fetching chat history", error)
                }
            }
        }
    }

    // New function to start/restart the conversation loading with periodic refresh
    fun startChatConversation(receiverId: String, page: Int = 1, limit: Int = 50) {
        activeReceiverId = receiverId // Store the current receiver ID
        // Cancel any existing refresh job before starting a new one
        refreshJob?.cancel()
        refreshJob = null // Clear the job

        // Start the initial load immediately
        fetchChatHistory(receiverId, page, limit)

        // Start the periodic refresh job
        refreshJob = viewModelScope.launch {
            while (isActive) { // Keep refreshing as long as the coroutine is active
                delay(15000) // Wait for 5 seconds
                Log.d("ChatViewModel", "Performing periodic refresh for receiverId=$receiverId")
                fetchChatHistory(receiverId, page, limit) // Re-fetch the chat history
            }
        }
    }

    fun sendMessage(receiverId: String, messageText: String) {
        if (messageText.isBlank()) return

        val tempMessage = ChatMessage(
            id = "temp_${System.currentTimeMillis()}", // Use a temporary client-side ID
            user_sender = currentUserId,
            user_receiver = receiverId,
            chat = messageText,
            datetime = getCurrentIsoTime(),
            status = "sending", // Indicate that it's being sent
            created_at = getCurrentIsoTime()
        )
        Log.d("ChatViewModel", "Yang ngirim id nya ${tempMessage.user_sender}")

        // Add the temporary message for optimistic UI update
        // We'll add it to the top if reverseLayout is true in LazyColumn
        messages.add(0, tempMessage) // Add to top for reverseLayout in ChatScreen
        // Ensure messages are sorted if you're not solely relying on reverseLayout
        // messages.sortByDescending { it.datetime } // You might need this if ordering isn't guaranteed by add(0, ...)

        isSending = true

        viewModelScope.launch {
            chatRepository.startChat(receiverId, messageText).collectLatest { result ->
                isSending = false
                result.onSuccess { sentMessage ->
                    // Find and update the temporary message with the real one from the server
                    val index = messages.indexOfFirst { it.id == tempMessage.id }
                    if (index != -1) {
                        messages[index] = sentMessage.copy(status = "sent") // Update status if needed
                    } else {
                        // If somehow the temp message isn't found (e.g., race condition), just add the new one
                        messages.add(0, sentMessage) // Add new message at the top
                    }
                    messages.sortByDescending { it.datetime } // Re-sort after update

                    // Trigger an immediate refresh after sending to ensure message status
                    // or any server-side updates are reflected quickly.
                    activeReceiverId?.let { fetchChatHistory(it) }

                }.onFailure {
                    // If failed, remove temp message and show error
                    val index = messages.indexOfFirst { it.id == tempMessage.id }
                    if (index != -1) {
                        messages.removeAt(index)
                    }
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
        Log.d("SetCurrentUserId View Model", "Setting current user ID to: $id")
        viewModelScope.launch {
            try {
                // Kirim token ke backend untuk verifikasi dan ambil user doc ID
                // Assuming `id` here is the Firebase UID passed from ChatScreen
                val response = RetrofitInstance.Userapi.getUserByFirebaseUid(id)

                val userId = response.data?.publicProfile?.id
                Log.d("ChatViewModel", "User ID from response: $userId")

                if (userId == null) {
                    errorMessage = "Gagal memuat data user dari token."
                    Log.e("ChatViewModel", "User ID is null in getUserByFirebaseUid response")
                    return@launch
                }

                currentUserId = userId

                // Ambil detail user dari repository
                userRepository.getUserById(currentUserId).collect { result ->
                    result.onSuccess { user ->
                        _currentUser.value = user
                        Log.d("ChatViewModel", "Current user loaded: ${user.username}")
                    }.onFailure { e ->
                        errorMessage = "Gagal memuat user dari database."
                        Log.e("ChatViewModel", "Failed to load current user", e)
                    }
                }

            } catch (e: HttpException) {
                errorMessage = "Token tidak valid: ${e.code()} ${e.message()}"
                Log.e("ChatViewModel", "HTTP error saat verifikasi token", e)
            } catch (e: Exception) {
                errorMessage = "Terjadi kesalahan saat verifikasi token: ${e.localizedMessage}"
                Log.e("ChatViewModel", "Unexpected error saat verifikasi token", e)
            }
        }
    }

    fun updateCurrentUser(user: User) {
        _currentUser.value = user
        currentUserId = user.id
    }

    fun isMessageFromCurrentUser(message: ChatMessage): Boolean {
        // currentUserId needs to be properly set before this function is called.
        // It's set in `setCurrentUserId`.
        val isFromCurrent = message.user_sender == currentUserId
        Log.d("ChatViewModel", "Message ${message.id}: sender=${message.user_sender}, currentUser=$currentUserId, isFromCurrent=$isFromCurrent")
        return isFromCurrent
    }

    fun getMessageSenderName(message: ChatMessage): String {
        return if (isMessageFromCurrentUser(message)) {
            _currentUser.value?.username ?: "You"
        } else {
            otherUserName.ifBlank { "Unknown" }
        }
    }

    fun getMessageSenderProfile(message: ChatMessage): String? {
        return if (isMessageFromCurrentUser(message)) {
            _currentUser.value?.profile_picture
        } else {
            otherUserProfile
        }
    }

    fun formatMessageTime(datetime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(datetime)

            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error parsing datetime: $datetime", e)
            datetime.substring(11, 16) // fallback to simple substring
        }
    }

    fun clearError() {
        errorMessage = null
    }

    // Helper function for debugging
    fun debugCurrentUserId(): String = currentUserId

    // Helper function to ensure message ordering - reconsider if needed with new refresh logic
    // private fun addMessageSorted(message: ChatMessage) {
    //     val index = messages.indexOfFirst { it.datetime <= message.datetime }
    //     if (index == -1) {
    //         messages.add(message)
    //     } else {
    //         messages.add(index, message)
    //     }
    // }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel() // Cancel the refresh job when the ViewModel is destroyed
        Log.d("ChatViewModel", "ChatViewModel cleared, refresh job cancelled.")
    }
}