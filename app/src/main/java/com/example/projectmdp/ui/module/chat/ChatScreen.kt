package com.example.projectmdp.ui.module.chat

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.projectmdp.data.source.dataclass.ChatMessage
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    receiverId: String,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
    navController: NavController,
    currentUserId: String
) {
    var messageText by remember { mutableStateOf("") }

    // --- Start of Modified Section ---
    LaunchedEffect(receiverId, currentUserId) {
        Log.d("Chat Screen", "Current user Firebase UID : $currentUserId")

        viewModel.setCurrentUserId(currentUserId)

        // 2. Wait for the ViewModel to successfully load the current user's database ID.
        // We use snapshotFlow to observe the `currentUser` state and filter for non-null `id`.
        // `.first()` ensures this block only executes once after the ID is available.
        val dbUserId = snapshotFlow { viewModel.currentUser.value?.id }
            .filterNotNull() // Only proceed when the ID is available
            .first() // Get the first non-null ID and then stop observing

        Log.d("Chat Screen", "Current user DB ID ready: $dbUserId. Starting chat conversation.")

        // 3. Once the current user's database ID is known, start the conversation.
        // This function in the ViewModel now handles both initial load and periodic refreshing.
        viewModel.startChatConversation(receiverId)
    }
    // --- End of Modified Section ---

    val messages = viewModel.messages
    val isSending = viewModel.isSending
    val isLoading = viewModel.isLoading
    val error = viewModel.errorMessage
    val otherUserName = viewModel.otherUserName
    val otherUserProfile = viewModel.otherUserProfile

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header dengan info user lawan chat
        ChatHeader(
            userName = otherUserName,
            userProfile = otherUserProfile,
            onNavigateBack = { navController.popBackStack() }
        )

        // Messages area
        Box(
            modifier = Modifier.weight(1f)
        ) {
            if (isLoading && messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.Bottom,
                    reverseLayout = true
                ) {
                    // Sorting here is crucial if your ViewModel doesn't guarantee order
                    items(messages.sortedByDescending { it.datetime }, key = { it.id }) { message ->
                        ChatMessageItem(
                            message = message,
                            isFromCurrentUser = viewModel.isMessageFromCurrentUser(message),
                            senderName = viewModel.getMessageSenderName(message),
                            senderProfile = viewModel.getMessageSenderProfile(message),
                            messageTime = viewModel.formatMessageTime(message.datetime),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // Error message overlay
            error?.let { errorMsg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, Color.Red)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMsg,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss", color = Color.Red)
                        }
                    }
                }
            }
        }

        // Message input area
        ChatInputArea(
            messageText = messageText,
            onMessageTextChange = { messageText = it },
            onSendMessage = {
                if (messageText.isNotBlank()) {
                    viewModel.sendMessage(receiverId, messageText)
                    messageText = ""
                }
            },
            isSending = isSending
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatHeader(
    userName: String,
    userProfile: String?,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Profile picture
                Box(
                    modifier = Modifier.size(40.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(userProfile)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.3f)),
                        contentScale = ContentScale.Crop
                    )

                }

                Spacer(modifier = Modifier.width(12.dp))

                // User info
                Column {
                    Text(
                        text = userName.ifBlank { "Loading..." },
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    isFromCurrentUser: Boolean,
    senderName: String,
    senderProfile: String?,
    messageTime: String,
    modifier: Modifier = Modifier
) {
    // Debug logging
    Log.d("ChatMessageItem", "Message ${message.id}: isFromCurrentUser=$isFromCurrentUser, sender=${message.user_sender}, senderName=$senderName")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromCurrentUser) {
            // Profile picture untuk pesan dari user lain (kiri)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(senderProfile)
                    .crossfade(true)
                    .build(),
                contentDescription = "Sender Profile",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.3f)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message bubble
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .wrapContentWidth(),
            shape = RoundedCornerShape(
                topStart = if (isFromCurrentUser) 16.dp else 4.dp,
                topEnd = if (isFromCurrentUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isFromCurrentUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.White
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Sender name (hanya untuk pesan dari user lain)
                if (!isFromCurrentUser) {
                    Text(
                        text = senderName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Message text
                Text(
                    text = message.chat,
                    fontSize = 14.sp,
                    color = if (isFromCurrentUser) Color.White else Color.Black,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Time and status
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = messageTime,
                        fontSize = 11.sp,
                        color = if (isFromCurrentUser) {
                            Color.White.copy(alpha = 0.7f)
                        } else {
                            Color.Gray
                        }
                    )

                    if (isFromCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        // Message status (sent, delivered, read)
                        MessageStatusIcon(
                            status = message.status,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        if (isFromCurrentUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // Placeholder untuk profile picture current user (bisa ditambahkan jika perlu)
            Box(modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun MessageStatusIcon(
    status: String,
    color: Color
) {
    when (status) {
        "sent" -> {
            Text(
                text = "✓",
                fontSize = 12.sp,
                color = color
            )
        }
        "delivered" -> {
            Text(
                text = "✓✓",
                fontSize = 10.sp,
                color = color
            )
        }
        "read" -> {
            Text(
                text = "✓✓",
                fontSize = 10.sp,
                color = Color.Blue
            )
        }
        "sending" -> { // Added status for optimistic update
            Text(
                text = "...",
                fontSize = 10.sp,
                color = color
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputArea(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isSending: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Message input field
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send button
            FloatingActionButton(
                onClick = onSendMessage,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// Legacy component (dapat dihapus jika tidak digunakan)
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