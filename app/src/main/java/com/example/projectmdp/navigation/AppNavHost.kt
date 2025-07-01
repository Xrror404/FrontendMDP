package com.example.projectmdp.navigation

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.projectmdp.ui.module.Midtrans.MidtransScreen
import com.example.projectmdp.ui.module.Products.CreateProductScreen
import com.example.projectmdp.ui.module.Products.DetailsScreen
import com.example.projectmdp.ui.module.login.LoginScreen
import com.example.projectmdp.ui.module.register.RegisterScreen
import com.example.projectmdp.ui.module.UserDashboard.UserDashboardScreen
import com.example.projectmdp.ui.module.chat.ChatListScreen
import com.example.projectmdp.ui.module.chat.ChatScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
        modifier = modifier
    ) {
        composable(Routes.LOGIN) {
            // Using hiltViewModel() to properly inject dependencies
            LoginScreen(
                viewModel = hiltViewModel(),
                navController = navController
            )
        }
        composable(Routes.REGISTER) {
            // Using hiltViewModel() to properly inject dependencies
            RegisterScreen(
                viewModel = hiltViewModel(),
                navController = navController
            )
        }
        composable(Routes.USER_DASHBOARD) {
            UserDashboardScreen(
                viewModel = hiltViewModel(),
                navController = navController
            )
        }
        composable(
            route = Routes.PRODUCT_DETAIL,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            Log.d("NavigationDebug", "Arrived at ProductDetailScreen. Received productId: $productId")
            if (productId != null) {
                DetailsScreen(
                    productViewModel = hiltViewModel(),
                    navController = navController,
                    productId = productId
                )
            } else {
                Text("Error: Product ID was null in DetailsScreen.")
                Log.e("NavigationDebug", "Product ID was null for PRODUCT_DETAIL route.")
            }
        }
        composable(route = Routes.ADD_PRODUCT) {
            CreateProductScreen(
                viewModel = hiltViewModel(),
                navController = navController
            )
        }
        composable(Routes.chatRoute("{otherUserId}")) { navBackStackEntry ->
            val otherUserId = navBackStackEntry.arguments?.getString("otherUserId") ?: ""
            val currentUser = FirebaseAuth.getInstance().currentUser?.uid
            Log.d("NavigationDebug", "Arrived at ChatScreen. Received otherUserId: $otherUserId")
            Log.d("NavigationDebug", "Current User ID: $currentUser")
            ChatScreen(
                receiverId = otherUserId,
                navController = navController,
                currentUserId = currentUser ?: ""
            )
        }
        composable(route = Routes.EDIT_PROFILE) {
            com.example.projectmdp.ui.module.EditProfile.EditProfileScreen(
                viewModel = hiltViewModel(),
                navController = navController
            )
        }
        composable(route = Routes.TRANSACTION_HISTORY) {
            com.example.projectmdp.ui.module.TransactionHistory.TransactionHistoryScreen(
                viewModel = hiltViewModel(),
                navController = navController
            )
        }
        composable(
            route = Routes.TRANSACTION_DETAIL,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")
            Log.d("NavigationDebug", "Arrived at TransactionDetailScreen. Received transactionId: $transactionId")
            if (transactionId != null) {
                com.example.projectmdp.ui.module.TransactionDetail.TransactionDetailScreen(
                    transactionId = transactionId,
                    viewModel = hiltViewModel(),
                    navController = navController
                )
            } else {
                Text("Error: Transaction ID was null in TransactionDetailScreen.")
                Log.e("NavigationDebug", "Transaction ID was null for TRANSACTION_DETAIL route.")
            }
        }
        composable(route = Routes.ANALYTICS) {
            com.example.projectmdp.ui.module.Analytics.AnalyticsScreen(
                viewModel = hiltViewModel(),
                navController = navController
            )
        }
        composable(route = Routes.CHAT_LIST) {
            ChatListScreen(
                viewModel = hiltViewModel(),
                navController = navController
            )
        }
        composable(
            route = Routes.MIDTRANS,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType },
                navArgument("price") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            val price = backStackEntry.arguments?.getString("price")?.toDoubleOrNull()

            if (productId != null && price != null) {
                MidtransScreen(
                    navController = navController,
                    productId = productId
                )
            } else {
                Text("Error: Missing product ID or price.")
            }
        }
        composable(
            route = Routes.UPDATE_PRODUCT_WITH_ID, // Use the route with ID placeholder
            arguments = listOf(navArgument("productId") { type = NavType.StringType }) // Declare the argument
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") // Extract the productId
            if (productId != null) {
                CreateProductScreen(
                    viewModel = hiltViewModel(),
                    navController = navController,
                    productId = productId // Pass the productId to CreateProductScreen
                )
            } else {
                Text("Error: Product ID was null for UPDATE_PRODUCT route.")
                Log.e("NavigationDebug", "Product ID was null for UPDATE_PRODUCT route.")
            }
        }

    }
}