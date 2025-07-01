package com.example.projectmdp.ui.module.Products

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.projectmdp.R
import com.example.projectmdp.data.source.dataclass.Product
import com.example.projectmdp.data.source.dataclass.User
import com.example.projectmdp.navigation.Routes
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    navController: NavController,
    productViewModel: ProductViewModel = hiltViewModel(),
    productId: String
) {
    val product: Product? by productViewModel.selectedProduct.observeAsState()
    val seller: User? by productViewModel.selectedProductSeller.observeAsState()
    val isLoading: Boolean by productViewModel.isLoading.observeAsState(initial = false)
    val errorMessage: String? by productViewModel.errorMessage.observeAsState()
    val context = LocalContext.current
    val currentLoggedInUserId: String? by productViewModel.currentLoggedInUserId.observeAsState()
    val productDeletionSuccess by productViewModel.productDeletionSuccess.observeAsState()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    LaunchedEffect(productId) {
        productViewModel.fetchProductById(productId)
    }

    LaunchedEffect(productDeletionSuccess) {
        if (productDeletionSuccess == true) {
            Toast.makeText(context, "Product deleted successfully!", Toast.LENGTH_SHORT).show()
            navController.previousBackStackEntry?.savedStateHandle?.set("shouldRefreshDashboard", true)
            navController.popBackStack()
            // Consider adding a reset function in ViewModel to prevent this from re-triggering
            // productViewModel.resetDeletionStatus()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            productViewModel.clearErrorMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.name ?: "Loading Product...") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            val currentProduct = product
            // Show bottom bar only when data is loaded and not in a loading state
            if (currentProduct != null && !isLoading) {
                val isMyProduct = currentProduct.user_id == currentLoggedInUserId

                Surface(
                    shadowElevation = 8.dp, // Add a shadow for better separation
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding() // Handles gesture navigation padding
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isMyProduct) {
                            // --- Buttons for Owner ---
                            Button(
                                onClick = { navController.navigate("UPDATE_PRODUCT/$productId") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Update")
                                Spacer(Modifier.width(8.dp))
                                Text("Update", fontSize = 16.sp)
                            }

                            OutlinedButton(
                                onClick = { productViewModel.deleteProduct(currentProduct.product_id) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                                Spacer(Modifier.width(8.dp))
                                Text("Delete", fontSize = 16.sp)
                            }
                        } else {
                            // --- Buttons for Buyer ---
                            Button(
                                onClick = {
                                    navController.navigate(
                                        Routes.midtransRoute(
                                            productId = currentProduct.product_id,
                                            price = currentProduct.price
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Buy Now")
                                Spacer(Modifier.width(8.dp))
                                Text("Buy Now", fontSize = 16.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    seller?.id?.let {
                                        navController.navigate("chat/$it")
                                    } ?: Toast.makeText(context, "Seller information not available.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = "Chat")
                                Spacer(Modifier.width(8.dp))
                                Text("Chat", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValuesFromScaffold ->
        // This Box is the single scrollable container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValuesFromScaffold) // Apply padding from Scaffold
                .verticalScroll(rememberScrollState()), // Main scroll handler
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                errorMessage != null && product == null -> Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                product == null -> if (!isLoading) Text("Product with ID $productId not found.")
                else -> {
                    // Create a stable local copy to avoid smart-cast issues
                    val currentProduct = product

                    // This Column is NOT scrollable itself, it just arranges content vertically
                    Column(
                        modifier = Modifier.fillMaxWidth() // No scroll or size modifiers here
                    ) {
                        // --- PRODUCT IMAGE ---
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(product!!.image)
                                .crossfade(true)
                                .build(),
                            contentDescription = product!!.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.alert_error),
                            placeholder = painterResource(id = R.drawable.landscape_placeholder)
                        )

                        // --- MAIN CONTENT WITH PADDING ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp)
                        ) {
                            // --- PRODUCT NAME & CATEGORY ---
                            Text(
                                text = product!!.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (product!!.hasCategory()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = product!!.category,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))

                            // --- PRODUCT DETAILS SECTION ---
                            Text(
                                text = "Detail Produk",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            product!!.description?.let {
                                DetailItem(icon = Icons.Default.Description, label = "Deskripsi", content = it)
                            }

                            DetailItem(icon = Icons.Default.MonetizationOn, label = "Harga", content = currencyFormat.format(product!!.price))

                            val formattedDate = remember(product!!.created_at) {
                                try {
                                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                                        timeZone = TimeZone.getTimeZone("UTC")
                                    }
                                    parser.parse(product!!.created_at)?.let { date ->
                                        SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(date)
                                    } ?: product!!.created_at
                                } catch (e: Exception) {
                                    product!!.created_at // Fallback
                                }
                            }
                            DetailItem(icon = Icons.Default.CalendarToday, label = "Tanggal Upload", content = formattedDate)

                            // --- VISUAL SEPARATOR ---
                            Divider(modifier = Modifier.padding(vertical = 24.dp))

                            // --- SELLER INFORMATION SECTION ---
                            Text(
                                text = "Informasi Penjual",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            DetailItem(icon = Icons.Default.Person, label = "Penjual", content = seller?.username ?: "Informasi tidak tersedia")
                            DetailItem(icon = Icons.Default.LocationOn, label = "Lokasi", content = seller?.address ?: "Lokasi tidak diketahui")

                            Spacer(modifier = Modifier.height(16.dp)) // Final padding at the bottom
                        }
                    }
                }
            }
        }
    }
}

/**
 * A private helper composable to display a detail item consistently.
 * It includes an icon, a label, and the content.
 */
@Composable
private fun DetailItem(
    icon: ImageVector,
    label: String,
    content: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    Spacer(modifier = Modifier.height(18.dp)) // Spacing after each detail item
}

/**
 * An extension function to check if the product has a non-blank category.
 */
private fun Product.hasCategory(): Boolean {
    return !this.category.isNullOrBlank()
}