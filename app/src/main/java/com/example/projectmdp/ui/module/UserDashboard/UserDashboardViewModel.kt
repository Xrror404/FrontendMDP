package com.example.projectmdp.ui.module.UserDashboard

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.repository.ProductRepository
import com.example.projectmdp.data.source.dataclass.Product
import com.example.projectmdp.data.source.local.SessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class UserDashboardViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val sessionManager: SessionManager// Ensure this is correctly injected
) : ViewModel() {

    // Using mutableStateOf for properties directly observed by Compose UI
    // The `by` delegate automatically creates a State object.
    var searchQuery by mutableStateOf("")
        private set // Set to private set so only ViewModel can change directly

    var products by mutableStateOf<List<Product>>(emptyList())
        private set // Set to private set

    var isLoading by mutableStateOf(false)
        private set

    var userInitials by mutableStateOf("U")
        private set

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    // FirebaseFirestore is directly used here for initials, which is fine
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        loadUserInitials()
        loadProducts(forceRefresh = true) // Load products initially when ViewModel is created
    }

    fun onSearchQueryChange(query: String) { // Made fun from open fun
        searchQuery = query
        Log.d("UserDashboardViewModel", "Query changed: $query")
        if (query.isEmpty()) {
            loadProducts(forceRefresh = true) // Load all products when search is cleared
        } else {
            searchProducts()
        }
    }

    fun searchProducts() { // Made fun from open fun
        if (searchQuery.isBlank()) {
            loadProducts(forceRefresh = true) // If search query is blank, just load all
            return
        }
        Log.d("UserDashboardViewModel", "Searching for products with query: $searchQuery")
        // No need for 'if (productRepository != null)' because it's @Inject and non-nullable
        searchProductsWithRepository()
    }

    private fun searchProductsWithRepository() {
        viewModelScope.launch {
            isLoading = true
            try {
                // Ensure productRepository?.searchProducts returns Flow<Result<List<Product>>>
                // and you collect it.
                productRepository.searchProducts(searchQuery).collectLatest { result ->
                    result.fold(
                        onSuccess = { searchResults ->
                            products = searchResults // Update mutableStateOf directly
                            Log.d("UserDashboardViewModel", "Search completed: ${searchResults.size} products found")
                        },
                        onFailure = { error ->
                            Log.e("UserDashboardViewModel", "Search failed: ${error.message}", error)
                            // You might want to show this error in the UI
                            products = emptyList() // Clear products on error
                        }
                    )
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("UserDashboardViewModel", "Search failed with exception: ${e.message}", e)
                products = emptyList() // Clear products on exception
                isLoading = false
            }
        }
    }

    // Main function to load products, now accepts forceRefresh
    fun loadProducts(forceRefresh: Boolean = false) { // Made fun from open fun, added forceRefresh
        Log.d("UserDashboardViewModel", "Loading products (forceRefresh: $forceRefresh)")
        loadProductsWithRepository(forceRefresh) // Pass forceRefresh to the repository call
    }

    private fun loadProductsWithRepository(forceRefresh: Boolean) {
        viewModelScope.launch {
            isLoading = true
            try {
                // Ensure productRepository?.getAllProducts returns Flow<Result<ProductWithPagination>>
                productRepository.getAllProducts(forceRefresh = forceRefresh).collectLatest { result ->
                    result.fold(
                        onSuccess = { productWithPagination ->
                            products = productWithPagination.products // Update mutableStateOf directly
                            Log.d("UserDashboardViewModel", "Products loaded: ${products.size}")
                        },
                        onFailure = { error ->
                            Log.e("UserDashboardViewModel", "Failed to load products: ${error.message}", error)
                            products = emptyList() // Clear products on error
                        }
                    )
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("UserDashboardViewModel", "Failed to load products with exception: ${e.message}", e)
                products = emptyList()
                isLoading = false
            }
        }
    }

    private fun loadUserInitials() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val email = currentUser.email
            val displayName = currentUser.displayName

            userInitials = when {
                !displayName.isNullOrBlank() -> {
                    val names = displayName.split(" ")
                    if (names.size >= 2) {
                        "${names[0][0]}${names[1][0]}".uppercase()
                    } else {
                        displayName.take(2).uppercase()
                    }
                }
                !email.isNullOrBlank() -> email.take(2).uppercase()
                else -> "U"
            }
        }
    }

    fun buyProduct(product: Product) {
        Log.d("UserDashboardViewModel", "Buy product: ${product.name}")
        // Implement your buy logic
    }

    fun chatWithSeller(sellerId: String) {
        Log.d("UserDashboardViewModel", "Chat with seller: $sellerId")
        // Implement your chat logic
    }

    fun logout() {
        Log.d("UserDashboardViewModel", "Logging out user")
        auth.signOut()
        sessionManager.clearToken()

        // Clear any cached data if needed
        products = emptyList()
        searchQuery = ""
    }
}