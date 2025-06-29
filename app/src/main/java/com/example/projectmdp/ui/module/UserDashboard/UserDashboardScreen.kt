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
import androidx.compose.material.icons.filled.Tune
import com.example.projectmdp.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.projectmdp.data.source.dataclass.Product
import com.example.projectmdp.navigation.Routes
import java.text.NumberFormat
import java.util.*
import androidx.compose.material.icons.filled.ArrowDropDown // <-- TAMBAHKAN IMPORT INI


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboardScreen(
    viewModel: UserDashboardViewModel = viewModel(),
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val searchQuery = viewModel.searchQuery
    val products = viewModel.products
    val userInitials = viewModel.userInitials
    val isLoading = viewModel.isLoading
    val modalBottomSheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(navBackStackEntry) {
        val shouldRefresh = navBackStackEntry?.savedStateHandle?.get<Boolean>("shouldRefreshDashboard")
        if (shouldRefresh == true) {
            Log.d("UserDashboard", "Menerima sinyal refresh. Memuat ulang produk...")
            viewModel.loadProducts(forceRefresh = true)
            navBackStackEntry?.savedStateHandle?.remove<Boolean>("shouldRefreshDashboard")
        }
    }

    // --- PERUBAHAN DIMULAI: Implementasi Modal Bottom Sheet untuk Filter ---
    // Daftar kategori untuk filter, di-remember agar tidak dibuat ulang pada setiap recomposition
    // --- PERUBAHAN DIMULAI: Implementasi Modal Bottom Sheet untuk Filter ---
    // Daftar kategori untuk filter
    val categories = remember {
        listOf("All Categories", "Sword", "Gadget & Technology", "Furniture", "Games", "Books","Other")
    }
    // State untuk mengontrol apakah dropdown kategori terbuka atau tidak
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    // State untuk menyimpan kategori yang dipilih di UI.
    var selectedCategory by remember { mutableStateOf(categories.first()) }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = modalBottomSheetState
        ) {
            // Konten untuk filter
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Filter Options", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(24.dp))

                // === Dropdown/Combobox untuk Kategori (IMPLEMENTASI YANG DIPERBAIKI) ===
                Text(
                    text = "Filter by Category",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Gunakan Box untuk membungkus TextField dan DropdownMenu
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // TextField ini hanya untuk tampilan, tidak bisa diedit langsung
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = { },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true, // Penting!
                        trailingIcon = {
                            // Icon yang menunjukkan ini adalah dropdown
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown, // atau ikon lain yang sesuai
                                contentDescription = "Open categories",
                                Modifier.clickable { isCategoryDropdownExpanded = true }
                            )
                        }
                    )

                    // DropdownMenu standar yang akan muncul di atas segalanya
                    DropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false },
                        // Sesuaikan lebar menu agar sama dengan TextField
                        modifier = Modifier.fillMaxWidth(0.9f) // Sesuaikan fraksi jika perlu
                    ) {
                        categories.forEach { categoryOption ->
                            DropdownMenuItem(
                                text = { Text(categoryOption) },
                                onClick = {
                                    selectedCategory = categoryOption
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }

                    // Tambahkan Box transparan di atas TextField agar seluruh area bisa diklik
                    // untuk membuka menu. Ini adalah trik UX yang bagus.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Transparent)
                            .clickable(
                                onClick = { isCategoryDropdownExpanded = true },
                            )
                    )
                }
                // === Akhir Dropdown ===

                Spacer(modifier = Modifier.height(28.dp))

                // Tombol untuk menerapkan dan membersihkan filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Panggil loadProducts untuk menghapus filter dan menampilkan semua
                            viewModel.loadProducts(forceRefresh = true)
                            showBottomSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Clear")
                    }
                    Button(
                        onClick = {
                            // --- PERUBAHAN UTAMA DI SINI ---
                            // Panggil fungsi filter kategori yang baru di ViewModel
                            viewModel.filterProductsByCategory(selectedCategory)
                            showBottomSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Apply")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    // --- PERUBAHAN SELESAI ---

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            TopBar(
                userInitials = userInitials,
                onProfileClick = { /* Handle profile click */ }
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
                },
                onFilterClick = { showBottomSheet = true }
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                ) {
                    items(products) { product ->
                        ProductCard(
                            product = product,
                            onProductClick = {
                                val routeToNavigate = Routes.productDetailRoute(product.product_id)
                                navController.navigate(routeToNavigate)
                            },
                            onBuyClick = { viewModel.buyProduct(product) },
                            onChatClick = { viewModel.chatWithSeller(product.user_id) }
                        )
                    }
                }
            }
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
    onProfileClick: () -> Unit
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

@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search for products...") },
            modifier = Modifier.weight(1f),
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

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onFilterClick,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Filter Products",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
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
                    Button(
                        onClick = onBuyClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Buy",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buy", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onChatClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.chat_24px),
                            contentDescription = "Chat",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chat", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}