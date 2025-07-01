package com.example.projectmdp.ui.module.Midtrans

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.repository.ProductRepository
import com.example.projectmdp.data.repository.TransactionRepository
import com.example.projectmdp.data.source.dataclass.Product
import com.example.projectmdp.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MidtransViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    // UI States
    private val _isLoading = mutableStateOf(false)
    val isLoading = _isLoading

    // Payment Data
    private val _paymentUrl = MutableStateFlow<String?>(null)
    val paymentUrl: StateFlow<String?> = _paymentUrl.asStateFlow()

    private val _paymentToken = MutableStateFlow<String?>(null)
    val paymentToken: StateFlow<String?> = _paymentToken.asStateFlow()

    // Selected product for payment
    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _orderId = MutableStateFlow<String?>(null)
    val orderId: StateFlow<String?> = _orderId.asStateFlow()

    // Error handling
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Navigation events
    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // Payment status
    private val _paymentStatus = MutableStateFlow(PaymentStatus.PENDING)
    val paymentStatus: StateFlow<PaymentStatus> = _paymentStatus.asStateFlow()

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun setSelectedProduct(product: Product) {
        _selectedProduct.value = product
    }

    fun loadProduct(productId: String) {
        Log.d("MidtransViewModel", "Starting loadProduct for ID: $productId")
        _isLoading.value = true
        _errorMessage.value = null // Clear previous errors
        
        viewModelScope.launch {
            try {
                productRepository.getProductById(productId).collect { result ->
                    result.onSuccess { product ->
                        _selectedProduct.value = product
                        _isLoading.value = false
                        Log.d("MidtransViewModel", "Product loaded successfully: ${product.name} (ID: ${product.product_id})")
                    }.onFailure { error ->
                        _errorMessage.value = "Failed to load product: ${error.localizedMessage ?: error.message}"
                        _isLoading.value = false
                        Log.e("MidtransViewModel", "Error loading product with ID: $productId", error)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load product: ${e.message}"
                _isLoading.value = false
                Log.e("MidtransViewModel", "Exception loading product with ID: $productId", e)
            }
        }
    }

    fun createTransaction(quantity: Int = 1) {
        val product = _selectedProduct.value ?: run {
            val errorMsg = "Product not selected - cannot create transaction"
            _errorMessage.value = errorMsg
            Log.e("MidtransViewModel", errorMsg)
            return
        }

        Log.d("MidtransViewModel", "Starting createTransaction for product: ${product.name} (ID: ${product.product_id}), quantity: $quantity")
        _isLoading.value = true
        _errorMessage.value = null // Clear previous errors
        
        viewModelScope.launch {
            try {
                val totalPrice = (product.price).toDouble()
                Log.d("MidtransViewModel", "Total price calculated: $totalPrice")
                
                transactionRepository.createTransaction(
                    productId = product.product_id,
                    quantity = quantity,
                    total_price = totalPrice
                ).collect { result ->
                    result.onSuccess { createResult ->
                        Log.d("MidtransViewModel", "Transaction created successfully: ${createResult.transaction.transaction_id}")
                        _orderId.value = createResult.transaction.transaction_id
                        _paymentToken.value = createResult.snapToken
                        _paymentUrl.value = createResult.redirectUrl
                        _paymentStatus.value = PaymentStatus.PENDING
                        _isLoading.value = false
                        Log.d("MidtransViewModel", "Transaction created successfully")
                        Log.d("MidtransViewModel", "Snap token: ${createResult.snapToken}")
                        Log.d("MidtransViewModel", "Redirect URL: ${createResult.redirectUrl}")
                    }.onFailure { error ->
                        _errorMessage.value = "Failed to create transaction: ${error.localizedMessage ?: error.message}"
                        _isLoading.value = false
                        Log.e("MidtransViewModel", "Error creating transaction for product: ${product.product_id}", error)
                    }
                }
            } catch (e: Exception) {
                Log.e("MidtransViewModel", "Exception creating transaction for product: ${product.product_id}", e)
                _errorMessage.value = "Failed to create transaction: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun checkPaymentStatus(orderId: String) {
        _isLoading.value = true
        Log.d("CheckStatusPayment", "Starting checkPaymentStatus for order ID: $orderId")
        viewModelScope.launch {
            try {
//                transactionRepository.getTransactionById(orderId).collect { result ->
//                    result.onSuccess { transaction ->
//                        _paymentStatus.value = when (transaction.payment_status?.lowercase()) {
//                            "settlement", "capture", "success" -> PaymentStatus.SUCCESS
//                            "pending" -> PaymentStatus.PENDING
//                            "deny", "cancel", "expire", "failure", "failed" -> PaymentStatus.FAILED
//                            else -> PaymentStatus.PENDING
//                        }
//                        _isLoading.value = false
//                        Log.d("MidtransViewModel", "Payment status: ${transaction.payment_status}")
//                    }.onFailure { error ->
//                        _errorMessage.value = error.localizedMessage ?: "Failed to check payment status"
//                        _isLoading.value = false
//                        Log.e("MidtransViewModel", "Error checking payment status", error)
//                    }
//                }
                _orderId.value?.let { id ->
                    Log.d("UpdateRequest", "Updating ID: $id with status: $paymentStatus")
                    transactionRepository.updateTransactionStatus(id, "completed", "Lunas").collect { result ->
                        result.onSuccess {
                            Log.d("Transaction", "Update status success: ${it.payment_status}")
                            _navigationEvent.emit(Routes.USER_DASHBOARD)
                        }.onFailure { err ->
                            Log.e("Transaction", "Update status failed", err)
                            _errorMessage.value = "Failed to update status: ${err.message}"
                        }
                    }
                } ?: Log.e("Midtrans", "Order ID is null, cannot update status")
            } catch (e: Exception) {
                Log.e("MidtransViewModel", "Exception checking payment status", e)
                _errorMessage.value = "Failed to check payment status: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun resetPayment() {
        _paymentUrl.value = null
        _paymentToken.value = null
        _paymentStatus.value = PaymentStatus.PENDING
    }
}

enum class PaymentStatus {
    PENDING, SUCCESS, FAILED
}