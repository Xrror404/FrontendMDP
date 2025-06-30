package com.example.projectmdp.ui.module.chat

import android.util.Log // Add this import for Log.d/e
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.repository.ChatRepository
import com.example.projectmdp.data.source.dataclass.Conversation // Make sure this import is present and correct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<ConversationItem>>(emptyList())
    val conversations: StateFlow<List<ConversationItem>> = _conversations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    var errorMessage: String? = null

    init {
        loadConversations()
    }

    fun loadConversations() {
        _isLoading.value = true
        viewModelScope.launch {
            chatRepository.getUserConversations().collect { result: Result<List<com.example.projectmdp.data.source.dataclass.Conversation>> -> // Explicitly typed
                result.onSuccess { list: List<Conversation> -> // Explicitly typed
                    Log.d("ChatListViewModel", "Received conversations: $list")
                    val items = list.map { conversation ->
                        ConversationItem(
                            otherUserId = conversation.otherUserId,
                            otherUser = UserItem(
                                id = conversation.otherUser?.id ?: "",
                                name = conversation.otherUser?.username ?: "Unknown",
                                profilePicture = conversation.otherUser?.profile_picture
                            ),
                            lastMessage = conversation.lastMessage,
                            lastMessageTime = conversation.lastMessageTime,
                            unreadCount = 0
                        )
                    }
                    _conversations.value = items
                }.onFailure {
                    errorMessage = it.localizedMessage ?: "Failed to load conversations"
                    Log.e("ChatListViewModel", "Error loading conversations: $errorMessage")
                }
                _isLoading.value = false
            }
        }

    }
}