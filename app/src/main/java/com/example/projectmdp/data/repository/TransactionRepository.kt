package com.example.projectmdp.data.repository

import android.util.Log
import com.example.projectmdp.data.source.dataclass.Transaction
import com.example.projectmdp.data.source.dataclass.User
import com.example.projectmdp.data.source.dataclass.Product
import com.example.projectmdp.data.source.local.dao.TransactionDao
import com.example.projectmdp.data.source.local.entity.TransactionEntity
import com.example.projectmdp.data.source.remote.RetrofitInstance
import com.example.projectmdp.data.source.remote.TransactionApi
import com.example.projectmdp.data.source.remote.CreateTransactionRequest
import com.example.projectmdp.data.source.remote.UpdateTransactionStatusRequest
import com.example.projectmdp.data.source.response.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Data class untuk transaction details dengan manual join
data class TransactionDetails(
    val transaction: Transaction,
    val seller: User?,
    val product: Product?
)

data class CreateTransactionResult(
    val transaction: Transaction,
    val snapToken: String,
    val redirectUrl: String
)

// Extension functions untuk mapping
fun com.example.projectmdp.data.source.response.Transaction.toTransaction(): Transaction {
    // Validate required fields
    Log.d("TransactionMapping", "${product.product_id}")
    val productId = this.product.product_id
    val productName = this.product.name
    val sellerId = this.user_seller.id
    
    android.util.Log.d("TransactionMapping", "Mapping transaction:")
    android.util.Log.d("TransactionMapping", "Product ID: $productId")
    android.util.Log.d("TransactionMapping", "Product Name: $productName")
    android.util.Log.d("TransactionMapping", "Seller ID: $sellerId")
    
    if (productId.isNullOrEmpty()) {
        throw IllegalArgumentException("Product ID cannot be null or empty")
    }
    if (productName.isNullOrEmpty()) {
        throw IllegalArgumentException("Product name cannot be null or empty")
    }
    if (sellerId.isNullOrEmpty()) {
        throw IllegalArgumentException("Seller ID cannot be null or empty")
    }
    
    return Transaction(
        transaction_id = this.transaction_id,
        seller = User(
            id = sellerId,
            email = this.user_seller.email ?: "",
            username = this.user_seller.name ?: "",
            phone_number = this.user_seller.phone ?: "",
            profile_picture = this.user_seller.profile_picture,
            address = "",
            role = "both",
            firebase_uid = null,
            auth_provider = "local",
            created_at = "",
            deleted_at = null
        ),
        buyer_email = this.email_buyer,
        product = Product(
            product_id = productId,
            name = productName,
            description = this.product.description,
            price = this.product.price ?: 0.0,
            category = this.product.category ?: "",
            image = this.product.image_url ?: "",
            user_id = sellerId,
            created_at = "",
            deleted_at = null
        ),
        quantity = this.quantity,
        total_price = this.total_price,
        datetime = this.datetime,
        payment_id = this.payment_id,
        payment_status = this.payment_status,
        payment_description = this.payment_description,
        user_role = this.user_role,

        // === Mapping Midtrans Fields ===
        midtrans_order_id = this.midtrans_order_id,
        snap_token = this.snap_token,
        redirect_url = this.redirect_url,
        payment_type = this.payment_type,
        va_number = this.va_number,
        pdf_url = this.pdf_url,
        settlement_time = this.settlement_time,
        expiry_time = this.expiry_time
    )
}

