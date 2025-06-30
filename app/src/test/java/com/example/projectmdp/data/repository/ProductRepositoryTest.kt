package com.example.projectmdp.data.repository

import android.net.Uri
import com.example.projectmdp.data.source.dataclass.Product
import com.example.projectmdp.data.source.local.dao.ProductDao
import com.example.projectmdp.data.source.local.entity.ProductEntity
import com.example.projectmdp.data.source.remote.ProductApi
import com.example.projectmdp.data.source.response.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
@RunWith(MockitoJUnitRunner::class)
class ProductRepositoryTest {

    @Mock
    private lateinit var productDao: ProductDao

    @Mock
    private lateinit var productApi: ProductApi

    @Mock
    private lateinit var mockUri: Uri

    private lateinit var productRepository: ProductRepository

    // Sample data untuk testing
    private val sampleProduct = Product(
        product_id = "1",
        name = "Test Product",
        price = 100.0,
        description = "Test Description",
        category = "Electronics",
        image = "test_image.jpg",
        user_id = "user1",
        created_at = "2024-01-01",
        deleted_at = null
    )

    private val sampleProductEntity = ProductEntity(
        product_id = "1",
        name = "Test Product",
        price = 100.0,
        description = "Test Description",
        category = "Electronics",
        image = "test_image.jpg",
        user_id = "user1",
        created_at = "2024-01-01",
        deleted_at = null
    )

    private val sampleProductResponse = com.example.projectmdp.data.source.response.Product(
        product_id = "1",
        name = "Test Product",
        price = 100.0,
        description = "Test Description",
        category = "Electronics",
        image = "test_image.jpg",
        user_id = "user1",
        created_at = "2024-01-01",
        deleted_at = null
    )

    private val samplePagination = PaginationInfo(
        currentPage = 1,
        totalPages = 1,
        totalProducts = 1,
        hasNext = false,
        hasPrev = false,
        limit = 10
    )

    @Before
    fun setup() {
        productRepository = ProductRepository(productDao, productApi)
    }
//    @Test
    fun `getAllProducts returns cached data first then remote data`() = runTest {
        // Arrange
        val cachedProducts = listOf(sampleProductEntity)
        val remoteResponse = GetAllProductsData(
            products = listOf(sampleProductResponse),
            pagination = samplePagination
        )
        val apiResponse = ApiResponse<GetAllProductsData>(
            status = 200,
            message = "Success",
            data = remoteResponse,
            error = null
        )

        whenever(productDao.getAllProducts()).thenReturn(cachedProducts)
        whenever(productApi.getAllProducts(1, 10)).thenReturn(apiResponse)

        // Act
        val result = productRepository.getAllProducts().first()

        // Assert
        assertTrue("Should return success", result.isSuccess)
        val productWithPagination = result.getOrNull()
        assertNotNull("Result should not be null", productWithPagination)
        assertEquals("Should have 1 product", 1, productWithPagination!!.products.size)
        assertEquals("Product name should match", "Test Product", productWithPagination.products[0].name)

        // Verify DAO interactions
        verify(productDao).getAllProducts()
        verify(productDao).clearAllProducts()
        verify(productDao).insertAll(any())
    }

    @Test
    fun `getAllProducts returns cached data when remote fails`() = runTest {
        // Arrange
        val cachedProducts = listOf(sampleProductEntity)
        val apiResponse = ApiResponse<GetAllProductsData>(
            status = 500,
            message = "Error",
            data = null,
            error = "Network error"
        )

        whenever(productDao.getAllProducts()).thenReturn(cachedProducts)
        whenever(productApi.getAllProducts(1, 10)).thenReturn(apiResponse)

        // Act
        val result = productRepository.getAllProducts().first()

        // Assert
        assertTrue("Should return success with cached data", result.isSuccess)
        val productWithPagination = result.getOrNull()
        assertNotNull("Result should not be null", productWithPagination)
        assertEquals("Should have cached product", 1, productWithPagination!!.products.size)
    }

