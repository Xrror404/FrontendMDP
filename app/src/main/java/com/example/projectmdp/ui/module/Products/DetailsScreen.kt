package com.example.projectmdp.ui.module.Products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape // Required for button shapes
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder // Keep if used elsewhere
import androidx.compose.material.icons.filled.Share // Keep if used elsewhere
import androidx.compose.material.icons.filled.ShoppingCart // <--- ADD THIS IMPORT for Buy Now icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext // <--- ADD THIS IMPORT for Toast/ImageRequest
import androidx.compose.ui.res.painterResource // <--- ADD THIS IMPORT for any custom drawables (like chat icon if not Material)
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // <--- ADD THIS IMPORT for font size if used in buttons
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest // <--- ADD THIS IMPORT for AsyncImage model builder
import com.example.projectmdp.R // <--- ADD THIS IMPORT if you use R.drawable for placeholders/custom icons
import com.example.projectmdp.data.source.dataclass.Product
import com.example.projectmdp.data.source.dataclass.User
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import android.widget.Toast // <--- ADD THIS IMPORT for toast messages
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email

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

    val context = LocalContext.current // Get context for Toast messages
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.name ?: "Loading Product...") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        // --- START: MODIFIED bottomBar ---
        bottomBar = {
            // Check if product is loaded before showing buttons
            if (product != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp) // Padding around the buttons
                        .navigationBarsPadding(), // Account for system navigation bar
                    horizontalArrangement = Arrangement.spacedBy(16.dp), // Space between buttons
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            navController.navigate("TRANSACTION")
                        },
                        modifier = Modifier.weight(1f), // Take equal weight
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Buy Now",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Buy Now", fontSize = 16.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val sellerId = seller?.id
                            if (sellerId != null) {
                               navController.navigate("chat/$sellerId")
                            } else {
                                Toast.makeText(context, "Seller information not available for chat.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f), // Take equal weight
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Chat",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Chat", fontSize = 16.sp)
                    }
                }
            }
        }
    ) { paddingValuesFromScaffold -> // Rename paddingValues to avoid clash with local paddingValues
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValuesFromScaffold) // Use the Scaffold's padding
                .padding(horizontal = 16.dp) // Keep your original horizontal padding inside
                .verticalScroll(rememberScrollState()), // Keep original scroll modifier on content
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                errorMessage != null -> {
                    Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                }
                product == null -> {
                    Text("Product with ID $productId not found.")
                }
                else -> {
                    // This content remains identical to your original ProductDetailsContent logic
                    ProductDetailsContent(product = product!!, seller = seller, currencyFormat = currencyFormat)
                }
            }
        }
    }
}

// ProductDetailsContent Composable (remains completely original from your provided code)
@Composable
private fun ProductDetailsContent(product: Product, seller: User?, currencyFormat: NumberFormat) {
    val context = LocalContext.current // Added context for ImageRequest

    Column(modifier = Modifier.fillMaxSize()) { // Keep fillMaxSize as it's the inner scrollable content
        AsyncImage(
            model = ImageRequest.Builder(context) // Use context for ImageRequest
                .data(product.image)
                .crossfade(true)
                .build(),
            contentDescription = product.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.alert_error), // Assuming these drawables exist
            placeholder = painterResource(id = R.drawable.landscape_placeholder) // Assuming these drawables exist
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = product.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (product.hasCategory()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Description",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        product.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Price: ${currencyFormat.format(product.price)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Handle date formatting
        val formattedDate = remember(product.created_at) {
            try {
                // Assuming product.created_at is in a format like "yyyy-MM-dd'T'HH:mm:ss'Z'" (ISO 8601 UTC)
                // Adjust this SimpleDateFormat pattern to match your backend's string exactly
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                parser.timeZone = java.util.TimeZone.getTimeZone("UTC") // Set UTC timezone for parsing
                val date = parser.parse(product.created_at)
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date) // Format for display
            } catch (e: Exception) {
                e.printStackTrace()
                product.created_at // Fallback to raw string if parsing fails
            }
        }
        Text(
            text = "Created At: $formattedDate",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.weight(1f)) // Original spacer, pushes content up if less than full screen
        Text(
            text = "Seller: ${seller?.username ?: "N/A"}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Location: ${seller?.address ?: "N/A"}", // Assuming seller has an address field
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp)) // Original spacer
    }
}