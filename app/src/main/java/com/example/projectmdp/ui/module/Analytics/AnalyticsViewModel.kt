package com.example.projectmdp.ui.module.Analytics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.repository.TransactionRepository
import com.example.projectmdp.data.repository.UserRepository
import com.example.projectmdp.data.source.dataclass.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsData(
    // Buyer Statistics
    val totalSpent: Double = 0.0,
    val totalPurchases: Int = 0,
    val completedPurchases: Int = 0,
    val pendingPurchases: Int = 0,
    val failedPurchases: Int = 0,
    
    // Seller Statistics
    val totalEarned: Double = 0.0,
    val totalSales: Int = 0,
    val completedSales: Int = 0,
    val pendingSales: Int = 0,
    val failedSales: Int = 0,
    
    // Recent Transactions
    val recentTransactions: List<Transaction> = emptyList(),
    
    // Category Breakdown
    val categoryBreakdown: Map<String, CategoryStats> = emptyMap()
)

data class CategoryStats(
    val totalAmount: Double = 0.0,
    val count: Int = 0
)

data class AnalyticsUiState(
    val analyticsData: AnalyticsData = AnalyticsData(),
    val currentUserEmail: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    fun loadAnalytics(forceRefresh: Boolean = false) {
        Log.d("AnalyticsVM", "Loading analytics data, forceRefresh: $forceRefresh")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // First, get current user email
                var currentUserEmail = _uiState.value.currentUserEmail
                
                if (currentUserEmail.isEmpty()) {
                    try {
                        userRepository.getCurrentUserProfile().collect { userResult ->
                            userResult.onSuccess { user ->
                                currentUserEmail = user.email
                                _uiState.value = _uiState.value.copy(currentUserEmail = currentUserEmail)
                                Log.d("AnalyticsVM", "Got current user email: $currentUserEmail")
                                
                                // Now proceed with analytics loading
                                loadAnalyticsWithUserEmail(currentUserEmail, forceRefresh)
                                return@collect
                            }.onFailure { error ->
                                Log.e("AnalyticsVM", "Error getting current user", error)
                                // Continue with fallback approach
                                loadAnalyticsWithFallback(forceRefresh)
                                return@collect
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("AnalyticsVM", "Exception getting current user", e)
                        loadAnalyticsWithFallback(forceRefresh)
                    }
                } else {
                    // User email already available
                    loadAnalyticsWithUserEmail(currentUserEmail, forceRefresh)
                }
            } catch (e: Exception) {
                Log.e("AnalyticsVM", "Exception loading analytics", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load analytics: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun loadAnalyticsWithUserEmail(userEmail: String, forceRefresh: Boolean) {
        try {
            Log.d("AnalyticsVM", "Loading analytics for user: $userEmail")
            
            // Get all transactions for comprehensive analysis
            transactionRepository.getMyTransactions(forceRefresh).collect { result ->
                result.onSuccess { transactions ->
                    Log.d("AnalyticsVM", "Processing ${transactions.size} transactions for analytics")
                    
                    val analyticsData = loadComprehensiveAnalytics(transactions, userEmail)
                    
                    _uiState.value = _uiState.value.copy(
                        analyticsData = analyticsData,
                        currentUserEmail = userEmail,
                        isLoading = false,
                        errorMessage = null
                    )
                }.onFailure { error ->
                    Log.e("AnalyticsVM", "Error loading transactions", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load transactions"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AnalyticsVM", "Exception in loadAnalyticsWithUserEmail", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Failed to load analytics: ${e.message}"
            )
        }
    }
    
    private suspend fun loadAnalyticsWithFallback(forceRefresh: Boolean) {
        try {
            Log.d("AnalyticsVM", "Loading analytics with fallback approach")
            
            // Get all transactions first to extract user email
            transactionRepository.getMyTransactions(forceRefresh).collect { result ->
                result.onSuccess { transactions ->
                    Log.d("AnalyticsVM", "Got ${transactions.size} transactions for fallback analysis")
                    
                    // Try to extract current user email from transactions
                    val currentUserEmail = if (transactions.isNotEmpty()) {
                        // Look for the most frequent buyer email (likely the current user)
                        transactions.groupBy { it.buyer_email }
                            .maxByOrNull { it.value.size }?.key
                            ?: transactions.firstOrNull()?.buyer_email
                            ?: ""
                    } else {
                        ""
                    }
                    
                    Log.d("AnalyticsVM", "Extracted user email from transactions: $currentUserEmail")
                    
                    if (currentUserEmail.isNotEmpty()) {
                        val analyticsData = loadComprehensiveAnalytics(transactions, currentUserEmail)
                        
                        _uiState.value = _uiState.value.copy(
                            analyticsData = analyticsData,
                            currentUserEmail = currentUserEmail,
                            isLoading = false,
                            errorMessage = null
                        )
                    } else {
                        Log.e("AnalyticsVM", "Cannot determine current user email from transactions")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Cannot determine current user"
                        )
                    }
                }.onFailure { error ->
                    Log.e("AnalyticsVM", "Error loading transactions for fallback", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load analytics"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AnalyticsVM", "Exception in loadAnalyticsWithFallback", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Failed to load analytics: ${e.message}"
            )
        }
    }

    private suspend fun loadComprehensiveAnalytics(transactions: List<Transaction>, userEmail: String): AnalyticsData {
        Log.d("AnalyticsVM", "Loading comprehensive analytics for user: $userEmail")
        
        // Debug: Print detailed analytics data
        try {
            val debugInfo = transactionRepository.debugAnalyticsData(userEmail)
            Log.d("AnalyticsVM", debugInfo)
        } catch (e: Exception) {
            Log.e("AnalyticsVM", "Error getting debug info", e)
        }
        
        // Use repository functions for accurate totals
        val totalSpent = try {
            val spent = transactionRepository.getBuyerTotalSpent(userEmail)
            Log.d("AnalyticsVM", "Buyer total spent from repository: $spent")
            spent
        } catch (e: Exception) {
            Log.e("AnalyticsVM", "Error getting buyer total spent from repository", e)
            0.0
        }
        
        val totalEarned = try {
            // Try to get seller total earned using user ID from transactions
            val sellerId = transactions.find { it.seller.email == userEmail }?.seller?.id ?: ""
            Log.d("AnalyticsVM", "Found seller ID for user $userEmail: $sellerId")
            
            if (sellerId.isNotEmpty()) {
                val earned = transactionRepository.getSellerTotalEarned(sellerId)
                Log.d("AnalyticsVM", "Seller total earned from repository: $earned")
                earned
            } else {
                Log.d("AnalyticsVM", "No seller ID found for user, checking if user is in any seller transactions")
                // Alternative: check if user appears as seller by email in any transaction
                val sellerTransactions = transactions.filter { it.seller.email == userEmail }
                Log.d("AnalyticsVM", "Found ${sellerTransactions.size} seller transactions by email match")
                
                if (sellerTransactions.isNotEmpty()) {
                    val sellerId2 = sellerTransactions.first().seller.id
                    Log.d("AnalyticsVM", "Using alternative seller ID: $sellerId2")
                    transactionRepository.getSellerTotalEarned(sellerId2)
                } else {
                    0.0
                }
            }
        } catch (e: Exception) {
            Log.e("AnalyticsVM", "Error getting seller total earned from repository", e)
            0.0
        }
        
        // Use repository functions for transaction counts
        val buyerTransactionCount = try {
            val count = transactionRepository.getBuyerTransactionCount(userEmail)
            Log.d("AnalyticsVM", "Buyer transaction count from repository: $count")
            count
        } catch (e: Exception) {
            Log.e("AnalyticsVM", "Error getting buyer transaction count from repository", e)
            0
        }
        
        val sellerTransactionCount = try {
            val sellerId = transactions.find { it.seller.email == userEmail }?.seller?.id ?: ""
            if (sellerId.isNotEmpty()) {
                val count = transactionRepository.getSellerTransactionCount(sellerId)
                Log.d("AnalyticsVM", "Seller transaction count from repository: $count")
                count
            } else {
                // Fallback to counting from transactions
                val count = transactions.count { it.seller.email == userEmail }
                Log.d("AnalyticsVM", "Seller transaction count from transaction list: $count")
                count
            }
        } catch (e: Exception) {
            Log.e("AnalyticsVM", "Error getting seller transaction count from repository", e)
            0
        }
        
        Log.d("AnalyticsVM", "Final repository stats - Total spent: $totalSpent, Total earned: $totalEarned")
        Log.d("AnalyticsVM", "Final repository stats - Buyer count: $buyerTransactionCount, Seller count: $sellerTransactionCount")
        
        // Calculate detailed statistics from transaction data
        val buyerTransactions = transactions.filter { it.buyer_email == userEmail }
        val sellerTransactions = transactions.filter { it.seller.email == userEmail }
        
        Log.d("AnalyticsVM", "Transaction filtering - Buyer: ${buyerTransactions.size}, Seller: ${sellerTransactions.size}")
        Log.d("AnalyticsVM", "Buyer transactions status breakdown:")
        buyerTransactions.forEachIndexed { index, transaction ->
            Log.d("AnalyticsVM", "  [$index] Status: ${transaction.payment_status}, Price: ${transaction.total_price}")
        }
        Log.d("AnalyticsVM", "Seller transactions status breakdown:")
        sellerTransactions.forEachIndexed { index, transaction ->
            Log.d("AnalyticsVM", "  [$index] Status: ${transaction.payment_status}, Price: ${transaction.total_price}")
        }
        
        // Calculate status breakdowns
        val buyerStats = calculateBuyerStats(buyerTransactions)
        val sellerStats = calculateSellerStats(sellerTransactions)
        
        // Get recent transactions (last 10)
        val recentTransactions = transactions
            .sortedByDescending { it.datetime }
            .take(10)
        
        // Calculate category breakdown
        val categoryBreakdown = calculateCategoryBreakdown(transactions.filter { 
            it.buyer_email == userEmail || it.seller.email == userEmail 
        })
        
        Log.d("AnalyticsVM", "Category breakdown: $categoryBreakdown")
        
        return AnalyticsData(
            totalSpent = totalSpent,
            totalPurchases = buyerTransactionCount,
            completedPurchases = buyerStats.completedCount,
            pendingPurchases = buyerStats.pendingCount,
            failedPurchases = buyerStats.failedCount,
            
            totalEarned = totalEarned,
            totalSales = sellerTransactionCount,
            completedSales = sellerStats.completedCount,
            pendingSales = sellerStats.pendingCount,
            failedSales = sellerStats.failedCount,
            
            recentTransactions = recentTransactions,
            categoryBreakdown = categoryBreakdown
        )
    }

    private fun calculateBuyerStats(transactions: List<Transaction>): BuyerStats {
        val completedCount = transactions.count { isCompletedStatus(it.payment_status) }
        val pendingCount = transactions.count { isPendingStatus(it.payment_status) }
        val failedCount = transactions.count { isFailedStatus(it.payment_status) }
        
        return BuyerStats(
            totalSpent = 0.0,
            totalCount = transactions.size,
            completedCount = completedCount,
            pendingCount = pendingCount,
            failedCount = failedCount
        )
    }
    
    private fun calculateSellerStats(transactions: List<Transaction>): SellerStats {
        val completedCount = transactions.count { isCompletedStatus(it.payment_status) }
        val pendingCount = transactions.count { isPendingStatus(it.payment_status) }
        val failedCount = transactions.count { isFailedStatus(it.payment_status) }
        
        return SellerStats(
            totalEarned = 0.0,
            totalCount = transactions.size,
            completedCount = completedCount,
            pendingCount = pendingCount,
            failedCount = failedCount
        )
    }
    
    private fun calculateCategoryBreakdown(transactions: List<Transaction>): Map<String, CategoryStats> {
        return transactions
            .filter { isCompletedStatus(it.payment_status) }
            .groupBy { it.product.category.ifEmpty { "Other" } }
            .mapValues { (_, categoryTransactions) ->
                CategoryStats(
                    totalAmount = categoryTransactions.sumOf { it.total_price },
                    count = categoryTransactions.size
                )
            }
    }
    
    private fun isCompletedStatus(status: String?): Boolean {
        return status?.lowercase() in listOf("completed", "settlement", "capture", "success")
    }
    
    private fun isPendingStatus(status: String?): Boolean {
        return status?.lowercase() in listOf("pending")
    }
    
    private fun isFailedStatus(status: String?): Boolean {
        return status?.lowercase() in listOf("cancelled", "cancel", "deny", "expire", "failure", "failed")
    }

    fun refreshAnalytics() {
        loadAnalytics(forceRefresh = true)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun setCurrentUserEmail(email: String) {
        _uiState.value = _uiState.value.copy(currentUserEmail = email)
        loadAnalytics()
    }
}

private data class BuyerStats(
    val totalSpent: Double,
    val totalCount: Int,
    val completedCount: Int,
    val pendingCount: Int,
    val failedCount: Int
)

private data class SellerStats(
    val totalEarned: Double,
    val totalCount: Int,
    val completedCount: Int,
    val pendingCount: Int,
    val failedCount: Int
)
