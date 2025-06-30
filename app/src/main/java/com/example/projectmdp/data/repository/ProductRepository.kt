package com.example.projectmdp.data.repository

import android.content.Context // Added import for Context
import android.net.Uri
import android.provider.OpenableColumns // Added import for OpenableColumns
import android.util.Log
import com.example.projectmdp.data.source.dataclass.Product
import com.example.projectmdp.data.source.local.dao.ProductDao
import com.example.projectmdp.data.source.remote.ProductApi
import com.example.projectmdp.data.source.remote.RetrofitInstance
import com.example.projectmdp.data.source.response.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream // Added import for FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

// Data class untuk pagination
data class ProductWithPagination(
    val products: List<Product>,
    val pagination: PaginationInfo
)

// Extension functions untuk mapping
fun com.example.projectmdp.data.source.response.Product.toProduct(): Product {
    return Product(
        product_id = this.product_id,
        name = this.name,
        price = this.price,
        description = this.description,
        category = this.category,
        image = this.image ?: "",
        user_id = this.user_id,
        created_at = this.created_at,
        deleted_at = this.deleted_at
    )
}

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
//    private val productApi: ProductApi
) {
    private fun createTempFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            // Use a unique name for the temp file to avoid collisions and make it recognizable
            val fileName = getFileName(context, uri)
            val tempFile = File(context.cacheDir, "upload_product_${System.currentTimeMillis()}_${fileName ?: "image"}")
            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e("FileUtil", "Error creating temp file from URI: ${e.message}", e)
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    result = cursor.getString(nameIndex)
                }
            }
        }
        // Fallback to last path segment if content resolver fails or it's a file URI
        if (result == null) {
            result = uri.lastPathSegment
        }
        return result
    }
    // CRUD Operations with Remote + Local

    open suspend fun getAllProducts(
        page: Int = 1,
        limit: Int = 10,
        forceRefresh: Boolean = false
    ): Flow<Result<ProductWithPagination>> = flow {
        try {
            // Emit cached data first for better UX
            if (!forceRefresh) {
                val cachedProducts = productDao.getAllProducts().map { Product.fromProductEntity(it) }
                if (cachedProducts.isNotEmpty()) {
                    val pagination = PaginationInfo(
                        currentPage = 1,
                        totalPages = 1,
                        totalProducts = cachedProducts.size,
                        hasNext = false,
                        hasPrev = false,
                        limit = cachedProducts.size
                    )
                    emit(Result.success(ProductWithPagination(cachedProducts, pagination)))
                }
            }

            // Fetch from remote
            val response = RetrofitInstance.Productapi.getAllProducts(page, limit)
            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val products = responseData.products.map { it.toProduct() }

                    // Cache to local database
                    val productEntities = products.map { it.toProductEntity() }
                    if (page == 1) {
                        productDao.clearAllProducts() // Clear cache for first page
                    }
                    productDao.insertAll(productEntities)

                    val productWithPagination = ProductWithPagination(products, responseData.pagination)
                    emit(Result.success(productWithPagination))
                } ?: emit(Result.failure(Exception("No data received")))
            } else {
                // If remote fails, return cached data if available
                val cachedProducts = productDao.getAllProducts().map { Product.fromProductEntity(it) }
                if (cachedProducts.isNotEmpty()) {
                    val pagination = PaginationInfo(1, 1, cachedProducts.size, false, false, cachedProducts.size)
                    emit(Result.success(ProductWithPagination(cachedProducts, pagination)))
                } else {
                    emit(Result.failure(Exception(response.error ?: "Failed to fetch products")))
                }
            }
        } catch (e: Exception) {
            // On error, try to return cached data
            try {
                val cachedProducts = productDao.getAllProducts().map { Product.fromProductEntity(it) }
                if (cachedProducts.isNotEmpty()) {
                    val pagination = PaginationInfo(1, 1, cachedProducts.size, false, false, cachedProducts.size)
                    emit(Result.success(ProductWithPagination(cachedProducts, pagination)))
                } else {
                    emit(Result.failure(e))
                }
            } catch (cacheError: Exception) {
                emit(Result.failure(e))
            }
        }
    }

    suspend fun getProductById(id: String): Flow<Result<Product>> = flow {
        try {
            // Try to get from cache first
            productDao.getProductById(id)?.let { cachedProduct ->
                emit(Result.success(Product.fromProductEntity(cachedProduct)))
            }

            // Fetch from remote
            val response = RetrofitInstance.Productapi.getProductById(id)
            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val product = responseData.product.toProduct()
                    
                    // Update cache
                    productDao.insert(product.toProductEntity())
                    
                    emit(Result.success(product))
                } ?: emit(Result.failure(Exception("Product not found")))
            } else {
                // If remote fails but we have cache, that's OK
                productDao.getProductById(id)?.let { cachedProduct ->
                    // Already emitted above, no need to emit again
                } ?: emit(Result.failure(Exception(response.error ?: "Product not found")))
            }
        } catch (e: Exception) {
            // Try cache on error
            productDao.getProductById(id)?.let { cachedProduct ->
                emit(Result.success(Product.fromProductEntity(cachedProduct)))
            } ?: emit(Result.failure(e))
        }
    }

    suspend fun addProduct(
        context: Context, // Required for Uri processing
        name: String,
        description: String,
        price: Double,
        category: String,
        imageUri: Uri?
    ): Flow<Result<Product>> = flow {
        // Declare tempFile here so it's accessible in finally block
        var tempFile: File? = null

        try {
            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val priceBody = price.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())

            var imagePart: MultipartBody.Part? = null
            imageUri?.let { uri ->
                // Call the private helper function
                tempFile = createTempFileFromUri(context, uri)

                if (tempFile != null && tempFile!!.exists()) { // Use !! after null check
                    val requestFile = tempFile!!.asRequestBody("image/*".toMediaTypeOrNull())
                    val fileName = getFileName(context, uri) ?: "product_image.jpg"
                    imagePart = MultipartBody.Part.createFormData("image", fileName, requestFile)
                } else {
                    emit(Result.failure(Exception("Failed to prepare image file for upload. Please try again.")))
                    return@flow // Exit flow early if file cannot be prepared
                }
            }

            val response = RetrofitInstance.Productapi.addProduct(
                nameBody, descriptionBody, priceBody, categoryBody, imagePart
            )

            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val product = responseData.product.toProduct()

                    // Cache to local
                    productDao.insert(product.toProductEntity())

                    emit(Result.success(product))
                } ?: emit(Result.failure(Exception("Failed to create product: No product data received from backend.")))
            } else {
                emit(Result.failure(Exception(response.error ?: "Failed to create product: Unknown backend error.")))
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error adding product: ${e.message}", e)
            emit(Result.failure(e))
        } finally {
            // Ensure temporary file is deleted whether success or failure
            tempFile?.delete()
        }
    }



    suspend fun updateProduct(
        id: String,
        name: String? = null,
        description: String? = null,
        price: Double? = null,
        category: String? = null,
        imageUri: Uri? = null
    ): Flow<Result<Product>> = flow {
        try {
            val nameBody = name?.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody = description?.toRequestBody("text/plain".toMediaTypeOrNull())
            val priceBody = price?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryBody = category?.toRequestBody("text/plain".toMediaTypeOrNull())
            
            var imagePart: MultipartBody.Part? = null
            imageUri?.let { uri ->
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                }
            }

            val response = RetrofitInstance.Productapi.updateProduct(
                id, nameBody, descriptionBody, priceBody, categoryBody, imagePart
            )
            
            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val product = responseData.product.toProduct()
                    
                    // Update cache
                    productDao.update(product.toProductEntity())
                    
                    emit(Result.success(product))
                } ?: emit(Result.failure(Exception("Failed to update product")))
            } else {
                emit(Result.failure(Exception(response.error ?: "Failed to update product")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun deleteProduct(id: String): Flow<Result<Boolean>> = flow {
        try {
            val response = RetrofitInstance.Productapi.deleteProduct(id)
            if (response.isSuccess()) {
                // Remove from cache
                productDao.markAsDeleted(id, System.currentTimeMillis().toString())
                emit(Result.success(true))
            } else {
                emit(Result.failure(Exception(response.error ?: "Failed to delete product")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun searchProducts(name: String): Flow<Result<List<Product>>> = flow {
        emit(Result.success(emptyList())) // Untuk menunjukkan loading

        // Coba search cache dulu
        val cachedResults = productDao.searchProductsByName(name).map { Product.fromProductEntity(it) }
        if (cachedResults.isNotEmpty()) {
            emit(Result.success(cachedResults)) // Pertama: tampilkan cache
        }
        Log.d("SearchDebug", "Searching for: $name")
        Log.d("SearchDebug", "Cached: ${cachedResults.size}")
        try {
            // Coba fetch dari remote
            val response = RetrofitInstance.Productapi.getProductByName(name)
            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val products = responseData.product.map { it.toProduct() }

                    // Cache remote result
                    productDao.insertAll(products.map { it.toProductEntity() })

                    Log.d("SearchDebug", "Remote success: ${products.size}")
                    emit(Result.success(products)) // Kedua: update dengan hasil terbaru
                } ?: emit(Result.failure(Exception("No products found")))
            } else {
                // Kalau API gagal dan tidak ada cache
                if (cachedResults.isEmpty()) {
                    emit(Result.failure(Exception(response.error ?: "No products found")))
                }
            }
        } catch (e: Exception) {
            if (cachedResults.isEmpty()) {
                emit(Result.failure(e))
            }
            // Kalau error, dan cache sudah ditampilkan sebelumnya → tidak perlu emit error lagi
        }
    }


    // Local-only operations for offline support
    fun getAllProductsLocal(): Flow<List<Product>> {
        return productDao.getAllProductsFlow().map { entities ->
            entities.map { Product.fromProductEntity(it) }
        }
    }

    suspend fun getProductsByCategory(category: String): List<Product> {
        return productDao.getProductsByCategory(category).map { Product.fromProductEntity(it) }
    }

    suspend fun getProductsByUser(userId: String): List<Product> {
        return productDao.getProductsByUser(userId).map { Product.fromProductEntity(it) }
    }

    suspend fun clearCache() {
        productDao.clearAllProducts()
    }
}