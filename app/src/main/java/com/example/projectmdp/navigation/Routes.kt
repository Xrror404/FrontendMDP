package com.example.projectmdp.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val USER_DASHBOARD = "user_dashboard"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    const val ADD_PRODUCT = "add_product"
    const val CHAT = "chat/{otherUserId}"
    const val EDIT_PROFILE = "edit_profile"
    const val TRANSACTION_HISTORY = "transaction_history"
    const val CHAT_LIST = "chat_list"
    const val TRANSACTION = "transaction"
    const val UPDATE_PRODUCT_WITH_ID = "update_product/{productId}"
    const val MIDTRANS = "midtrans/{productId}/{price}"
    // Helper function to create the route with an ID
    fun productDetailRoute(productId: String): String {
        return "product_detail/$productId"
    }
    fun chatRoute(otherUserId: String): String {
        return "chat/$otherUserId"
    }

    fun transactionRoute(transactionId: String): String{
        return "transaction/$transactionId"
    }
    fun midtransRoute(productId: String, price: Double): String {
        return "midtrans/$productId/$price"
    }
    fun updateProductRoute(productId: String): String {
        return "update_product/$productId"
    }
}