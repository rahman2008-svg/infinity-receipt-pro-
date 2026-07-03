package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    viewModel: ReceiptViewModel
) {
    val products by viewModel.searchedProducts.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()
    val searchQuery by viewModel.productSearchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedProductForStockUpdate by remember { mutableStateOf<Product?>(null) }

    val currencySymbol = when (profile?.currencyCode) {
        "INR" -> "₹"
        "BDT" -> "৳"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "$"
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { 
                    Text(
                        "Product Catalog", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00210E)
                        )
                    ) 
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = Color(0xFFF7FBF6)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF006D3A),
                contentColor = Color.White,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Product")
            }
        },
        containerColor = Color(0xFFF7FBF6)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setProductSearch(it) },
                placeholder = { Text("Search product catalog...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            "No products found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Tap '+' to add your first sales product.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products) { product ->
                        ProductItemRow(
                            product = product,
                            currencySymbol = currencySymbol,
                            onClick = { selectedProductForStockUpdate = product },
                            onDelete = { viewModel.deleteProduct(product) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // ADD PRODUCT DIALOG
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var sku by remember { mutableStateOf("") }
        var barcode by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Electronics") }
        var priceStr by remember { mutableStateOf("0.0") }
        var stockStr by remember { mutableStateOf("100") }
        var alertStr by remember { mutableStateOf("10") }

        val presets = listOf(
            Triple("Premium Laptop", "Electronics", Pair(1200.0, "LAP-101")),
            Triple("Hot Coffee", "Beverages", Pair(4.50, "COF-202")),
            Triple("Smartphone Pro", "Electronics", Pair(799.0, "PHN-303")),
            Triple("Kotlin Programming Guide", "Books", Pair(29.99, "BOK-404")),
            Triple("Design Consulting (hr)", "Services", Pair(150.0, "SRV-505"))
        )

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Add New Product",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00210E)
                    )

                    Text(
                        "QUICK PRESETS (পছন্দসই ডেমো প্রোডাক্ট)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF006D3A)
                        )
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.take(3).forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFE8F3EB), RoundedCornerShape(10.dp))
                                    .clickable {
                                        name = preset.first
                                        category = preset.second
                                        priceStr = preset.third.first.toString()
                                        sku = preset.third.second
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    preset.first.split(" ").last(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF006D3A)
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.drop(3).forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFFE8F3EB), RoundedCornerShape(10.dp))
                                    .clickable {
                                        name = preset.first
                                        category = preset.second
                                        priceStr = preset.third.first.toString()
                                        sku = preset.third.second
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    preset.first.split(" ").last(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF006D3A)
                                    )
                                )
                            }
                        }
                    }

                    Text(
                        "LIVE INTERACTIVE PREVIEW (লাইভ ভিউ)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF707972)
                        )
                    )

                    // LIVE PREVIEW CARD
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FBF6)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCDE8D4)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFD1E8D7), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Inventory,
                                    contentDescription = null,
                                    tint = Color(0xFF006D3A),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name.ifBlank { "Product Name Preview" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF00210E)),
                                    maxLines = 1
                                )
                                Text(
                                    text = "Category: $category | SKU: ${sku.ifBlank { "N/A" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    maxLines = 1
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val dPrice = priceStr.toDoubleOrNull() ?: 0.0
                                Text(
                                    text = "$currencySymbol${String.format("%.2f", dPrice)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF006D3A))
                                )
                                Text(
                                    text = "Stock: ${stockStr.ifBlank { "0" }}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Gray)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_product_name"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU Code") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Barcode") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Unit Sale Price") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = stockStr,
                            onValueChange = { stockStr = it },
                            label = { Text("Stock Qty") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = alertStr,
                            onValueChange = { alertStr = it },
                            label = { Text("Stock Alert Threshold") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { showAddDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val price = priceStr.toDoubleOrNull() ?: 0.0
                                    val stock = stockStr.toIntOrNull() ?: 100
                                    val alert = alertStr.toIntOrNull() ?: 10
                                    viewModel.addProduct(
                                        Product(
                                            name = name,
                                            sku = sku,
                                            barcode = barcode,
                                            category = category,
                                            unitPrice = price,
                                            stockQuantity = stock,
                                            stockAlertThreshold = alert
                                        )
                                    )
                                    showAddDialog = false
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dialog_save_product_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF006D3A),
                                contentColor = Color.White
                            ),
                            enabled = name.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // UPDATE STOCK DIALOG
    if (selectedProductForStockUpdate != null) {
        val prod = selectedProductForStockUpdate!!
        var currentStockInput by remember { mutableStateOf(prod.stockQuantity.toString()) }

        Dialog(onDismissRequest = { selectedProductForStockUpdate = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Update Stock Quantity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Product: ${prod.name}", style = MaterialTheme.typography.bodyMedium)

                    OutlinedTextField(
                        value = currentStockInput,
                        onValueChange = { currentStockInput = it },
                        label = { Text("Stock Quantity") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { selectedProductForStockUpdate = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val newQty = currentStockInput.toIntOrNull()
                                if (newQty != null) {
                                    viewModel.updateProductStock(prod.id, newQty)
                                    selectedProductForStockUpdate = null
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Update")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItemRow(
    product: Product,
    currencySymbol: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = product.stockQuantity <= product.stockAlertThreshold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("product_row_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E3E0)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFD1E8D7), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Inventory, contentDescription = null, tint = Color(0xFF006D3A))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF00210E)))
                Text("Category: ${product.category} | SKU: ${product.sku.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                if (isLowStock) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "LOW STOCK ALERT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$currencySymbol${String.format("%.2f", product.unitPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF006D3A)
                )
                Text(
                    "Stock: ${product.stockQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isLowStock) Color(0xFFC62828) else Color.Gray
                )
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.LightGray)
                }
            }
        }
    }
}
