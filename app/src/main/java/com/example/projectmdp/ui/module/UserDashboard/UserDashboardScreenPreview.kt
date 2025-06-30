//package com.example.projectmdp.ui.module.UserDashboard
//
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.lifecycle.ViewModel
//import androidx.navigation.compose.rememberNavController
//import com.google.firebase.Timestamp
//
//// Preview ViewModel implementation
//class PreviewUserDashboardViewModel : UserDashboardViewModel() {
//
//
//    fun setLoadingState(loading: Boolean) {
//        isLoading = loading
//    }
//}
//
//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun UserDashboardScreenPreview() {
//    val previewViewModel = PreviewUserDashboardViewModel()
//
//    MaterialTheme {
//        UserDashboardScreen(
//            viewModel = previewViewModel,
//            navController = rememberNavController()
//        )
//    }
//}
//
//@Preview(showBackground = true, name = "Loading State")
//@Composable
//fun UserDashboardScreenLoadingPreview() {
//    val previewViewModel = PreviewUserDashboardViewModel()
//    previewViewModel.setLoadingState(true) // Use the public method instead of direct assignment
//
//    MaterialTheme {
//        UserDashboardScreen(
//            viewModel = previewViewModel,
//            navController = rememberNavController()
//        )
//    }
//}