fun com.example.projectmdp.data.source.response.Transaction.toTransactionEntity(): TransactionEntity {
    // Validate required fields
    val productId = this.product.product_id
    val sellerId = this.user_seller.id
    
    android.util.Log.d("TransactionMapping", "Mapping to entity:")
    android.util.Log.d("TransactionMapping", "Product ID: $productId")
    android.util.Log.d("TransactionMapping", "Seller ID: $sellerId")
    
    if (productId.isNullOrEmpty()) {
        throw IllegalArgumentException("Product ID cannot be null or empty for entity mapping")
    }
    if (sellerId.isNullOrEmpty()) {
        throw IllegalArgumentException("Seller ID cannot be null or empty for entity mapping")
    }
    
    return TransactionEntity(
        transaction_id = this.transaction_id,
        user_seller_id = sellerId,
        email_buyer = this.email_buyer,
        product_id = productId,
        quantity = this.quantity,
        total_price = this.total_price,
        datetime = this.datetime,
        payment_id = this.payment_id,
        payment_status = this.payment_status,
        payment_description = this.payment_description,

        // Mapping Midtrans Fields
        midtrans_order_id = this.midtrans_order_id,
        snap_token = this.snap_token,
        redirect_url = this.redirect_url,
        payment_type = this.payment_type,
        va_number = this.va_number,
        pdf_url = this.pdf_url,
        settlement_time = this.settlement_time,
        expiry_time = this.expiry_time,

        lastUpdated = System.currentTimeMillis(),
        isSynced = true
    )
}

