package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.SimFormatters
import com.example.core.shareDigitalReceipt
import com.example.data.model.MarketplaceProduct
import com.example.data.model.OrderStage
import com.example.data.model.SellerProfile
import com.example.data.model.SimulatorOrder
import com.example.ui.components.DemoVerifiedBadge
import com.example.ui.components.QrCodeImage
import com.example.ui.components.SimDisclaimerBanner
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RubyError

@Composable
fun SellerDashboardScreen(
    sellerProfile: SellerProfile?,
    products: List<MarketplaceProduct>,
    incomingOrders: List<SimulatorOrder>,
    onSaveStore: (String, String, String, String) -> Unit,
    onSaveProduct: (MarketplaceProduct?, String, String, String, Double, Long) -> Unit,
    onAdvanceOrderStage: (SimulatorOrder) -> Unit,
    onResolveRefund: (SimulatorOrder, Boolean, String) -> Unit,
    onSwitchToBuyerMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showStoreDialog by remember { mutableStateOf(false) }
    var productDialogTarget by remember { mutableStateOf<Pair<Boolean, MarketplaceProduct?>>(false to null) }
    var refundResolveTarget by remember { mutableStateOf<SimulatorOrder?>(null) }

    val myProducts = remember(products, sellerProfile) {
        if (sellerProfile == null) emptyList() else products.filter { it.sellerId == sellerProfile.id }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Seller Dashboard", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Separate Seller Mode • Simulated Storefront",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    onClick = onSwitchToBuyerMode,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("switch_buyer_mode_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Buyer Mode", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            SimDisclaimerBanner("DEMO VERIFIED SELLER MODE • SIMULATED EARNINGS ONLY")
        }

        if (sellerProfile == null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(48.dp))
                        Text("Create Your Vault Store", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Open a virtual storefront, generate your unique Vault QR code, list virtual products, and accept in-app virtual payments.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showStoreDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_seller_store_btn")
                        ) {
                            Text("Create Seller Account")
                        }
                    }
                }
            }
        } else {
            // Seller KPIs & QR Code Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sellerProfile.storeName, style = MaterialTheme.typography.titleLarge)
                                Text("ID: ${sellerProfile.sellerCode} • ${sellerProfile.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DemoVerifiedBadge()
                                IconButton(onClick = { showStoreDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Store")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Virtual Earnings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        SimFormatters.formatSimUsd(sellerProfile.virtualEarnings),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldSuccess
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Orders", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${sellerProfile.completedOrdersCount}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MetallicGold
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            QrCodeImage(payload = sellerProfile.qrPayload, sizeDp = 165)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Unique Vault QR • ${sellerProfile.qrPayload}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                    }
                }
            }

            // Seller Products Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("My Products (${myProducts.size})", style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = { productDialogTarget = true to null },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_seller_product_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Product")
                    }
                }
            }

            if (myProducts.isEmpty()) {
                item {
                    Text(
                        "No products yet. Add your first product so buyers can discover it in the Marketplace.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(myProducts, key = { it.id }) { prod ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${SimFormatters.formatSimUsd(prod.price)} • Stock: ${prod.stockCount}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MetallicGold
                                )
                            }
                            IconButton(onClick = { productDialogTarget = true to prod }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Product")
                            }
                        }
                    }
                }
            }

            // Incoming Orders Header
            item {
                Text("Incoming Orders (${incomingOrders.size})", style = MaterialTheme.typography.titleLarge)
            }

            if (incomingOrders.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "No incoming orders yet. Share or scan your Store QR code in Scan & Pay to test receiving an order!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(incomingOrders, key = { it.id }) { order ->
                    SellerOrderCard(
                        order = order,
                        onAdvanceStage = { onAdvanceOrderStage(order) },
                        onResolveRefund = { refundResolveTarget = order },
                        onDownloadReceipt = {
                            val txt = "SELLER SIMULATOR RECEIPT\nOrder: ${order.orderCode}\nBuyer: ${order.buyerName}\nAmount: ${SimFormatters.formatSimUsd(order.totalAmount)}\nStatus: ${order.status}\nTxn: ${order.transactionRef}"
                            shareDigitalReceipt(context, "Seller Receipt ${order.orderCode}", txt)
                        }
                    )
                }
            }
        }
    }

    if (showStoreDialog) {
        SellerStoreEditorDialog(
            existing = sellerProfile,
            onDismiss = { showStoreDialog = false },
            onSave = { name, desc, cat, tone ->
                showStoreDialog = false
                onSaveStore(name, desc, cat, tone)
            }
        )
    }

    if (productDialogTarget.first) {
        SellerProductEditorDialog(
            existing = productDialogTarget.second,
            defaultCategory = sellerProfile?.category ?: "Digital",
            onDismiss = { productDialogTarget = false to null },
            onSave = { name, desc, cat, price, stock ->
                val target = productDialogTarget.second
                productDialogTarget = false to null
                onSaveProduct(target, name, desc, cat, price, stock)
            }
        )
    }

    refundResolveTarget?.let { order ->
        var response by remember { mutableStateOf("Approved simulated refund.") }
        AlertDialog(
            onDismissRequest = { refundResolveTarget = null },
            title = { Text("Refund Dispute (${order.orderCode})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Buyer Reason: ${order.refundReason}", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = response,
                        onValueChange = { response = it },
                        label = { Text("Seller Response") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        refundResolveTarget = null
                        onResolveRefund(order, true, response)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.Black)
                ) { Text("Approve Refund") }
            },
            dismissButton = {
                TextButton(onClick = {
                    refundResolveTarget = null
                    onResolveRefund(order, false, response)
                }) { Text("Decline", color = RubyError) }
            }
        )
    }
}

