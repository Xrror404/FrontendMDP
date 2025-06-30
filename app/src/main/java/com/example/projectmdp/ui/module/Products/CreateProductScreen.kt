package com.example.projectmdp.ui.module.Products

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel // Use hiltViewModel consistently
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.projectmdp.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.material.icons.filled.AddCircle
import androidx.navigation.compose.currentBackStackEntryAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProductScreen(
    viewModel: ProductViewModel = hiltViewModel(), // Changed to hiltViewModel()
    navController: NavController,
    productId: String? = null // ADDED: Accept nullable productId for update mode
) {
    // State for input fields
    var productName by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var productImageUri by remember { mutableStateOf<Uri?>(null) } // Local state for selected new image

    // Observe ViewModel states
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val productCreationSuccess by viewModel.productCreationSuccess.observeAsState()
    val productUpdateSuccess by viewModel.productUpdateSuccess.observeAsState() // Observe update success
    val selectedProduct by viewModel.selectedProduct.observeAsState() // Observe product for pre-filling fields

    val context: Context = LocalContext.current

    // Launcher for image picking
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        productImageUri = uri // Update local state for new image URI
    }

    // Effect to load product data if productId is provided (for update mode)
    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.fetchProductById(productId)
        } else {
            // Clear fields if navigating from update mode to add mode
            productName = ""
            productDescription = ""
            productPrice = ""
            productCategory = ""
            productImageUri = null
            viewModel.setSelectedProduct(null) // Clear selected product in ViewModel
        }
    }

    // Effect to populate fields once selectedProduct is loaded for update
    LaunchedEffect(selectedProduct) {
        selectedProduct?.let { product ->
            // Only populate fields if this is an update scenario AND the loaded product matches the ID
            if (productId != null && product.product_id == productId) {
                productName = product.name
                productDescription = product.description ?: ""
                productPrice = product.price.toString()
                productCategory = product.category
                // Set image URI if exists, otherwise it will remain null (showing placeholder)
                productImageUri = product.image?.let { Uri.parse(it) }
            }
        }
    }

    // Effect to observe product creation/update success and navigate back
    LaunchedEffect(productCreationSuccess, productUpdateSuccess) { // Observe both success flags
        if (productCreationSuccess == true || productUpdateSuccess == true) {
            val operationType = if (productId == null) "created" else "updated"
            Toast.makeText(context, "Product $operationType successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetProductCreationStatus() // Resets both flags now if using a combined method

            // Signal to previous screen (UserDashboardScreen) to refresh
            navController.previousBackStackEntry?.savedStateHandle?.set("shouldRefreshDashboard", true)

            navController.popBackStack() // Navigate back to previous screen
        }
    }

    // Observe error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message: String ->
            Toast.makeText(context, message as CharSequence, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage() // Clear error message after displaying
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId == null) "Add New Product" else "Update Product") }, // Dynamic title
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product Image Input
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable(enabled = !isLoading) { imagePickerLauncher.launch("image/*") }
                    .clip(RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (productImageUri != null) {
                    AsyncImage(
                        model = productImageUri,
                        contentDescription = "Selected product image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.AddCircle,
                            contentDescription = "Add Photo",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap to select image",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Product Name
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Product Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )

            // Product Description
            OutlinedTextField(
                value = productDescription,
                onValueChange = { productDescription = it },
                label = { Text("Product Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                maxLines = 5,
                enabled = !isLoading
            )

            // Product Price
            OutlinedTextField(
                value = productPrice,
                onValueChange = { newValue ->
                    val filteredValue = newValue.filter { it.isDigit() || it == '.' }
                    if (filteredValue.count { it == '.' } <= 1) {
                        productPrice = filteredValue
                    }
                },
                label = { Text("Price (e.g., 150000.00)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !isLoading
            )

            // Product Category
            OutlinedTextField(
                value = productCategory,
                onValueChange = { productCategory = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Add/Update Product Button
            Button(
                onClick = {
                    val priceDouble = productPrice.toDoubleOrNull()
                    if (productName.isBlank() || productDescription.isBlank() || priceDouble == null || productCategory.isBlank() || productImageUri == null) {
                        Toast.makeText(context, "Please fill all fields and select an image.", Toast.LENGTH_SHORT).show()
                    } else {
                        if (productId == null) {
                            // ADD PRODUCT logic
                            viewModel.createProduct(
                                productName,
                                productDescription,
                                priceDouble,
                                productCategory,
                                productImageUri
                            )
                        } else {
                            // UPDATE PRODUCT logic
                            viewModel.updateProduct(
                                productId, // Pass the existing product ID
                                productName,
                                productDescription,
                                priceDouble,
                                productCategory,
                                productImageUri // Pass the potentially new image URI
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (productId == null) "Add Product" else "Update Product") // Dynamic button text
                }
            }
        }
    }
}