    @Test
    fun `getAllProducts returns failure when no cache and remote fails`() = runTest {
        // Arrange
        val apiResponse = ApiResponse<GetAllProductsData>(
            status = 500,
            message = "Error",
            data = null,
            error = "Network error"
        )

        whenever(productDao.getAllProducts()).thenReturn(emptyList())
        whenever(productApi.getAllProducts(1, 10)).thenReturn(apiResponse)

        // Act
        val result = productRepository.getAllProducts().first()

        // Assert
        assertTrue("Should return failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Exception should not be null", exception)
        assertTrue("Error message should contain failed to fetch",
            exception!!.message!!.contains("Failed to fetch products"))
    }
//    @Test
    fun `getProductById returns cached then remote data`() = runTest {
        // Arrange
        val productId = "1"
        val remoteResponse = GetProductData(product = sampleProductResponse)
        val apiResponse = ApiResponse<GetProductData>(
            status = 200,
            message = "Success",
            data = remoteResponse,
            error = null
        )

        whenever(productDao.getProductById(productId)).thenReturn(sampleProductEntity)
        whenever(productApi.getProductById(productId)).thenReturn(apiResponse)

        // Act
        val result = productRepository.getProductById(productId).first()

        // Assert
        assertTrue("Should return success", result.isSuccess)
        val product = result.getOrNull()
        assertNotNull("Product should not be null", product)
        assertEquals("Product ID should match", productId, product!!.product_id)
        assertEquals("Product name should match", "Test Product", product.name)

        // Verify interactions
        verify(productDao, atLeastOnce()).getProductById(productId)
        verify(productDao).insert(any<ProductEntity>())
    }

    @Test
    fun `getProductById returns failure when product not found`() = runTest {
        // Arrange
        val productId = "999"
        val apiResponse = ApiResponse<GetProductData>(
            status = 404,
            message = "Not Found",
            data = null,
            error = "Product not found"
        )

        whenever(productDao.getProductById(productId)).thenReturn(null)
        whenever(productApi.getProductById(productId)).thenReturn(apiResponse)

        // Act
        val result = productRepository.getProductById(productId).first()

        // Assert
        assertTrue("Should return failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Exception should not be null", exception)
        assertTrue("Error message should contain not found",
            exception!!.message!!.contains("Product not found"))
    }
//    @Test
    fun `addProduct creates product successfully`() = runTest {
        // Arrange
        val name = "New Product"
        val description = "New Description"
        val price = 150.0
        val category = "Books"
        whenever(productDao.insert(any())).thenReturn(Unit)
        val newProductResponse = sampleProductResponse.copy(
            name = name,
            description = description,
            price = price,
            category = category
        )
        val remoteResponse = AddProductData(product = newProductResponse)
        val apiResponse = ApiResponse<AddProductData>(
            status = 201,
            message = "Success",
            data = remoteResponse,
            error = null
        )

        whenever(productApi.addProduct(any(), any(), any(), any(), any())).thenReturn(apiResponse)

        // Act
        val result = productRepository.addProduct(name, description, price, category, null).first()

        // Assert
        assertTrue("Should return success", result.isSuccess)
        val product = result.getOrNull()
        assertNotNull("Product should not be null", product)
        assertEquals("Product name should match", name, product!!.name)
        assertEquals("Product price should match", price, product.price, 0.01)

        // Verify DAO insert was called
        verify(productDao).insert(any<ProductEntity>())
    }

    @Test
    fun `addProduct returns failure when API fails`() = runTest {
        // Arrange
        val apiResponse = ApiResponse<AddProductData>(
            status = 400,
            message = "Error",
            data = null,
            error = "Validation failed"
        )

        whenever(productApi.addProduct(any(), any(), any(), any(), any())).thenReturn(apiResponse)

        // Act
        val result = productRepository.addProduct("Name", "Desc", 100.0, "Cat", null).first()

        // Assert
        assertTrue("Should return failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Exception should not be null", exception)
        assertTrue("Error message should contain failed to create",
            exception!!.message!!.contains("Failed to create product"))

        // Verify DAO insert was NOT called
        verify(productDao, never()).insert(any<ProductEntity>())
    }
//    @Test
    fun `updateProduct updates product successfully`() = runTest {
        // Arrange
        val productId = "1"
        val newName = "Updated Product"
        val newPrice = 200.0

        val updatedProductResponse = sampleProductResponse.copy(
            name = newName,
            price = newPrice
        )
        val remoteResponse = UpdateProductData(product = updatedProductResponse)
        val apiResponse = ApiResponse<UpdateProductData>(
            status = 200,
            message = "Success",
            data = remoteResponse,
            error = null
        )

        whenever(productApi.updateProduct(any(), any(), any(), any(), any(), any())).thenReturn(apiResponse)

        // Act
        val result = productRepository.updateProduct(productId, newName, null, newPrice, null, null).first()

        // Assert
        assertTrue("Should return success", result.isSuccess)
        val product = result.getOrNull()
        assertNotNull("Product should not be null", product)
        assertEquals("Product name should be updated", newName, product!!.name)
        assertEquals("Product price should be updated", newPrice, product.price, 0.01)

        // Verify DAO update was called
        verify(productDao).update(any<ProductEntity>())
    }

