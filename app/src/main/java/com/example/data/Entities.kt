package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReceiptItem(
    val productId: Long? = null,
    val productName: String,
    val sku: String? = null,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = 1,
    val companyName: String = "NexReceipt Ltd",
    val address: String = "123 Business Rd, suite 100",
    val phone: String = "+1 234 567 890",
    val email: String = "billing@nexreceipt.com",
    val website: String = "www.nexreceipt.com",
    val taxNumber: String = "VAT-9928341",
    val bankDetails: String = "Bank of Commerce\nAcct: 9988776655\nIFSC: BOCK000123",
    val upiId: String = "pay@upi",
    val currencyCode: String = "USD",
    val footerText: String = "Thank you for your business!",
    val logoUrl: String? = null
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val dueBalance: Double = 0.0
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String = "",
    val barcode: String = "",
    val category: String = "General",
    val unitPrice: Double = 0.0,
    val stockQuantity: Int = 100,
    val stockAlertThreshold: Int = 10
)

@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNumber: String,
    val type: String, // RECEIPT, INVOICE, CASH_MEMO, ESTIMATE, QUOTATION, etc.
    val date: Long = System.currentTimeMillis(),
    val customerId: Long? = null,
    val customerName: String,
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerAddress: String = "",
    val items: List<ReceiptItem>,
    val subtotal: Double,
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountRate: Double = 0.0,
    val discountAmount: Double = 0.0,
    val total: Double,
    val paymentStatus: String, // PAID, PENDING, DUE
    val notes: String = "",
    val currency: String = "$",
    val footer: String = "",
    val signatureSvg: String? = null
)
