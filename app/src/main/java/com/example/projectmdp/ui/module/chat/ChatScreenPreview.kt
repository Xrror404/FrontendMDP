package com.example.projectmdp.ui.module.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.projectmdp.data.source.dataclass.User

/**
 * Demo dan contoh penggunaan ChatScreen yang sudah di-update
 * dengan tampilan WhatsApp-like
 */

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    val navController = rememberNavController()
    
    ChatScreen(
        receiverId = "receiver123",
        navController = navController,
        currentUserId = "current456"
    )
}

/**
 * Contoh penggunaan ChatScreen dengan User object
 */
@Composable
fun ChatScreenExample(
    receiverId: String,
    navController: NavController
) {
    val currentUser = User(
        id = "current_user_123",
        username = "John Doe",
        email = "john@example.com",
        phone_number = "+628123456789",
        profile_picture = "https://example.com/profile.jpg",
        address = "Jakarta",
        role = "buyer",
        firebase_uid = null,
        auth_provider = "local",
        created_at = "2024-01-01",
        deleted_at = null
    )
    
    ChatScreen(
        receiverId = receiverId,
        navController = navController,
        currentUserId = currentUser.id
    )
}
