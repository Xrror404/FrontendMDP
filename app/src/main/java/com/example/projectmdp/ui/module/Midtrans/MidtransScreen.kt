package com.example.projectmdp.ui.module.Midtrans

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.projectmdp.R
import com.example.projectmdp.data.source.dataclass.Product
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MidtransScreen(
    navController: NavController,
    viewModel: MidtransViewModel = hiltViewModel(),
    productId: String? = null
) {
    val context = LocalContext.current
    val product by viewModel.selectedProduct.collectAsState()
    val paymentUrl by viewModel.paymentUrl.collectAsState()
    val isLoading by remember { viewModel.isLoading }
    val paymentStatus by viewModel.paymentStatus.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val quantityState = remember { mutableStateOf(1) }
    val totalPrice = remember(product, quantityState.value) {
        product?.price?.times(quantityState.value) ?: 0.0
    }

    // Load product when productId is available
    LaunchedEffect(productId) {
        productId?.let { id ->
            Log.d("MidtransScreen", "Loading product with ID: $id")
            viewModel.loadProduct(id)
        } ?: run {
            Log.e("MidtransScreen", "ProductId is null!")
        }
    }

    // Debug log when product changes
    LaunchedEffect(product) {
        product?.let {
            Log.d("MidtransScreen", "Product loaded: ${it.name}, ID: ${it.product_id}")
        } ?: run {
            Log.d("MidtransScreen", "Product is null")
        }
    }

    // Handle error messages with toast
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    // Navigate back when needed
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { destination ->
            navController.navigate(destination) {
                popUpTo(navController.graph.startDestinationId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment with Midtrans") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                paymentUrl != null -> {
                    // Show WebView for Midtrans payment page
                    MidtransWebView(
                        paymentUrl = paymentUrl!!,
                        onPaymentComplete = { success ->
                            if (success) {
                                viewModel.checkPaymentStatus(paymentUrl!!.substringAfterLast('/'))
                            } else {
                                // Payment failed or canceled
                                viewModel.resetPayment()
                            }
                        }
                    )
                }
                paymentStatus == PaymentStatus.SUCCESS -> {
                    // Show payment success screen
                    PaymentResultScreen(
                        success = true,
                        onClose = {
                            viewModel.resetPayment()
                            navController.popBackStack()
                        }
                    )
                }
                paymentStatus == PaymentStatus.FAILED -> {
                    // Show payment failed screen
                    PaymentResultScreen(
                        success = false,
                        onClose = {
                            viewModel.resetPayment()
                        },
                        onRetry = {
                            viewModel.resetPayment()
                            viewModel.createTransaction(quantityState.value)
                        }
                    )
                }
                product != null -> {
                    // Show product payment details
                    ProductPaymentDetails(
                        product = product!!,
                        quantity = quantityState.value,
                        onQuantityChange = { quantityState.value = it },
                        totalPrice = totalPrice,
                        isLoading = isLoading,
                        onProceedToPayment = {
                            viewModel.createTransaction(quantityState.value)
                        }
                    )
                }
                isLoading -> {
                    // Show loading indicator while fetching product details
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    // Show error message if no product found
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = if (productId != null) {
                                    "Product with ID $productId not found"
                                } else {
                                    "No product ID provided"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                            
                            Button(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.padding(horizontal = 32.dp)
                            ) {
                                Text("Go Back")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductPaymentDetails(
    product: Product,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    totalPrice: Double,
    isLoading: Boolean,
    onProceedToPayment: () -> Unit
) {
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Product Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column {
                // Product Image
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(product.image)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    error = painterResource(id = R.drawable.alert_error),
                    placeholder = painterResource(id = R.drawable.landscape_placeholder)
                )

                // Product Details
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Unit Price: ${currencyFormat.format(product.price)}",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    product.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Quantity Selector
//        Card(
//            modifier = Modifier.fillMaxWidth(),
//            shape = RoundedCornerShape(12.dp),
//            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//        ) {
//            Column(
//                modifier = Modifier.padding(16.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                Text(
//                    text = "Quantity",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.SemiBold
//                )

//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Button(
//                        onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
//                        shape = RoundedCornerShape(8.dp),
//                        modifier = Modifier.width(48.dp),
//                        contentPadding = PaddingValues(0.dp)
//                    ) {
//                        Text("-", fontSize = 20.sp)
//                    }
//
//                    Text(
//                        text = quantity.toString(),
//                        style = MaterialTheme.typography.titleLarge,
//                        fontWeight = FontWeight.Bold
//                    )
//
//                    Button(
//                        onClick = { onQuantityChange(quantity + 1) },
//                        shape = RoundedCornerShape(8.dp),
//                        modifier = Modifier.width(48.dp),
//                        contentPadding = PaddingValues(0.dp)
//                    ) {
//                        Text("+", fontSize = 20.sp)
//                    }
//                }
//            }
//        }

        // Total price section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Payment Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Subtotal (${quantity} ${if (quantity > 1) "items" else "item"})",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = currencyFormat.format(totalPrice),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Payment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = currencyFormat.format(totalPrice),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Information about Midtrans Sandbox
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Midtrans Sandbox Test Environment",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = "This is a test payment using Midtrans Sandbox. No actual charges will be made.",
                    style = MaterialTheme.typography.bodySmall,
                )

                Text(
                    text = "For testing, use these credit card details: \nCard Number: 4811 1111 1111 1114 \nCVV: 123 \nExp: Any future date",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Proceed to payment button
        Button(
            onClick = onProceedToPayment,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    "Proceed to Payment",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MidtransWebView(
    paymentUrl: String,
    onPaymentComplete: (Boolean) -> Unit
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: ""
                        // Check if URL contains success or failure indicators
                        return when {
                            url.contains("transaction_status=settlement") ||
                            url.contains("transaction_status=capture") ||
                            url.contains("status=success") ||
                            url.contains("status_code=200") -> {
                                Log.d("MidtransWebView", "Payment successful: $url")
                                onPaymentComplete(true)
                                true
                            }
                            url.contains("transaction_status=deny") ||
                            url.contains("transaction_status=cancel") ||
                            url.contains("transaction_status=expire") ||
                            url.contains("transaction_status=failure") ||
                            url.contains("status=failed") -> {
                                Log.d("MidtransWebView", "Payment failed: $url")
                                onPaymentComplete(false)
                                true
                            }
                            else -> false // Continue loading the URL
                        }
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        Log.e("MidtransWebView", "WebView error: ${error?.description}")
                        onPaymentComplete(false)
                    }
                }
                loadUrl(paymentUrl)
            }
        },
        update = { webView ->
            webView.loadUrl(paymentUrl)
        }
    )
}

@Composable
fun PaymentResultScreen(
    success: Boolean,
    onClose: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(60.dp))
                .background(if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (success) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (success) "Success" else "Failed",
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (success) "Payment Successful!" else "Payment Failed",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (success)
                "Thank you for your purchase! Your transaction has been completed successfully."
            else
                "We encountered a problem processing your payment. Please try again.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (onRetry != null && !success) {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    "Try Again",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                if (success) "Return to Home" else "Close",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
