package com.example.projectmdp.ui.module.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.projectmdp.data.source.dataclass.ChatMessage

@Composable
fun ChatScreen(
    receiverId: String,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
    navController: NavController,
    currentUserId: String
) {
    var messageText by remember { mutableStateOf("") }

    // One-time setup to load messages and set current user
    LaunchedEffect(receiverId) {
        viewModel.setCurrentUserId(currentUserId)
        viewModel.loadConversation(receiverId)
    }

    val messages = viewModel.messages
    val isSending = viewModel.isSending
    val isLoading = viewModel.isLoading
    val error = viewModel.errorMessage

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(messages.sortedByDescending { it.datetime }) { message ->
                MessageBubble(
                    text = message.chat,
                    isUser = message.user_sender == currentUserId
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Type a message") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(receiverId, messageText)
                        messageText = ""
                    }
                },
                enabled = !isSending
            ) {
                Text("Send")
            }
        }

        if (error != null) {
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun MessageBubble(text: String, isUser: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
