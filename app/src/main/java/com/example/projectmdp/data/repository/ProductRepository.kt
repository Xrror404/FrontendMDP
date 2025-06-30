package com.example.projectmdp.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.projectmdp.data.source.dataclass.Product
import com.example.projectmdp.data.source.local.dao.ProductDao
import com.example.projectmdp.data.source.remote.RetrofitInstance
import com.example.projectmdp.data.source.response.PaginationInfo
import com.example.projectmdp.data.source.response.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
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
    private val productDao: ProductDao
) {
    private fun createTempFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
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
        if (result == null) {
            result = uri.lastPathSegment
        }
        return result
    }
    // CRUD Operations with Remote + Local

    fun getAllProducts(
        page: Int = 1,
        limit: Int = 10,
        forceRefresh: Boolean = false
    ): Flow<Result<ProductWithPagination>> = flow {
        try {
            if (!forceRefresh) {
                val cachedProducts = productDao.getAllProducts().map { Product.fromProductEntity(it) }
                if (cachedProducts.isNotEmpty()) {
                    val pagination = PaginationInfo(1, 1, cachedProducts.size, false, false, cachedProducts.size)
                    emit(Result.success(ProductWithPagination(cachedProducts, pagination)))
                }
            }

            val response = RetrofitInstance.Productapi.getAllProducts(page, limit)
            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val products = responseData.products.map { it.toProduct() }
                    val productEntities = products.map { it.toProductEntity() }
                    if (page == 1) {
                        productDao.clearAllProducts()
                    }
                    productDao.insertAll(productEntities)
                    val productWithPagination = ProductWithPagination(products, responseData.pagination)
                    emit(Result.success(productWithPagination))
                } ?: emit(Result.failure(Exception("No data received")))
            } else {
                val cachedProducts = productDao.getAllProducts().map { Product.fromProductEntity(it) }
                if (cachedProducts.isNotEmpty()) {
                    val pagination = PaginationInfo(1, 1, cachedProducts.size, false, false, cachedProducts.size)
                    emit(Result.success(ProductWithPagination(cachedProducts, pagination)))
                } else {
                    emit(Result.failure(Exception(response.error ?: "Failed to fetch products")))
                }
            }
        } catch (e: Exception) {
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

    fun getProductById(id: String): Flow<Result<Product>> = flow {
        try {
            productDao.getProductById(id)?.let { cachedProduct ->
                emit(Result.success(Product.fromProductEntity(cachedProduct)))
            }

            val response = RetrofitInstance.Productapi.getProductById(id)
            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val product = responseData.product.toProduct()
                    productDao.insert(product.toProductEntity())
                    emit(Result.success(product))
                } ?: emit(Result.failure(Exception("Product not found")))
            } else {
                productDao.getProductById(id) ?: emit(Result.failure(Exception(response.error ?: "Product not found")))
            }
        } catch (e: Exception) {
            productDao.getProductById(id)?.let { cachedProduct ->
                emit(Result.success(Product.fromProductEntity(cachedProduct)))
            } ?: emit(Result.failure(e))
        }
    }

    fun addProduct(
        context: Context,
        name: String,
        description: String,
        price: Double,
        category: String,
        imageUri: Uri?
    ): Flow<Result<Product>> = flow {
        var tempFile: File? = null
        try {
            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val priceBody = price.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())

            val imagePart = imageUri?.let { uri ->
                tempFile = createTempFileFromUri(context, uri)
                tempFile?.let { file ->
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val fileName = getFileName(context, uri) ?: "product_image.jpg"
                    MultipartBody.Part.createFormData("image", fileName, requestFile)
                }
            }

            val response = RetrofitInstance.Productapi.addProduct(nameBody, descriptionBody, priceBody, categoryBody, imagePart)

            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val product = responseData.product.toProduct()
                    productDao.insert(product.toProductEntity())
                    emit(Result.success(product))
                } ?: emit(Result.failure(Exception("Failed to create product: No data received.")))
            } else {
                emit(Result.failure(Exception(response.error ?: "Failed to create product.")))
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error adding product: ${e.message}", e)
            emit(Result.failure(e))
        } finally {
            tempFile?.delete()
        }
    }

    fun updateProduct(
        context: Context, // Menerima Context
        productId: String,
        name: String, // Menggunakan non-nullable untuk konsistensi
        description: String,
        price: Double, // Menerima Double
        category: String,
        imageUri: Uri?
    ): Flow<Result<Product>> = flow {
        var tempFile: File? = null
        try {
            // Konversi ke RequestBody di dalam repository
            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val priceBody = price.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())

            val imagePart = imageUri?.let { uri ->
                tempFile = createTempFileFromUri(context, uri)
                tempFile?.let { file ->
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val fileName = getFileName(context, uri) ?: "product_image.jpg"
                    MultipartBody.Part.createFormData("image", fileName, requestFile)
                }
            }

            // Memanggil API dengan parameter yang sudah di-format
            val response = RetrofitInstance.Productapi.updateProduct(
                id = productId, // Pastikan nama parameter di API cocok
                name = nameBody,
                description = descriptionBody,
                price = priceBody,
                category = categoryBody,
                image = imagePart
            )

            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val product = responseData.product.toProduct()
                    productDao.update(product.toProductEntity()) // Update cache lokal
                    emit(Result.success(product))
                } ?: emit(Result.failure(Exception("Failed to update product: No data received.")))
            } else {
                emit(Result.failure(Exception(response.error ?: "Failed to update product.")))
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error updating product: ${e.message}", e)
            emit(Result.failure(e))
        } finally {
            tempFile?.delete() // Selalu hapus file sementara
        }
    }

    fun deleteProduct(id: String): Flow<Result<Boolean>> = flow {
        try {
            val response = RetrofitInstance.Productapi.deleteProduct(id)
            if (response.isSuccess()) {
                productDao.markAsDeleted(id, System.currentTimeMillis().toString())
                emit(Result.success(true))
            } else {
                emit(Result.failure(Exception(response.error ?: "Failed to delete product")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun searchProducts(name: String): Flow<Result<List<Product>>> = flow {
        try {
            val cachedResults = productDao.searchProductsByName("%$name%").map { Product.fromProductEntity(it) }
            emit(Result.success(cachedResults))

            val response = RetrofitInstance.Productapi.getProductByName(name)
            if (response.isSuccess()) {
                response.data?.let { responseData ->
                    val remoteProducts = responseData.product.map { it.toProduct() }
                    productDao.insertAll(remoteProducts.map { it.toProductEntity() })
                    emit(Result.success(remoteProducts))
                }
            }
        } catch (e: Exception) {
            // Error ditangani dengan hanya menampilkan data cache jika ada
            Log.e("ProductRepository", "Search failed, relying on cache.", e)
        }
    }

    // Local-only operations for offline support
    fun getAllProductsLocal(): Flow<List<Product>> {
        return productDao.getAllProductsFlow().map { entities ->
            entities.map { Product.fromProductEntity(it) }
        }
    }

    // --- FUNGSI BARU UNTUK FILTER, MENGEMBALIKAN FLOW ---
    /**
     * Mendapatkan produk berdasarkan kategori dari cache lokal.
     * Mengembalikan Flow untuk diobservasi oleh ViewModel.
     */
    fun getProductsByCategory(category: String): Flow<Result<List<Product>>> = flow {
        try {
            val entities = productDao.getProductsByCategory(category)
            val products = entities.map { Product.fromProductEntity(it) }
            emit(Result.success(products))
        } catch (e: Exception) {
            Log.e("ProductRepository", "Error getting products by category from DAO", e)
            emit(Result.failure(e))
        }
    }

    // --- FUNGSI LAMA UNTUK KATEGORI, TIPE SUSPEND (DIPERTAHANKAN) ---
    /**
     * Mendapatkan produk berdasarkan kategori dari cache lokal.
     * Fungsi suspend untuk penggunaan satu kali.
     */
    suspend fun getProductsByCategorySuspend(category: String): List<Product> {
        return productDao.getProductsByCategory(category).map { Product.fromProductEntity(it) }
    }


    suspend fun getProductsByUser(userId: String): List<Product> {
        return productDao.getProductsByUser(userId).map { Product.fromProductEntity(it) }
    }

    suspend fun clearCache() {
        productDao.clearAllProducts()
    }
}