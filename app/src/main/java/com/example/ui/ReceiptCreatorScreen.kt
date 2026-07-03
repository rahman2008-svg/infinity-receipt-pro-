package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptCreatorScreen(
    viewModel: ReceiptViewModel,
    onNavigateBack: () -> Unit,
    onReceiptGenerated: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()
    val profile by viewModel.businessProfile.collectAsState()

    // Form inputs
    var selectedType by remember { mutableStateOf("Invoice") }
    var receiptNumber by remember { mutableStateOf("INV-${Calendar.getInstance().get(Calendar.YEAR)}-${(1000..9999).random()}") }
    
    // Customer
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerNameInput by remember { mutableStateOf("") }
    var customerPhoneInput by remember { mutableStateOf("") }
    var customerEmailInput by remember { mutableStateOf("") }
    var customerAddressInput by remember { mutableStateOf("") }
    
    // Items
    val currentItems = remember { mutableStateListOf<ReceiptItem>() }
    
    // Calculations
    var taxRateInput by remember { mutableStateOf("15.0") }
    var discountRateInput by remember { mutableStateOf("0.0") }
    var notesInput by remember { mutableStateOf("Payment due within 30 days.") }
    var footerInput by remember { mutableStateOf(profile?.footerText ?: "Thank you for your business!") }
    var paymentStatusInput by remember { mutableStateOf("PAID") }

    // Dialog flags
    var showCustomerDialog by remember { mutableStateOf(false) }
    var showProductDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    // Digital Signature
    val signaturePoints = remember { mutableStateListOf<Offset>() }

    val typesList = listOf(
        "Invoice", "Receipt", "Cash Memo", "Estimate", 
        "Quotation", "Rent Receipt", "Salary Receipt", "Donation Receipt"
    )

    // Derived states
    val subtotal = currentItems.sumOf { it.totalPrice }
    val taxRate = taxRateInput.toDoubleOrNull() ?: 0.0
    val taxAmount = subtotal * (taxRate / 100.0)
    val discountRate = discountRateInput.toDoubleOrNull() ?: 0.0
    val discountAmount = subtotal * (discountRate / 100.0)
    val totalAmount = (subtotal + taxAmount - discountAmount).coerceAtLeast(0.0)

    val currencySymbol = when (profile?.currencyCode) {
        "INR" -> "₹"
        "BDT" -> "৳"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "$"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Receipt") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Receipt Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Receipt Config",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Dropdown-style Selector for Document Type
                        var expandedTypeDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Document Type") },
                                trailingIcon = {
                                    IconButton(onClick = { expandedTypeDropdown = true }) {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedTypeDropdown = true }
                                    .testTag("type_dropdown")
                            )
                            DropdownMenu(
                                expanded = expandedTypeDropdown,
                                onDismissRequest = { expandedTypeDropdown = false }
                            ) {
                                typesList.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            selectedType = type
                                            expandedTypeDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = receiptNumber,
                            onValueChange = { receiptNumber = it },
                            label = { Text("Receipt/Invoice Number") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("receipt_number_input")
                        )
                    }
                }
            }

            // Customer details section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Customer Info",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { showCustomerDialog = true },
                                modifier = Modifier.testTag("select_customer_button")
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Select/Add")
                            }
                        }

                        if (selectedCustomer != null) {
                            ListItem(
                                headlineContent = { Text(selectedCustomer!!.name) },
                                supportingContent = { Text("${selectedCustomer!!.phone} | ${selectedCustomer!!.email}") },
                                leadingContent = {
                                    Icon(Icons.Filled.Person, contentDescription = null)
                                },
                                trailingContent = {
                                    IconButton(onClick = {
                                        selectedCustomer = null
                                        customerNameInput = ""
                                        customerPhoneInput = ""
                                        customerEmailInput = ""
                                        customerAddressInput = ""
                                    }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                    }
                                },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            )
                        } else {
                            OutlinedTextField(
                                value = customerNameInput,
                                onValueChange = { customerNameInput = it },
                                label = { Text("Customer Name (Optional)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_customer_name")
                            )
                            OutlinedTextField(
                                value = customerPhoneInput,
                                onValueChange = { customerPhoneInput = it },
                                label = { Text("Customer Phone (Optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Items List card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Receipt Items",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showAddItemDialog = true },
                                modifier = Modifier.testTag("add_item_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Item")
                            }
                        }

                        if (currentItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No items added. Click 'Add Item' above.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            currentItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.productName, fontWeight = FontWeight.Bold)
                                        Text("${item.quantity} x $currencySymbol${String.format("%.2f", item.unitPrice)}")
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "$currencySymbol${String.format("%.2f", item.totalPrice)}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = { currentItems.removeAt(index) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red)
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }

            // Taxes, Discounts & Payment Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tax & Discount Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = taxRateInput,
                                onValueChange = { taxRateInput = it },
                                label = { Text("Tax % (GST/VAT)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = discountRateInput,
                                onValueChange = { discountRateInput = it },
                                label = { Text("Discount %") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "Payment Status",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("PAID", "PENDING", "DUE").forEach { status ->
                                val selected = paymentStatusInput == status
                                FilterChip(
                                    selected = selected,
                                    onClick = { paymentStatusInput = status },
                                    label = { Text(status) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Digital Signature Pad Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Digital Signature",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { signaturePoints.clear() }) {
                                Text("Clear")
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset -> signaturePoints.add(offset) },
                                        onDrag = { change, _ ->
                                            signaturePoints.add(change.position)
                                        },
                                        onDragEnd = {
                                            signaturePoints.add(Offset.Unspecified)
                                        }
                                    )
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (signaturePoints.isNotEmpty()) {
                                    val path = Path()
                                    var firstPoint = true
                                    for (i in 0 until signaturePoints.size) {
                                        val point = signaturePoints[i]
                                        if (point == Offset.Unspecified) {
                                            firstPoint = true
                                        } else {
                                            if (firstPoint) {
                                                path.moveTo(point.x, point.y)
                                                firstPoint = false
                                            } else {
                                                path.lineTo(point.x, point.y)
                                            }
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color.DarkGray,
                                        style = Stroke(width = 5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Notes & Custom Footer Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Notes Section") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = footerInput,
                            onValueChange = { footerInput = it },
                            label = { Text("Receipt Footer Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Calculation Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryRow("Subtotal", "$currencySymbol${String.format("%.2f", subtotal)}")
                        if (taxRate > 0) {
                            SummaryRow("Tax ($taxRate%)", "+ $currencySymbol${String.format("%.2f", taxAmount)}")
                        }
                        if (discountRate > 0) {
                            SummaryRow("Discount ($discountRate%)", "- $currencySymbol${String.format("%.2f", discountAmount)}")
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$currencySymbol${String.format("%.2f", totalAmount)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Generate Button
            item {
                Button(
                    onClick = {
                        val itemsList = currentItems.toList()
                        if (itemsList.isEmpty()) {
                            // Can't generate empty receipt
                            return@Button
                        }
                        val cName = selectedCustomer?.name ?: customerNameInput.ifBlank { "Walk-in Customer" }
                        val cPhone = selectedCustomer?.phone ?: customerPhoneInput
                        val cEmail = selectedCustomer?.email ?: customerEmailInput
                        val cAddr = selectedCustomer?.address ?: customerAddressInput

                        // Serialize signature points to basic string: "x,y;x,y|x,y"
                        val serializedSignature = if (signaturePoints.isNotEmpty()) {
                            signaturePoints.joinToString(separator = "|") { pt ->
                                if (pt == Offset.Unspecified) "U" else "${pt.x},${pt.y}"
                            }
                        } else null

                        val receipt = Receipt(
                            receiptNumber = receiptNumber,
                            type = selectedType,
                            date = System.currentTimeMillis(),
                            customerId = selectedCustomer?.id,
                            customerName = cName,
                            customerPhone = cPhone,
                            customerEmail = cEmail,
                            customerAddress = cAddr,
                            items = itemsList,
                            subtotal = subtotal,
                            taxRate = taxRate,
                            taxAmount = taxAmount,
                            discountRate = discountRate,
                            discountAmount = discountAmount,
                            total = totalAmount,
                            paymentStatus = paymentStatusInput,
                            notes = notesInput,
                            currency = currencySymbol,
                            footer = footerInput,
                            signatureSvg = serializedSignature
                        )

                        viewModel.createReceipt(receipt) { generatedId ->
                            onReceiptGenerated(generatedId)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("generate_receipt_button"),
                    shape = RoundedCornerShape(16.dp),
                    enabled = currentItems.isNotEmpty()
                ) {
                    Text("Generate & Save", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // SELECT CUSTOMER DIALOG
    if (showCustomerDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val searchedList by viewModel.searchedCustomers.collectAsState()

        Dialog(onDismissRequest = { showCustomerDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Select Customer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showCustomerDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.setCustomerSearch(it)
                        },
                        placeholder = { Text("Search customer...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchedList) { cust ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCustomer = cust
                                        showCustomerDialog = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null)
                                    Column {
                                        Text(cust.name, fontWeight = FontWeight.Bold)
                                        Text(cust.phone, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            showCustomerDialog = false
                            showProductDialog = true // Trigger customer adding
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New Customer instead")
                    }
                }
            }
        }
    }

    // ADD NEW CUSTOMER DIALOG (triggered from customer selector)
    if (showProductDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showProductDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Create New Customer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { showProductDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val newCust = Customer(
                                        name = name,
                                        phone = phone,
                                        email = email,
                                        address = address
                                    )
                                    viewModel.addCustomer(newCust)
                                    selectedCustomer = newCust
                                    showProductDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = name.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // ADD ITEM DIALOG
    if (showAddItemDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val searchedProducts by viewModel.searchedProducts.collectAsState()
        
        var selectedProduct by remember { mutableStateOf<Product?>(null) }
        var quantityInput by remember { mutableStateOf("1") }
        var customPriceInput by remember { mutableStateOf("") }
        var customNameInputItem by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddItemDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Add Receipt Item",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (selectedProduct == null) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.setProductSearch(it)
                            },
                            placeholder = { Text("Search product database...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchedProducts) { prod ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedProduct = prod
                                            customPriceInput = prod.unitPrice.toString()
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(prod.name, fontWeight = FontWeight.Bold)
                                            Text("Stock: ${prod.stockQuantity} | SKU: ${prod.sku}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text("$currencySymbol${String.format("%.2f", prod.unitPrice)}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Allow entering custom custom items
                        OutlinedTextField(
                            value = customNameInputItem,
                            onValueChange = { customNameInputItem = it },
                            placeholder = { Text("Or Type Custom Item Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (customNameInputItem.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = customPriceInput,
                                    onValueChange = { customPriceInput = it },
                                    placeholder = { Text("Price") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = quantityInput,
                                    onValueChange = { quantityInput = it },
                                    placeholder = { Text("Qty") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Button(
                                onClick = {
                                    val price = customPriceInput.toDoubleOrNull() ?: 0.0
                                    val qty = quantityInput.toIntOrNull() ?: 1
                                    currentItems.add(
                                        ReceiptItem(
                                            productName = customNameInputItem,
                                            quantity = qty,
                                            unitPrice = price,
                                            totalPrice = price * qty
                                        )
                                    )
                                    showAddItemDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = customPriceInput.isNotBlank() && quantityInput.isNotBlank()
                            ) {
                                Text("Add Custom Item")
                            }
                        }
                    } else {
                        // Product is selected, finalize details
                        Text("Selected: ${selectedProduct!!.name}", fontWeight = FontWeight.Bold)
                        Text("Default Price: $currencySymbol${selectedProduct!!.unitPrice}")

                        OutlinedTextField(
                            value = customPriceInput,
                            onValueChange = { customPriceInput = it },
                            label = { Text("Override Unit Price") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = quantityInput,
                            onValueChange = { quantityInput = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { selectedProduct = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back to list")
                            }
                            Button(
                                onClick = {
                                    val finalPrice = customPriceInput.toDoubleOrNull() ?: selectedProduct!!.unitPrice
                                    val qty = quantityInput.toIntOrNull() ?: 1
                                    currentItems.add(
                                        ReceiptItem(
                                            productId = selectedProduct!!.id,
                                            productName = selectedProduct!!.name,
                                            sku = selectedProduct!!.sku,
                                            quantity = qty,
                                            unitPrice = finalPrice,
                                            totalPrice = finalPrice * qty
                                        )
                                    )
                                    showAddItemDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, valStr: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(valStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
