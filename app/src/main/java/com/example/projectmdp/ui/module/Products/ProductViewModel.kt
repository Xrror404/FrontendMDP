package com.example.projectmdp.ui.module.Products

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.repository.ProductRepository
import com.example.projectmdp.data.repository.UserRepository
import com.example.projectmdp.data.source.dataclass.Product
import com.example.projectmdp.data.source.dataclass.User
import com.example.projectmdp.data.source.local.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle,
    private val sessionManager: SessionManager,
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {
    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    private val _selectedProduct = MutableLiveData<Product?>()
    val selectedProduct: LiveData<Product?> = _selectedProduct

    val productId: String = savedStateHandle.get<String>("productId")
        ?: ""

    private val _selectedProductSeller = MutableLiveData<User?>()
    val selectedProductSeller: LiveData<User?> = _selectedProductSeller

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _productCreationSuccess = MutableLiveData<Boolean>()
    val productCreationSuccess: LiveData<Boolean> = _productCreationSuccess

    // ADDED: LiveData for product update success
    private val _productUpdateSuccess = MutableLiveData<Boolean>()
    val productUpdateSuccess: LiveData<Boolean> = _productUpdateSuccess

    private val _currentLoggedInUserId = MutableLiveData<String?>()
    val currentLoggedInUserId: LiveData<String?> = _currentLoggedInUserId

    init {
        if (productId.isNotEmpty()) {
            fetchProductById(productId)
        }
        _currentLoggedInUserId.value = sessionManager.getUserId()
        Log.d("ProductViewModel", "Current Logged In User ID: ${_currentLoggedInUserId.value}")
    }

    fun setSelectedProduct(product: Product?) { // Changed to nullable to allow clearing
        _selectedProduct.value = product
        if (product != null) {
            fetchSelectedProductSeller()
        } else {
            _selectedProductSeller.value = null // Clear seller if product is null
        }
    }


    fun fetchSelectedProductSeller() {
        _isLoading.value = true
        _errorMessage.value = null
        _selectedProductSeller.value = null

        viewModelScope.launch {
            val userId = _selectedProduct.value?.user_id
            if (userId != null) {
                userRepository.getUserById(userId).collect { result ->
                    _isLoading.value = false

                    result.onSuccess { user ->
                        _selectedProductSeller.value = user
                        _errorMessage.value = null
                    }.onFailure { throwable ->
                        _errorMessage.value = throwable.message ?: "Failed to load seller information."
                        _selectedProductSeller.value = null
                    }
                }
            } else {
                _errorMessage.value = "No user ID available for the selected product."
                _isLoading.value = false
            }
        }
    }

    fun fetchProductById(productId: String) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            productRepository.getProductById(productId).collect { result ->
                _isLoading.value = false
                result.onSuccess { product ->
                    _selectedProduct.value = product
                    _errorMessage.value = null
                    fetchSelectedProductSeller()
                }.onFailure { throwable ->
                    _selectedProduct.value = null
                    _errorMessage.value = throwable.message ?: "Unknown error fetching product."
                }
            }
        }
    }

    fun fetchAllProducts(page: Int, limit: Int, forceRefresh: Boolean) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            productRepository.getAllProducts(page, limit, forceRefresh).collect { result ->
                _isLoading.value = false
                result.onSuccess { productWithPagination ->
                    _products.value = productWithPagination.products
                }.onFailure { throwable ->
                    _errorMessage.value = throwable.message ?: "Failed to load products."
                }
            }
        }
    }

    fun createProduct(name: String, description: String, price: Double, category: String, imageUri: Uri?) {
        _isLoading.value = true
        _errorMessage.value = null
        _productCreationSuccess.value = false

        viewModelScope.launch {
            productRepository.addProduct(
                applicationContext,
                name,
                description,
                price,
                category,
                imageUri
            ).collect { result ->
                _isLoading.value = false
                result.onSuccess { createdProduct ->
                    _errorMessage.value = null
                    _productCreationSuccess.value = true
                }.onFailure { throwable ->
                    _errorMessage.value = throwable.message ?: "Failed to create product."
                    _productCreationSuccess.value = false
                }
            }
        }
    }

    private val _productDeletionSuccess = MutableLiveData<Boolean>()
    val productDeletionSuccess: LiveData<Boolean> = _productDeletionSuccess

    fun deleteProduct(productId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _productDeletionSuccess.value = false

        viewModelScope.launch {
            productRepository.deleteProduct(productId).collect { result ->
                _isLoading.value = false
                result.onSuccess {
                    _errorMessage.value = null
                    _productDeletionSuccess.value = true
                }.onFailure { throwable ->
                    _errorMessage.value = throwable.message ?: "Failed to delete product."
                    _productDeletionSuccess.value = false
                }
            }
        }
    }

    // CORRECTED: updateProduct function signature and call to repository
    fun updateProduct(
        productId: String,
        name: String,
        description: String,
        price: Double,
        category: String,
        imageUri: Uri? // Pass imageUri from Composable
    ) {
        _isLoading.value = true
        _errorMessage.value = null
        _productUpdateSuccess.value = false // Use the new update success LiveData

        viewModelScope.launch {
            productRepository.updateProduct(
                productId,
                name,
                description,
                price,
                category,
                imageUri // Pass imageUri
            ).collect { result ->
                _isLoading.value = false
                result.onSuccess { updatedProduct ->
                    _errorMessage.value = null
                    _productUpdateSuccess.value = true
                    _selectedProduct.value = updatedProduct // Update selected product LiveData
                }.onFailure { throwable ->
                    _errorMessage.value = throwable.message ?: "Failed to update product."
                    _productUpdateSuccess.value = false
                }
            }
        }
    }

    fun resetProductCreationStatus() {
        _productCreationSuccess.value = false
        // Also reset product update status if they share the same success UI message/navigation logic
        _productUpdateSuccess.value = false
    }

    // You might also want a separate reset function for update status if your UI handles them differently
    fun resetProductUpdateStatus() {
        _productUpdateSuccess.value = false
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}