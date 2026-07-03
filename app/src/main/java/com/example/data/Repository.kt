package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AppRepository(
    private val profileDao: BusinessProfileDao,
    private val customerDao: CustomerDao,
    private val productDao: ProductDao,
    private val receiptDao: ReceiptDao
) {
    // Business Profile
    val businessProfile: Flow<BusinessProfile?> = profileDao.getProfile()
    
    suspend fun getBusinessProfileSync(): BusinessProfile? {
        return profileDao.getProfileSync()
    }

    suspend fun updateBusinessProfile(profile: BusinessProfile) {
        profileDao.insertOrUpdate(profile)
    }

    // Customers
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    fun searchCustomers(query: String): Flow<List<Customer>> {
        return if (query.isBlank()) {
            customerDao.getAllCustomers()
        } else {
            customerDao.searchCustomers(query)
        }
    }

    suspend fun getCustomerById(id: Long): Customer? {
        return customerDao.getCustomerById(id)
    }

    suspend fun insertCustomer(customer: Customer): Long {
        return customerDao.insertCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteCustomer(customer)
    }

    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    fun searchProducts(query: String): Flow<List<Product>> {
        return if (query.isBlank()) {
            productDao.getAllProducts()
        } else {
            productDao.searchProducts(query)
        }
    }

    suspend fun getProductById(id: Long): Product? {
        return productDao.getProductById(id)
    }

    suspend fun insertProduct(product: Product): Long {
        return productDao.insertProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    suspend fun updateProductStock(id: Long, newStock: Int) {
        productDao.updateStock(id, newStock)
    }

    // Receipts
    val allReceipts: Flow<List<Receipt>> = receiptDao.getAllReceipts()

    suspend fun getReceiptById(id: Long): Receipt? {
        return receiptDao.getReceiptById(id)
    }

    suspend fun insertReceipt(receipt: Receipt): Long {
        return receiptDao.insertReceipt(receipt)
    }

    suspend fun deleteReceipt(receipt: Receipt) {
        receiptDao.deleteReceipt(receipt)
    }

    val totalSales: Flow<Double?> = receiptDao.getTotalSales()
    val pendingAmount: Flow<Double?> = receiptDao.getPendingAmount()
}
