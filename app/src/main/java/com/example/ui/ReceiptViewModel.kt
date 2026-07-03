package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReceiptViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(
        database.businessProfileDao(),
        database.customerDao(),
        database.productDao(),
        database.receiptDao()
    )

    // Exposed States
    val businessProfile = repository.businessProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BusinessProfile()
    )

    val customers = repository.allCustomers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val products = repository.allProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val receipts = repository.allReceipts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalSales = repository.totalSales.map { it ?: 0.0 }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val pendingAmount = repository.pendingAmount.map { it ?: 0.0 }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Search query states
    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery = _customerSearchQuery.asStateFlow()

    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery = _productSearchQuery.asStateFlow()

    val searchedCustomers = _customerSearchQuery
        .flatMapLatest { query -> repository.searchCustomers(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchedProducts = _productSearchQuery
        .flatMapLatest { query -> repository.searchProducts(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        seedDatabaseIfEmpty()
    }

    fun setCustomerSearch(query: String) {
        _customerSearchQuery.value = query
    }

    fun setProductSearch(query: String) {
        _productSearchQuery.value = query
    }

    // Business Profile CRUD
    fun updateBusinessProfile(profile: BusinessProfile) {
        viewModelScope.launch {
            repository.updateBusinessProfile(profile)
        }
    }

    // Customer CRUD
    fun addCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.insertCustomer(customer)
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    // Product CRUD
    fun addProduct(product: Product) {
        viewModelScope.launch {
            repository.insertProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun updateProductStock(productId: Long, newStock: Int) {
        viewModelScope.launch {
            repository.updateProductStock(productId, newStock)
        }
    }

    // Receipt CRUD
    fun createReceipt(receipt: Receipt, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertReceipt(receipt)
            // Adjust product stock quantities based on sold items
            receipt.items.forEach { item ->
                if (item.productId != null) {
                    val prod = repository.getProductById(item.productId)
                    if (prod != null) {
                        val updatedStock = (prod.stockQuantity - item.quantity).coerceAtLeast(0)
                        repository.updateProductStock(item.productId, updatedStock)
                    }
                }
            }
            // Update customer due balances if invoice is PENDING or DUE
            if ((receipt.paymentStatus == "DUE" || receipt.paymentStatus == "PENDING") && receipt.customerId != null) {
                val cust = repository.getCustomerById(receipt.customerId)
                if (cust != null) {
                    val updatedDue = cust.dueBalance + receipt.total
                    repository.insertCustomer(cust.copy(dueBalance = updatedDue))
                }
            }
            onComplete(id)
        }
    }

    fun deleteReceipt(receipt: Receipt) {
        viewModelScope.launch {
            repository.deleteReceipt(receipt)
        }
    }

    suspend fun getReceiptById(id: Long): Receipt? {
        return repository.getReceiptById(id)
    }

    // Database seeding
    private fun seedDatabaseIfEmpty() {
        viewModelScope.launch {
            // Check business profile
            val currentProfile = repository.getBusinessProfileSync()
            if (currentProfile == null) {
                repository.updateBusinessProfile(BusinessProfile())
            }

            // Check customers
            repository.allCustomers.first().let { currentCustomers ->
                if (currentCustomers.isEmpty()) {
                    val c1 = Customer(name = "Ahmed Chowdhury", phone = "+880 1711-223344", email = "ahmed@mail.com", address = "Dhaka, Bangladesh", dueBalance = 450.0)
                    val c2 = Customer(name = "Sarah Miller", phone = "+1 415-555-2671", email = "sarah.m@gmail.com", address = "San Francisco, CA")
                    val c3 = Customer(name = "Kunal Sharma", phone = "+91 98765 43210", email = "kunal@outlook.com", address = "Mumbai, India", dueBalance = 0.0)
                    repository.insertCustomer(c1)
                    repository.insertCustomer(c2)
                    repository.insertCustomer(c3)
                }
            }

            // Check products
            repository.allProducts.first().let { currentProducts ->
                if (currentProducts.isEmpty()) {
                    val p1 = Product(name = "Wireless Office Mouse", sku = "WOM-102", barcode = "88019283", category = "Electronics", unitPrice = 25.0, stockQuantity = 80, stockAlertThreshold = 10)
                    val p2 = Product(name = "Mechanical Keyboard RGB", sku = "MKB-87", barcode = "12984712", category = "Electronics", unitPrice = 79.99, stockQuantity = 45, stockAlertThreshold = 5)
                    val p3 = Product(name = "Ergonomic Office Chair", sku = "EOC-500", barcode = "55610293", category = "Furniture", unitPrice = 189.50, stockQuantity = 12, stockAlertThreshold = 3)
                    val p4 = Product(name = "USB-C Hub 8-in-1", sku = "UCH-88", barcode = "99018273", category = "Electronics", unitPrice = 45.0, stockQuantity = 150, stockAlertThreshold = 15)
                    repository.insertProduct(p1)
                    repository.insertProduct(p2)
                    repository.insertProduct(p3)
                    repository.insertProduct(p4)
                }
            }

            // Check receipts
            repository.allReceipts.first().let { currentReceipts ->
                if (currentReceipts.isEmpty()) {
                    // Seed some dummy receipts to show in charts/dashboard
                    val profile = repository.getBusinessProfileSync() ?: BusinessProfile()
                    val currency = profile.currencyCode
                    
                    val items1 = listOf(
                        ReceiptItem(productName = "Wireless Office Mouse", sku = "WOM-102", quantity = 2, unitPrice = 25.0, totalPrice = 50.0),
                        ReceiptItem(productName = "USB-C Hub 8-in-1", sku = "UCH-88", quantity = 1, unitPrice = 45.0, totalPrice = 45.0)
                    )
                    val r1 = Receipt(
                        receiptNumber = "INV-2026-001",
                        type = "Invoice",
                        date = System.currentTimeMillis() - 86400000 * 2, // 2 days ago
                        customerId = 1,
                        customerName = "Ahmed Chowdhury",
                        customerPhone = "+880 1711-223344",
                        items = items1,
                        subtotal = 95.0,
                        taxRate = 10.0,
                        taxAmount = 9.5,
                        discountRate = 5.0,
                        discountAmount = 4.75,
                        total = 99.75,
                        paymentStatus = "PAID",
                        notes = "Thank you for shopping!",
                        currency = currency,
                        footer = profile.footerText
                    )
                    
                    val items2 = listOf(
                        ReceiptItem(productName = "Ergonomic Office Chair", sku = "EOC-500", quantity = 1, unitPrice = 189.50, totalPrice = 189.50)
                    )
                    val r2 = Receipt(
                        receiptNumber = "INV-2026-002",
                        type = "Cash Memo",
                        date = System.currentTimeMillis() - 3600000 * 4, // 4 hours ago
                        customerId = 2,
                        customerName = "Sarah Miller",
                        items = items2,
                        subtotal = 189.50,
                        taxRate = 8.0,
                        taxAmount = 15.16,
                        total = 204.66,
                        paymentStatus = "PAID",
                        notes = "Immediate cash payment",
                        currency = currency,
                        footer = profile.footerText
                    )
                    repository.insertReceipt(r1)
                    repository.insertReceipt(r2)
                }
            }
        }
    }
}