    @Test
    fun `deleteProduct deletes product successfully`() = runTest {
        // Arrange
        val productId = "1"
        val apiResponse = ApiResponse<DeleteProductData>(
            status = 200,
            message = "Success",
            data = DeleteProductData(success = true),
            error = null
        )

        whenever(productApi.deleteProduct(productId)).thenReturn(apiResponse)

        // Act
        val result = productRepository.deleteProduct(productId).first()

        // Assert
        assertTrue("Should return success", result.isSuccess)
        val isDeleted = result.getOrNull()
        assertNotNull("Result should not be null", isDeleted)
        assertTrue("Product should be deleted", isDeleted!!)

        // Verify DAO markAsDeleted was called
        verify(productDao).markAsDeleted(eq(productId), any())
    }
//    @Test
    fun `searchProducts returns cached and remote results`() = runTest {
        // Arrange
        val searchName = "Test"
        val cachedProducts = listOf(sampleProductEntity)
        val remoteResponse = SearchProductData(product = listOf(sampleProductResponse))
        val apiResponse = ApiResponse<SearchProductData>(
            status = 200,
            message = "Success",
            data = remoteResponse,
            error = null
        )

        whenever(productDao.searchProductsByName(searchName)).thenReturn(cachedProducts)
        whenever(productApi.getProductByName(searchName)).thenReturn(apiResponse)

        // Act
        val result = productRepository.searchProducts(searchName).first()

        // Assert
        assertTrue("Should return success", result.isSuccess)
        val products = result.getOrNull()
        assertNotNull("Products should not be null", products)
        assertTrue("Should have products", products!!.isNotEmpty())

        // Verify interactions
        verify(productDao).searchProductsByName(searchName)
        verify(productDao).insertAll(any())
    }

    @Test
    fun `searchProducts returns cached results when remote fails`() = runTest {
        // Arrange
        val searchName = "Test"
        val cachedProducts = listOf(sampleProductEntity)
        val apiResponse = ApiResponse<SearchProductData>(
            status = 500,
            message = "Error",
            data = null,
            error = "Search failed"
        )

        whenever(productDao.searchProductsByName(searchName)).thenReturn(cachedProducts)
        whenever(productApi.getProductByName(searchName)).thenReturn(apiResponse)

        // Act
        val result = productRepository.searchProducts(searchName).first()

        // Assert
        assertTrue("Should return success with cached data", result.isSuccess)
        val products = result.getOrNull()
        assertNotNull("Products should not be null", products)
        assertEquals("Should have cached product", 1, products!!.size)
    }
//    @Test
    fun `getAllProductsLocal returns flow of products`() = runTest {
        // Arrange
        val productEntities = listOf(sampleProductEntity)
        whenever(productDao.getAllProductsFlow()).thenReturn(flowOf(productEntities))

        // Act
        val result = productRepository.getAllProductsLocal().first()

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals("Should have 1 product", 1, result.size)
        assertEquals("Product name should match", "Test Product", result[0].name)
    }

    @Test
    fun `getProductsByCategory returns products for category`() = runTest {
        // Arrange
        val category = "Electronics"
        val productEntities = listOf(sampleProductEntity)
        whenever(productDao.getProductsByCategory(category)).thenReturn(productEntities)

        // Act
        val result = productRepository.getProductsByCategory(category)

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals("Should have 1 product", 1, result.size)
        assertEquals("Product category should match", category, result[0].category)
    }

    @Test
    fun `getProductsByUser returns products for user`() = runTest {
        // Arrange
        val userId = "user1"
        val productEntities = listOf(sampleProductEntity)
        whenever(productDao.getProductsByUser(userId)).thenReturn(productEntities)

        // Act
        val result = productRepository.getProductsByUser(userId)

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals("Should have 1 product", 1, result.size)
        assertEquals("Product user_id should match", userId, result[0].user_id)
    }

    @Test
    fun `clearCache clears all products`() = runTest {
        // Act
        productRepository.clearCache()

        // Assert
        verify(productDao).clearAllProducts()
    }
}
