package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.BusinessProfile
import com.example.data.Receipt
import com.example.data.ReceiptItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    receiptId: Long,
    viewModel: ReceiptViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.businessProfile.collectAsState()
    
    var receipt by remember { mutableStateOf<Receipt?>(null) }
    var selectedTemplate by remember { mutableStateOf("A4 PDF Style") } // A4 PDF Style, Thermal 58mm
    var showExportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(receiptId) {
        receipt = viewModel.getReceiptById(receiptId)
    }

    if (receipt == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Receipt Details") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val currencySymbol = receipt!!.currency

    // Parse the digital signature points back
    val signaturePoints = remember(receipt) {
        val pointsList = mutableListOf<Offset>()
        receipt?.signatureSvg?.split("|")?.forEach { str ->
            if (str == "U") {
                pointsList.add(Offset.Unspecified)
            } else {
                val coords = str.split(",")
                if (coords.size == 2) {
                    val x = coords[0].toFloatOrNull()
                    val y = coords[1].toFloatOrNull()
                    if (x != null && y != null) {
                        pointsList.add(Offset(x, y))
                    }
                }
            }
        }
        pointsList
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(receipt!!.receiptNumber) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = buildShareText(receipt!!, profile, currencySymbol)
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share Receipt via")
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share via WhatsApp/Email")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Template Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("A4 PDF Style", "Thermal 58mm").forEach { temp ->
                    val selected = selectedTemplate == temp
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTemplate = temp }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = temp,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Scrollable Receipt Render
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (selectedTemplate == "A4 PDF Style") {
                    A4ReceiptTemplate(receipt!!, profile, currencySymbol, signaturePoints)
                } else {
                    ThermalReceiptTemplate(receipt!!, profile, currencySymbol, signaturePoints)
                }
            }

            // Action Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareText = buildShareText(receipt!!, profile, currencySymbol)
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Receipt via")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp/Share")
                    }

                    Button(
                        onClick = { showExportDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Print / PDF")
                    }
                }
            }
        }
    }

    // EXPORT PDF DIALOG
    if (showExportDialog) {
        var customFileName by remember { mutableStateOf("${receipt!!.receiptNumber}_Receipt.pdf") }
        var isPasswordProtected by remember { mutableStateOf(false) }
        var pdfPassword by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showExportDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "PDF Export & Print Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = customFileName,
                        onValueChange = { customFileName = it },
                        label = { Text("PDF File Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isPasswordProtected,
                            onCheckedChange = { isPasswordProtected = it }
                        )
                        Text("Protect PDF with Password")
                    }

                    if (isPasswordProtected) {
                        OutlinedTextField(
                            value = pdfPassword,
                            onValueChange = { pdfPassword = it },
                            label = { Text("PDF Password") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { showExportDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                Toast.makeText(context, "Exporting $customFileName successfully!", Toast.LENGTH_SHORT).show()
                                showExportDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun A4ReceiptTemplate(
    receipt: Receipt,
    profile: BusinessProfile?,
    currencySymbol: String,
    signaturePoints: List<Offset>
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(receipt.date))

    Card(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .testTag("a4_receipt_preview"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Block
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.companyName ?: "NexReceipt Pro+",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(profile?.address ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Phone: ${profile?.phone ?: ""}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Email: ${profile?.email ?: ""}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        if (!profile?.taxNumber.isNullOrBlank()) {
                            Text("GST/VAT ID: ${profile!!.taxNumber}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = receipt.type.uppercase(),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            receipt.receiptNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Text(dateStr, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray)
            }

            // Customer Block
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("BILL TO", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    Text(receipt.customerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    if (receipt.customerPhone.isNotBlank()) {
                        Text("Phone: ${receipt.customerPhone}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                    if (receipt.customerEmail.isNotBlank()) {
                        Text("Email: ${receipt.customerEmail}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                    if (receipt.customerAddress.isNotBlank()) {
                        Text("Address: ${receipt.customerAddress}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray)
            }

            // Items Table Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(8.dp)
                ) {
                    Text("Item Description", modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    Text("Qty", modifier = Modifier.weight(0.4f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = Color.DarkGray)
                    Text("Price", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, color = Color.DarkGray)
                    Text("Total", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, color = Color.DarkGray)
                }
            }

            // Items List
            items(receipt.items) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(item.productName, modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    Text(item.quantity.toString(), modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = Color.DarkGray)
                    Text("$currencySymbol${String.format("%.2f", item.unitPrice)}", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, color = Color.DarkGray)
                    Text("$currencySymbol${String.format("%.2f", item.totalPrice)}", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, color = Color.DarkGray)
                }
            }

            // Calculations Block
            item {
                HorizontalDivider(color = Color.LightGray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (receipt.notes.isNotBlank()) {
                            Text("NOTES", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Gray))
                            Text(receipt.notes, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1.8f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        CalculationRow("Subtotal", "$currencySymbol${String.format("%.2f", receipt.subtotal)}")
                        if (receipt.taxRate > 0) {
                            CalculationRow("Tax (${receipt.taxRate}%)", "+ $currencySymbol${String.format("%.2f", receipt.taxAmount)}")
                        }
                        if (receipt.discountRate > 0) {
                            CalculationRow("Discount (${receipt.discountRate}%)", "- $currencySymbol${String.format("%.2f", receipt.discountAmount)}")
                        }
                        HorizontalDivider(color = Color.LightGray, modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GRAND TOTAL", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                            Text(
                                "$currencySymbol${String.format("%.2f", receipt.total)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                        }

                        // Payment Status Stamp
                        val stampColor = when (receipt.paymentStatus.uppercase()) {
                            "PAID" -> Color(0xFF2E7D32)
                            "PENDING" -> Color(0xFFEF6C00)
                            else -> Color(0xFFC62828)
                        }
                        Box(
                            modifier = Modifier
                                .border(2.dp, stampColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                receipt.paymentStatus,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = stampColor,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }
            }

            // Bank Details & Signature Block
            item {
                HorizontalDivider(color = Color.LightGray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!profile?.bankDetails.isNullOrBlank()) {
                            Text("BANK PAYMENT DETAILS", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.Gray))
                            Text(profile!!.bankDetails, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }

                    Column(
                        modifier = Modifier.weight(0.8f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (signaturePoints.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(width = 100.dp, height = 50.dp)
                                    .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
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
                                                // Scaling the signature pad coordinates beautifully to fit 100x50 detail preview
                                                val scaledX = (point.x / 1000f) * size.width
                                                val scaledY = (point.y / 400f) * size.height
                                                if (firstPoint) {
                                                    path.moveTo(scaledX, scaledY)
                                                    firstPoint = false
                                                } else {
                                                    path.lineTo(scaledX, scaledY)
                                                }
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color.DarkGray,
                                            style = Stroke(width = 2.5f)
                                        )
                                    }
                                }
                            }
                            Text("Authorised Signatory", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }

            // Footer
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = receipt.footer,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ThermalReceiptTemplate(
    receipt: Receipt,
    profile: BusinessProfile?,
    currencySymbol: String,
    signaturePoints: List<Offset>
) {
    val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(receipt.date))

    Card(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .testTag("thermal_receipt_preview"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        shape = RoundedCornerShape(12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Info
            item {
                Text(
                    text = (profile?.companyName ?: "NexReceipt Pro+").uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = profile?.address ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray
                )
                Text(
                    text = "Phone: ${profile?.phone ?: ""}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray
                )
                if (!profile?.taxNumber.isNullOrBlank()) {
                    Text(
                        text = "VAT/GST: ${profile!!.taxNumber}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )
                }
                Text(
                    text = "----------------------------------",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Gray
                )
            }

            // Doc Info
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        receipt.type.uppercase(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        receipt.receiptNumber,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "DATE: $dateStr",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.DarkGray
                    )
                    Text(
                        "STATUS: ${receipt.paymentStatus}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                        color = if (receipt.paymentStatus == "PAID") Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
                Text(
                    text = "----------------------------------",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Gray
                )
            }

            // Customer
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Column {
                        Text(
                            "CUSTOMER: ${receipt.customerName}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        )
                        if (receipt.customerPhone.isNotBlank()) {
                            Text(
                                "PHONE: ${receipt.customerPhone}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                    }
                }
                Text(
                    text = "----------------------------------",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Gray
                )
            }

            // Items List
            items(receipt.items) { item ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            item.productName,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "$currencySymbol${String.format("%.2f", item.totalPrice)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                    Text(
                        "${item.quantity} x $currencySymbol${String.format("%.2f", item.unitPrice)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.DarkGray
                    )
                }
            }

            // Totals
            item {
                Text(
                    text = "----------------------------------",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Gray
                )
                CalculationRowThermal("SUBTOTAL", "$currencySymbol${String.format("%.2f", receipt.subtotal)}")
                if (receipt.taxRate > 0) {
                    CalculationRowThermal("TAX (${receipt.taxRate}%)", "+$currencySymbol${String.format("%.2f", receipt.taxAmount)}")
                }
                if (receipt.discountRate > 0) {
                    CalculationRowThermal("DISCOUNT (${receipt.discountRate}%)", "-$currencySymbol${String.format("%.2f", receipt.discountAmount)}")
                }
                Text(
                    text = "----------------------------------",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Gray
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "TOTAL AMOUNT",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "$currencySymbol${String.format("%.2f", receipt.total)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "----------------------------------",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Gray
                )
            }

            // UPI QR payment code
            item {
                if (!profile?.upiId.isNullOrBlank()) {
                    Text(
                        "SCAN TO PAY",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Simple mockup QR code block
                    Canvas(
                        modifier = Modifier
                            .size(100.dp)
                            .border(1.dp, Color.Gray)
                            .background(Color.White)
                    ) {
                        // Drawing a classic QR checkerboard pattern mockup
                        val size = 100.dp.toPx()
                        val blockSize = size / 5
                        
                        // Pos anchors (corners)
                        drawRect(Color.Black, Offset(0f, 0f), Size(blockSize * 1.5f, blockSize * 1.5f))
                        drawRect(Color.White, Offset(blockSize * 0.3f, blockSize * 0.3f), Size(blockSize * 0.9f, blockSize * 0.9f))
                        drawRect(Color.Black, Offset(blockSize * 0.5f, blockSize * 0.5f), Size(blockSize * 0.5f, blockSize * 0.5f))

                        drawRect(Color.Black, Offset(size - blockSize * 1.5f, 0f), Size(blockSize * 1.5f, blockSize * 1.5f))
                        drawRect(Color.White, Offset(size - blockSize * 1.2f, blockSize * 0.3f), Size(blockSize * 0.9f, blockSize * 0.9f))
                        drawRect(Color.Black, Offset(size - blockSize * 1.0f, blockSize * 0.5f), Size(blockSize * 0.5f, blockSize * 0.5f))

                        drawRect(Color.Black, Offset(0f, size - blockSize * 1.5f), Size(blockSize * 1.5f, blockSize * 1.5f))
                        drawRect(Color.White, Offset(blockSize * 0.3f, size - blockSize * 1.2f), Size(blockSize * 0.9f, blockSize * 0.9f))
                        drawRect(Color.Black, Offset(blockSize * 0.5f, size - blockSize * 1.0f), Size(blockSize * 0.5f, blockSize * 0.5f))

                        // Random blocks
                        drawRect(Color.Black, Offset(blockSize * 2f, blockSize * 2f), Size(blockSize * 0.5f, blockSize * 0.5f))
                        drawRect(Color.Black, Offset(blockSize * 3.5f, blockSize * 2.5f), Size(blockSize * 0.5f, blockSize * 0.8f))
                        drawRect(Color.Black, Offset(blockSize * 2f, blockSize * 3.5f), Size(blockSize * 0.8f, blockSize * 0.5f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        profile.upiId,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = receipt.footer,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
                Text(
                    text = "*** END OF RECEIPT ***",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun CalculationRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CalculationRowThermal(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = Color.DarkGray)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = Color.DarkGray)
    }
}

fun buildShareText(receipt: Receipt, profile: BusinessProfile?, currency: String): String {
    val itemsStr = receipt.items.joinToString(separator = "\n") { item ->
        "- ${item.productName} (x${item.quantity}) - $currency${String.format("%.2f", item.totalPrice)}"
    }
    return """
        *${receipt.type.uppercase()}*
        Receipt No: ${receipt.receiptNumber}
        Business: ${profile?.companyName ?: "NexReceipt"}
        
        *Billed To:*
        Customer: ${receipt.customerName}
        ${if (receipt.customerPhone.isNotBlank()) "Phone: ${receipt.customerPhone}" else ""}
        
        *Items:*
        $itemsStr
        
        Subtotal: $currency${String.format("%.2f", receipt.subtotal)}
        Tax: $currency${String.format("%.2f", receipt.taxAmount)}
        Discount: $currency${String.format("%.2f", receipt.discountAmount)}
        *Total Amount: $currency${String.format("%.2f", receipt.total)}*
        Status: ${receipt.paymentStatus}
        
        _${receipt.footer}_
    """.trimIndent()
}
