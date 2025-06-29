package com.example.projectmdp.ui.module.UserDashboard

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmdp.data.repository.ProductRepository
import com.example.projectmdp.data.source.dataclass.Product
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class UserDashboardViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    var searchQuery by mutableStateOf("")
        private set
    var products by mutableStateOf<List<Product>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var userInitials by mutableStateOf("U")
        private set
    var selectedCategory by mutableStateOf<String?>(null)
        private set

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    init {
        loadUserInitials()
        loadProducts(forceRefresh = true)
    }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        Log.d("UserDashboardViewModel", "Query changed: $query")
        if (query.isEmpty()) {
            // Jika query kosong, muat semua produk atau produk yang terakhir difilter
            if (selectedCategory != null && selectedCategory != "All Categories") {
                filterProductsByCategory(selectedCategory!!)
            } else {
                loadProducts(forceRefresh = true)
            }
        } else {
            searchProducts()
        }
    }

    fun searchProducts() {
        if (searchQuery.isBlank()) {
            loadProducts(forceRefresh = true)
            return
        }
        Log.d("UserDashboardViewModel", "Searching for products with query: $searchQuery")
        searchProductsWithRepository()
    }

    private fun searchProductsWithRepository() {
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

    fun loadProducts(forceRefresh: Boolean = false) {
        Log.d("UserDashboardViewModel", "Loading all products (forceRefresh: $forceRefresh)")
        selectedCategory = "All Categories" // Set kategori saat memuat semua
        loadProductsWithRepository(forceRefresh)
    }

    // --- INI FUNGSI YANG PERLU ANDA TAMBAHKAN ATAU PASTIKAN ADA ---
    fun filterProductsByCategory(category: String) {
        selectedCategory = category // Simpan kategori yang dipilih
        // Jika "All Categories" dipilih, muat semua produk.
        if (category == "All Categories") {
            loadProducts(forceRefresh = true)
            return
        }

        // Hapus query pencarian untuk menghindari konflik
        searchQuery = ""

        Log.d("UserDashboardViewModel", "Filtering products by category: $category")
        viewModelScope.launch {
            isLoading = true
            try {
                // Panggil fungsi repository yang benar (yang memanggil DAO)
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
                // Ganti dengan implementasi Anda yang sebenarnya, misalnya:
                productRepository.getAllProducts(forceRefresh = forceRefresh).collectLatest { result ->
                    result.fold(
                        onSuccess = { allProducts -> // Asumsi ini mengembalikan List<Product>
                            products = allProducts
                            Log.d("UserDashboardViewModel", "Products loaded: ${products.size}")
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
    }

    fun chatWithSeller(sellerId: String) {
        Log.d("UserDashboardViewModel", "Chat with seller: $sellerId")
    }
}