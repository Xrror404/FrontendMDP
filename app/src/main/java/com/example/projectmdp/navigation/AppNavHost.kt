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
import com.example.projectmdp.ui.module.Products.CreateProductScreen
import com.example.projectmdp.ui.module.Products.DetailsScreen
import com.example.projectmdp.ui.module.login.LoginScreen
import com.example.projectmdp.ui.module.register.RegisterScreen
import com.example.projectmdp.ui.module.UserDashboard.UserDashboardScreen
import com.example.projectmdp.ui.module.chat.ChatScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

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
    }
}