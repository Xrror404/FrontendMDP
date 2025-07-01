package com.example.projectmdp.data.source.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.projectmdp.data.source.local.entity.TransactionEntity
import com.example.projectmdp.data.source.local.entity.UserEntity
import com.example.projectmdp.data.source.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    // Basic CRUD Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)
    
    // Get Transactions
    @Query("SELECT * FROM transactions WHERE transaction_id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?
    
    @Query("SELECT * FROM transactions WHERE transaction_id = :id")
    fun getTransactionByIdFlow(id: String): Flow<TransactionEntity?>
    
    // Get Buyer Transactions
    @Query("SELECT * FROM transactions WHERE email_buyer = :buyerEmail ORDER BY datetime DESC")
    suspend fun getBuyerTransactions(buyerEmail: String): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE email_buyer = :buyerEmail ORDER BY datetime DESC")
    fun getBuyerTransactionsFlow(buyerEmail: String): Flow<List<TransactionEntity>>
    
    // Get Seller Transactions
    @Query("SELECT * FROM transactions WHERE user_seller_id = :sellerId ORDER BY datetime DESC")
    suspend fun getSellerTransactions(sellerId: String): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE user_seller_id = :sellerId ORDER BY datetime DESC")
    fun getSellerTransactionsFlow(sellerId: String): Flow<List<TransactionEntity>>
    
    // Get All Transactions
    @Query("SELECT * FROM transactions ORDER BY datetime DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions ORDER BY datetime DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    // Manual Join Queries - Get Related Data
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?
    
    @Query("SELECT * FROM products WHERE product_id = :productId")
    suspend fun getProductById(productId: String): ProductEntity?
    
    // Filter by Payment Status
    @Query("SELECT * FROM transactions WHERE payment_status = :status ORDER BY datetime DESC")
    suspend fun getTransactionsByPaymentStatus(status: String): List<TransactionEntity>
    
    @Query("SELECT * FROM transactions WHERE payment_status = :status ORDER BY datetime DESC")
    fun getTransactionsByPaymentStatusFlow(status: String): Flow<List<TransactionEntity>>
    
    // Filter by Date Range
    @Query("SELECT * FROM transactions WHERE datetime BETWEEN :startDate AND :endDate ORDER BY datetime DESC")
    suspend fun getTransactionsByDateRange(startDate: String, endDate: String): List<TransactionEntity>
    
    // Update Payment Status
    @Query("UPDATE transactions SET payment_status = :status, payment_description = :description, last_updated = :timestamp WHERE transaction_id = :id")
    suspend fun updatePaymentStatus(
        id: String, 
        status: String, 
        description: String?, 
        timestamp: Long = System.currentTimeMillis()
    )
    
    // Sync Management
    @Query("SELECT * FROM transactions WHERE is_synced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>
    
    @Query("UPDATE transactions SET is_synced = 1 WHERE transaction_id = :id")
    suspend fun markAsSynced(id: String)
    
    @Query("UPDATE transactions SET is_synced = 0 WHERE transaction_id = :id")
    suspend fun markAsUnsynced(id: String)
    
    // Cache Management
    @Query("DELETE FROM transactions WHERE transaction_id = :id")
    suspend fun deleteTransactionById(id: String)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
    
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
    
    // Statistics Queries
    @Query("SELECT COUNT(*) FROM transactions WHERE email_buyer = :buyerEmail")
    suspend fun getBuyerTransactionCount(buyerEmail: String): Int
    
    @Query("SELECT COUNT(*) FROM transactions WHERE user_seller_id = :sellerId")
    suspend fun getSellerTransactionCount(sellerId: String): Int
    
    @Query("SELECT SUM(total_price) FROM transactions WHERE email_buyer = :buyerEmail AND LOWER(payment_status) IN ('completed', 'settlement', 'capture', 'success')")
    suspend fun getBuyerTotalSpent(buyerEmail: String): Double?
    
    @Query("SELECT SUM(total_price) FROM transactions WHERE user_seller_id = :sellerId AND LOWER(payment_status) IN ('completed', 'settlement', 'capture', 'success')")
    suspend fun getSellerTotalEarned(sellerId: String): Double?


    //Tambahan Query untuk Midtrans
    @Query("UPDATE transactions SET snap_token = :snapToken, redirect_url = :redirectUrl, midtrans_order_id = :orderId WHERE transaction_id = :transactionId")
    suspend fun updateMidtransData(
        transactionId: String,
        snapToken: String,
        redirectUrl: String,
        orderId: String
    )

    @Query("UPDATE transactions SET payment_type = :paymentType, va_number = :vaNumber WHERE transaction_id = :transactionId")
    suspend fun updatePaymentMethod(
        transactionId: String,
        paymentType: String,
        vaNumber: String?
    )

    @Query("UPDATE transactions SET settlement_time = :settlementTime, payment_status = 'completed' WHERE midtrans_order_id = :orderId")
    suspend fun updateSettlement(orderId: String, settlementTime: String)

    @Query("SELECT * FROM transactions WHERE snap_token IS NOT NULL AND payment_status = 'pending'")
    suspend fun getPendingMidtransTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE expiry_time < :currentTime AND payment_status = 'pending'")
    suspend fun getExpiredTransactions(currentTime: String): List<TransactionEntity>
}