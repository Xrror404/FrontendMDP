package com.example.projectmdp.ui.module.UserDashboard

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.repository.ProductRepository
import com.example.projectmdp.data.repository.ProductWithPagination // Pastikan import ini ada
import com.example.projectmdp.data.source.dataclass.Product
import com.example.projectmdp.data.source.local.SessionManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class UserDashboardViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // --- State untuk UI yang diamati oleh Compose ---
    var searchQuery by mutableStateOf("")
        private set
    var products by mutableStateOf<List<Product>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var userInitials by mutableStateOf("U")
        private set
    var selectedCategory by mutableStateOf<String?>("All Categories")
        private set

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    init {
        loadUserInitials()
        loadProducts(forceRefresh = true)
    }

    /**
     * Memperbarui query pencarian. Jika query kosong, muat ulang produk sesuai filter terakhir.
     */
    fun onSearchQueryChange(query: String) {
        searchQuery = query
        Log.d("UserDashboardViewModel", "Query changed: $query")
        if (query.isBlank()) {
            // Jika search bar dikosongkan, muat ulang berdasarkan kategori yang terakhir dipilih
            val lastCategory = selectedCategory ?: "All Categories"
            filterProductsByCategory(lastCategory)
        } else {
            searchProducts()
        }
    }

    /**
     * Menjalankan pencarian produk berdasarkan searchQuery.
     */
    fun searchProducts() {
        if (searchQuery.isBlank()) {
            loadProducts(forceRefresh = true)
            return
        }
        Log.d("UserDashboardViewModel", "Searching for products with query: $searchQuery")
        viewModelScope.launch {
            isLoading = true
            try {
                productRepository.searchProducts(searchQuery).collectLatest { result ->
                    result.fold(
                        onSuccess = { searchResults ->
                            products = searchResults
                            Log.d("UserDashboardViewModel", "Search completed: ${searchResults.size} products found")
                        },
                        onFailure = { error ->
                            Log.e("UserDashboardViewModel", "Search failed: ${error.message}", error)
                            products = emptyList()
                        }
                    )
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("UserDashboardViewModel", "Search failed with exception: ${e.message}", e)
                products = emptyList()
                isLoading = false
            }
        }
    }

    /**
     * Memuat semua produk dari repository dan mereset filter.
     */
    fun loadProducts(forceRefresh: Boolean = false) {
        Log.d("UserDashboardViewModel", "Loading all products (forceRefresh: $forceRefresh)")
        selectedCategory = "All Categories"
        loadProductsWithRepository(forceRefresh)
    }

    /**
     * FUNGSI BARU: Memfilter produk berdasarkan kategori yang dipilih.
     */
    fun filterProductsByCategory(category: String) {
        selectedCategory = category
        searchQuery = "" // Hapus query pencarian

        if (category == "All Categories") {
            loadProducts(forceRefresh = true)
            return
        }

        Log.d("UserDashboardViewModel", "Filtering products by category: $category")
        viewModelScope.launch {
            isLoading = true
            try {
                // Panggil repository untuk mendapatkan produk berdasarkan kategori
                productRepository.getProductsByCategory(category).collectLatest { result ->
                    result.fold(
                        onSuccess = { filteredProducts ->
                            products = filteredProducts
                            Log.d("UserDashboardViewModel", "Filter completed: ${filteredProducts.size} products found for '$category'")
                        },
                        onFailure = { error ->
                            Log.e("UserDashboardViewModel", "Failed to filter products from repo: ${error.message}", error)
                            products = emptyList()
                        }
                    )
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("UserDashboardViewModel", "Filtering failed with exception: ${e.message}", e)
                products = emptyList()
                isLoading = false
            }
        }
    }

    private fun loadProductsWithRepository(forceRefresh: Boolean) {
        viewModelScope.launch {
            isLoading = true
            try {
                productRepository.getAllProducts(forceRefresh = forceRefresh).collectLatest { result ->
                    result.fold(
                        onSuccess = { productWithPagination ->
                            products = productWithPagination.products
                            Log.d("UserDashboardViewModel", "Products loaded: ${productWithPagination.products.size}")
                        },
                        onFailure = { error ->
                            Log.e("UserDashboardViewModel", "Failed to load products: ${error.message}", error)
                            products = emptyList()
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
        // TODO: Implement buy logic
    }

    fun chatWithSeller(sellerId: String) {
        Log.d("UserDashboardViewModel", "Chat with seller: $sellerId")
        // TODO: Implement chat logic
    }

    fun logout() {
        Log.d("UserDashboardViewModel", "Logging out user")
        auth.signOut()
        sessionManager.clearToken()

        // Hapus data yang di-cache di ViewModel untuk memastikan UI bersih
        products = emptyList()
        searchQuery = ""
        userInitials = "U"
        selectedCategory = "All Categories"
    }
}