@Composable
private fun SellerOrderCard(
    order: SimulatorOrder,
    onAdvanceStage: () -> Unit,
    onResolveRefund: () -> Unit,
    onDownloadReceipt: () -> Unit
) {
    val curStage = OrderStage.fromKey(order.status)
    val nextStage = curStage?.nextStage()
    val isTerminal = order.status in listOf("CANCELLED", "REFUNDED", "COMPLETED")

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${order.orderCode} • ${order.buyerName}", style = MaterialTheme.typography.titleMedium)
                Text(SimFormatters.formatSimUsd(order.totalAmount), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
            }
            Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Stage: ${curStage?.label ?: order.status} • Refund: ${order.refundStatus}", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (!isTerminal && nextStage != null) {
                    Button(
                        onClick = onAdvanceStage,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-> ${nextStage.label}", maxLines = 1)
                    }
                }
                if (order.refundStatus == "REQUESTED") {
                    Button(
                        onClick = onResolveRefund,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Review Refund")
                    }
                }
                OutlinedButton(onClick = onDownloadReceipt) {
                    Icon(Icons.Default.Download, contentDescription = "Download Seller Receipt", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SellerStoreEditorDialog(
    existing: SellerProfile?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.storeName ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "Virtual Vault Card storefront.") }
    var category by remember { mutableStateOf(existing?.category ?: "Digital Goods") }
    var toneHex by remember { mutableStateOf(existing?.logoToneHex ?: "#F5C451") }
    val tones = listOf("#F5C451", "#38BDF8", "#10B981", "#A855F7")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create Seller Account" else "Edit Seller Store") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Store Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Store Description") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    tones.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(if (toneHex == hex) 3.dp else 1.dp, Color.White, CircleShape)
                                .clickable { toneHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, desc, category, toneHex) }) { Text("Save Store") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SellerProductEditorDialog(
    existing: MarketplaceProduct?,
    defaultCategory: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Long) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var cat by remember { mutableStateOf(existing?.category ?: defaultCategory) }
    var priceText by remember { mutableStateOf(existing?.price?.toString() ?: "99.0") }
    var stockText by remember { mutableStateOf(existing?.stockCount?.toString() ?: "25") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Virtual Product" else "Edit Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cat, onValueChange = { cat = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price (SIM USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it },
                    label = { Text("Stock Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val p = priceText.toDoubleOrNull() ?: 0.0
                val s = stockText.toLongOrNull() ?: 1L
                onSave(name, desc, cat, p, s)
            }) { Text("Save Product") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
