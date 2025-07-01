package com.example.projectmdp.ui.module.chat

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.projectmdp.ui.module.chat.ChatListViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class UserItem(
    val id: String,
    val name: String,
    val profilePicture: String? = null // URL or resource ID
)
data class ConversationItem(val otherUserId: String, val otherUser: UserItem, val lastMessage: String, val lastMessageTime: String, val unreadCount: Int = 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    navController: androidx.navigation.NavController
) {
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inbox") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(conversations) { conversation ->
                        ConversationCard(conversation = conversation) {
                            navController.navigate("chat/${conversation.otherUserId}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationCard(
    conversation: ConversationItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = conversation.otherUser.profilePicture,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.LightGray)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.otherUser.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val isoDateFormat = remember {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            }

            val date = try {
                isoDateFormat.parse(conversation.lastMessageTime)
            } catch (e: Exception) {
                Log.e("TransactionDetail", "Error parsing date: ${conversation.lastMessageTime}", e)
                null
            }
            val displayDate = remember(conversation.lastMessageTime) {
                date?.let {
                    val cal = Calendar.getInstance()
                    val today = Calendar.getInstance()

                    cal.time = it

                    val dateFormatSameYear = SimpleDateFormat("MMM dd", Locale.getDefault()) // contoh: Jun 30
                    val dateFormatFull = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                    return@remember when {
                        // Hari ini
                        today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                                today.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) -> {
                            "Today"
                        }

                        // Kemarin
                        today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                                today.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR) == 1 -> {
                            "Yesterday"
                        }

                        // Tahun ini → tampilkan tgl singkat
                        today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) -> {
                            dateFormatSameYear.format(it)
                        }

                        // Tahun berbeda
                        else -> dateFormatFull.format(it)
                    }
                } ?: ""
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayDate, // already formatted
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}