fun TransactionEntity.toTransaction(): Transaction {
    return Transaction(
        transaction_id = this.transaction_id,
        seller = User.empty().copy(id = this.user_seller_id), // Placeholder, perlu di-join manual
        buyer_email = this.email_buyer,
        product = Product.empty().copy(product_id = this.product_id), // Placeholder, perlu di-join manual
        quantity = this.quantity,
        total_price = this.total_price,
        datetime = this.datetime,
        payment_id = this.payment_id,
        payment_status = this.payment_status,
        payment_description = this.payment_description,
        user_role = null,

        // === Mapping Midtrans Fields ===
        midtrans_order_id = this.midtrans_order_id,
        snap_token = this.snap_token,
        redirect_url = this.redirect_url,
        payment_type = this.payment_type,
        va_number = this.va_number,
        pdf_url = this.pdf_url,
        settlement_time = this.settlement_time,
        expiry_time = this.expiry_time
    )
}

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
//    private val transactionApi: TransactionApi
) {

    // Create Transaction (Remote + Local Cache)
    suspend fun createTransaction(
        productId: String,
        quantity: Int,
        total_price: Double
    ): Flow<Result<CreateTransactionResult>> = flow {
        try {
            val request = CreateTransactionRequest(productId, quantity, total_price)
            android.util.Log.d("TransactionRepository", "Creating transaction for product: $productId, quantity: $quantity, price: $total_price")
            
            val response = RetrofitInstance.Transactionapi.createTransaction(request)
            val rawJson = Gson().toJson(response)
            Log.d("RawTransactionJSON", rawJson)
//            Log.d("TransactionRepository", "${response.data?.transaction}")
            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    android.util.Log.d("TransactionRepository", "Transaction API response received")
                    android.util.Log.d("TransactionRepository", "Product ID from response: ${responseData.transaction.product.product_id}")
                    android.util.Log.d("TransactionRepository", "Product name from response: ${responseData.transaction.product.name}")
                    android.util.Log.d("TransactionRepository", "Snap token: ${responseData.snap_token}")
                    android.util.Log.d("TransactionRepository", "Redirect URL: ${responseData.redirect_url}")

                    val transaction = responseData.transaction.toTransaction()
                    
                    // Cache to local database
                    val transactionEntity = responseData.transaction.toTransactionEntity()
                    transactionDao.insertTransaction(transactionEntity)

                    val result = CreateTransactionResult(
                        transaction = transaction,
                        snapToken = responseData.snap_token,
                        redirectUrl = responseData.redirect_url
                    )

                    // Cache related user and product data
                    cacheRelatedData(responseData.transaction)
                    
                    android.util.Log.d("TransactionRepository", "Transaction created successfully")
                    emit(Result.success(result))
                } ?: run {
                    android.util.Log.e("TransactionRepository", "No transaction data received")
                    emit(Result.failure(Exception("No transaction data received")))
                }
            } else {
                android.util.Log.e("TransactionRepository", "API error: ${response.error}")
                emit(Result.failure(Exception(response.error ?: "Failed to create transaction")))
            }
        } catch (e: Exception) {
            android.util.Log.e("TransactionRepository", "Exception creating transaction", e)
            emit(Result.failure(e))
        }
    }

    // Get My Transactions (Cache-First + Remote Sync)
    suspend fun getMyTransactions(forceRefresh: Boolean = false): Flow<Result<List<Transaction>>> = flow {
        try {
            // Emit cached data first for better UX
            if (!forceRefresh) {
                val cachedTransactions = buildTransactionsFromCache()
                if (cachedTransactions.isNotEmpty()) {
                    emit(Result.success(cachedTransactions))
                }
            }

            // Fetch from remote
            val response = RetrofitInstance.Transactionapi.getMyTransactions()
            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val transactions = responseData.transactions.map { it.toTransaction() }
                    
                    // Cache to local database
                    val transactionEntities = responseData.transactions.map { it.toTransactionEntity() }
                    transactionDao.deleteAllTransactions() // Clear old cache
                    transactionDao.insertTransactions(transactionEntities)
                    
                    // Cache related data
                    responseData.transactions.forEach { cacheRelatedData(it) }
                    
                    emit(Result.success(transactions))
                } ?: emit(Result.failure(Exception("No transactions received")))
            } else {
                // Return cached data if remote fails
                val cachedTransactions = buildTransactionsFromCache()
                if (cachedTransactions.isNotEmpty()) {
                    if (!forceRefresh) {
                        // Already emitted above
                    } else {
                        emit(Result.success(cachedTransactions))
                    }
                } else {
                    emit(Result.failure(Exception(response.error ?: "Failed to fetch transactions")))
                }
            }
        } catch (e: Exception) {
            // Fallback to cached data
            try {
                val cachedTransactions = buildTransactionsFromCache()
                if (cachedTransactions.isNotEmpty()) {
                    emit(Result.success(cachedTransactions))
                } else {
                    emit(Result.failure(e))
                }
            } catch (cacheError: Exception) {
                emit(Result.failure(e))
            }
        }
    }

    // Get Transaction by ID (Simplified for Payment Status Check)
    suspend fun getTransactionById(transactionId: String): Flow<Result<Transaction>> = flow {
        try {
            // Check cache first
            transactionDao.getTransactionById(transactionId)?.let { transactionEntity ->
                val transaction = buildTransactionFromEntity(transactionEntity)
                emit(Result.success(transaction))
            }

            // Fetch from remote for fresh data
            val response = RetrofitInstance.Transactionapi.getTransactionById(transactionId)
            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val transaction = responseData.transaction.toTransaction()
                    
                    // Update cache
                    val transactionEntity = responseData.transaction.toTransactionEntity()
                    transactionDao.insertTransaction(transactionEntity)
                    
                    // Cache related data
                    cacheRelatedData(responseData.transaction)
                    
                    emit(Result.success(transaction))
                } ?: emit(Result.failure(Exception("Transaction not found")))
            } else {
                // If remote fails but we have cache, that's OK (already emitted above)
                transactionDao.getTransactionById(transactionId) ?: emit(
                    Result.failure(Exception(response.error ?: "Transaction not found"))
                )
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // Get Transaction by ID with Details (Manual Join)
    suspend fun getTransactionWithDetails(transactionId: String): Flow<Result<TransactionDetails>> = flow {
        try {
            // Check cache first
            transactionDao.getTransactionById(transactionId)?.let { transactionEntity ->
                val seller = transactionDao.getUserById(transactionEntity.user_seller_id)
                val product = transactionDao.getProductById(transactionEntity.product_id)
                
                val transaction = transactionEntity.toTransaction()
                val sellerUser = seller?.let { User.fromUserEntity(it) }
                val productData = product?.let { Product.fromProductEntity(it) }
                
                val details = TransactionDetails(
                    transaction = transaction.copy(
                        seller = sellerUser ?: User.empty(),
                        product = productData ?: Product.empty()
                    ),
                    seller = sellerUser,
                    product = productData
                )
                
                emit(Result.success(details))
            }

            // Fetch from remote for fresh data
            val response = RetrofitInstance.Transactionapi.getTransactionById(transactionId)
            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val transaction = responseData.transaction.toTransaction()
                    
                    // Update cache
                    val transactionEntity = responseData.transaction.toTransactionEntity()
                    transactionDao.insertTransaction(transactionEntity)
                    
                    // Cache related data
                    cacheRelatedData(responseData.transaction)
                    
                    val details = TransactionDetails(
                        transaction = transaction,
                        seller = transaction.seller,
                        product = transaction.product
                    )
                    
                    emit(Result.success(details))
                } ?: emit(Result.failure(Exception("Transaction not found")))
            } else {
                // If remote fails but we have cache, that's OK (already emitted above)
                transactionDao.getTransactionById(transactionId) ?: emit(
                    Result.failure(Exception(response.error ?: "Transaction not found"))
                )
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // Update Transaction Status
    suspend fun updateTransactionStatus(
        transactionId: String,
        paymentStatus: String,
        paymentDescription: String = ""
    ): Flow<Result<Transaction>> = flow {
        try {
            val request = UpdateTransactionStatusRequest(paymentStatus, paymentDescription)
            val response = RetrofitInstance.Transactionapi.updateTransactionStatus(transactionId, request)
            
            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val transaction = responseData.transaction.toTransaction()
                    
                    // Update local cache
                    transactionDao.updatePaymentStatus(
                        transactionId, 
                        paymentStatus, 
                        paymentDescription.ifEmpty { null }
                    )
                    
                    emit(Result.success(transaction))
                } ?: emit(Result.failure(Exception("Failed to update transaction")))
            } else {
                emit(Result.failure(Exception(response.error ?: "Failed to update transaction")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // Local-only operations for offline support
    fun getBuyerTransactionsFlow(buyerEmail: String): Flow<List<Transaction>> {
        return transactionDao.getBuyerTransactionsFlow(buyerEmail).map { entities ->
            entities.map { buildTransactionFromEntity(it) }
        }
    }

    fun getSellerTransactionsFlow(sellerId: String): Flow<List<Transaction>> {
        return transactionDao.getSellerTransactionsFlow(sellerId).map { entities ->
            entities.map { buildTransactionFromEntity(it) }
        }
    }

    suspend fun getTransactionsByPaymentStatus(status: String): List<Transaction> {
        return transactionDao.getTransactionsByPaymentStatus(status).map { 
            buildTransactionFromEntity(it) 
        }
    }

    // Helper methods
    private suspend fun buildTransactionsFromCache(): List<Transaction> {
        val entities = transactionDao.getAllTransactionsList()
        return entities.map { buildTransactionFromEntity(it) }
    }
    
    private suspend fun getAllCachedTransactions(): List<TransactionEntity> {
        return transactionDao.getAllTransactionsList()
    }

    private suspend fun buildTransactionFromEntity(entity: TransactionEntity): Transaction {
        val seller = transactionDao.getUserById(entity.user_seller_id)
        val product = transactionDao.getProductById(entity.product_id)
        
        return entity.toTransaction().copy(
            seller = seller?.let { User.fromUserEntity(it) } ?: User.empty(),
            product = product?.let { Product.fromProductEntity(it) } ?: Product.empty()
        )
    }

    private suspend fun cacheRelatedData(transaction: com.example.projectmdp.data.source.response.Transaction) {
        // This would be implemented if you have UserDao and ProductDao
        // For now, we assume the related data is cached by other repositories
    }

    // Statistics and Analytics
    suspend fun getBuyerTransactionCount(buyerEmail: String): Int {
        return transactionDao.getBuyerTransactionCount(buyerEmail)
    }

    suspend fun getSellerTransactionCount(sellerId: String): Int {
        return transactionDao.getSellerTransactionCount(sellerId)
    }

    suspend fun getBuyerTotalSpent(buyerEmail: String): Double {
        return transactionDao.getBuyerTotalSpent(buyerEmail) ?: 0.0
    }

    suspend fun getSellerTotalEarned(sellerId: String): Double {
        return transactionDao.getSellerTotalEarned(sellerId) ?: 0.0
    }

    // Cache Management
    suspend fun clearTransactionCache() {
        transactionDao.deleteAllTransactions()
    }
}