package com.example.projectmdp.ui.module.UserDashboard

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.res.painterResource
import com.example.projectmdp.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.projectmdp.navigation.Routes
import java.text.NumberFormat
import java.util.Locale
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun UserDashboardScreen(
    viewModel: UserDashboardViewModel = viewModel(),
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val searchQuery = viewModel.searchQuery
    val products = viewModel.products
    val userInitials = viewModel.userInitials
    val isLoading = viewModel.isLoading

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var showProfileMenu by remember { mutableStateOf(false) }

    LaunchedEffect(navBackStackEntry) {
        val shouldRefresh = navBackStackEntry?.savedStateHandle?.get<Boolean>("shouldRefreshDashboard")
        if (shouldRefresh == true) {
            Log.d("UserDashboard", "Received refresh signal. Reloading products...")
            viewModel.loadProducts(forceRefresh = true)
            navBackStackEntry?.savedStateHandle?.remove<Boolean>("shouldRefreshDashboard")
        }
    }
    LaunchedEffect(Unit) { // 'Unit' as a key ensures this block runs once when the Composable first enters the Composition
        Log.d("UserDashboard", "UserDashboardScreen initialized. Loading products initially.")
        viewModel.loadProducts(forceRefresh = true) // Load products immediately
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopBar(
                userInitials = userInitials,
                onProfileClick = { showProfileMenu = true },
                onChatClick = { navController.navigate(Routes.CHAT_LIST)}
            )

            SearchBar(
                searchQuery = searchQuery,
                onSearchChange = { viewModel.onSearchQueryChange(it) },
                onSearchSubmit = {
                    if (searchQuery.isBlank()) {
                        viewModel.loadProducts(forceRefresh = true)
                    } else {
                        viewModel.searchProducts()
                    }
                }
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No products found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(products) { product ->
                        ProductCard(
                            product = product,
                            onProductClick = {
                                val routeToNavigate = Routes.productDetailRoute(product.product_id)
                                Log.d("NavigationDebug", "Attempting to navigate to: $routeToNavigate")
                                navController.navigate(routeToNavigate)
                            },
                            onBuyClick = { viewModel.buyProduct(product) },
                            onChatClick = { viewModel.chatWithSeller(product.user_id) }
                        )
                    }
                }
            }
        }

        if (showProfileMenu) {
            ProfileMenuPopup(
                onDismiss = { showProfileMenu = false },
                onEditProfile = {
                    showProfileMenu = false
                    navController.navigate(Routes.EDIT_PROFILE)
                },
                onTransactionHistory = {
                    showProfileMenu = false
                    navController.navigate(Routes.TRANSACTION_HISTORY)
                },
                onLogout = {
                    showProfileMenu = false
                    viewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.USER_DASHBOARD) { inclusive = true }
                    }
                }
            )
        }

        FloatingActionButton(
            onClick = { navController.navigate(Routes.ADD_PRODUCT) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Product",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun TopBar(
    userInitials: String,
    onProfileClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "WeCycle",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Chat Icon Button
            IconButton(onClick = onChatClick) {
                Icon(
                    painter = painterResource(id = R.drawable.chat_24px), // make sure this exists
                    contentDescription = "Messages",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // User Profile Circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userInitials,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text("Search for products...") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
private fun ProductCard(
    product: com.example.projectmdp.data.source.dataclass.Product,
    onProductClick: () -> Unit,
    onBuyClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProductClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(product.image)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = painterResource(id = R.drawable.alert_error),
                    placeholder = painterResource(id = R.drawable.landscape_placeholder)
                )

                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = currencyFormat.format(product.price),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                product.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
//                    Button(
//                        onClick = onBuyClick,
//                        modifier = Modifier.weight(1f),
//                        shape = RoundedCornerShape(8.dp),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = MaterialTheme.colorScheme.primary
//                        )
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.ShoppingCart,
//                            contentDescription = "Buy",
//                            modifier = Modifier.size(16.dp)
//                        )
//                        Spacer(modifier = Modifier.width(4.dp))
//                        Text("Buy", fontSize = 12.sp)
//                    }

//                    OutlinedButton(
//                        onClick = onChatClick,
//                        modifier = Modifier.weight(1f),
//                        shape = RoundedCornerShape(8.dp),
//                        colors = ButtonDefaults.outlinedButtonColors(
//                            contentColor = MaterialTheme.colorScheme.primary
//                        )
//                    ) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.chat_24px),
//                            contentDescription = "Chat",
//                            modifier = Modifier.size(16.dp)
//                        )
//                        Spacer(modifier = Modifier.width(4.dp))
//                        Text("Chat", fontSize = 12.sp)
//                    }
                }
            }
        }
    }
}

@Composable
fun ProfileMenuPopup(
    onDismiss: () -> Unit,
    onEditProfile: () -> Unit,
    onTransactionHistory: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 16.dp)
                .width(200.dp)
                .clickable { /* Prevent dismissing when clicking inside the card */ },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                TextButton(
                    onClick = onEditProfile,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.person_24px),
//                            contentDescription = "Edit Profile",
//                            modifier = Modifier.size(20.dp)
//                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Edit Profile")
                    }
                }

                TextButton(
                    onClick = onTransactionHistory,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Transaction History",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Transaction History")
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                TextButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.logout_24px),
//                            contentDescription = "Logout",
//                            modifier = Modifier.size(20.dp)
//                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Logout")
                    }
                }
            }
        }
    }
}
