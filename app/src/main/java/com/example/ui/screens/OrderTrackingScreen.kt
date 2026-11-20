package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import com.example.core.SimFormatters
import com.example.core.shareDigitalReceipt
import com.example.data.model.OrderStage
import com.example.data.model.SellerProfile
import com.example.data.model.SimulatorOrder
import com.example.ui.components.ReceiptRow
import com.example.ui.components.SimDisclaimerBanner
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RubyError

@Composable
fun OrderTrackingScreen(
    orders: List<SimulatorOrder>,
    sellers: List<SellerProfile>,
    selectedOrderId: String?,
    onSelectOrder: (String?) -> Unit,
    onAdvanceDemoOrder: (SimulatorOrder) -> Unit,
    onCancelOrder: (SimulatorOrder) -> Unit,
    onRequestRefund: (SimulatorOrder, String) -> Unit,
    onAutoApproveDemoRefund: (SimulatorOrder) -> Unit,
    onSubmitReview: (SellerProfile, String, Int, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeOrder = orders.firstOrNull { it.id == selectedOrderId } ?: orders.firstOrNull()
    var refundTarget by remember { mutableStateOf<SimulatorOrder?>(null) }
    var reviewTarget by remember { mutableStateOf<SimulatorOrder?>(null) }
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Purchase & Order Tracking", style = MaterialTheme.typography.titleLarge)
        }

        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No simulated orders yet. Purchase from a verified demo store in Marketplace or Scan & Pay!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SimDisclaimerBanner("LIVE SIMULATOR TIMELINE • TIMESTAMPS RECORDED ON ACTUAL SIM EVENTS")
                }

                // Order Selector List
                items(orders, key = { it.id }) { ord ->
                    val isSelected = ord.id == activeOrder?.id
                    Surface(
                        onClick = { onSelectOrder(ord.id) },
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.2.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("order_item_${ord.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${ord.orderCode} • ${ord.sellerName}", style = MaterialTheme.typography.titleMedium)
                                Text(ord.itemsSummary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Status: ${ord.status} • Refund: ${ord.refundStatus}", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                            }
                            Text(
                                SimFormatters.formatSimUsd(ord.totalAmount),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MetallicGold
                            )
                        }
                    }
                }

                // Selected Order 9-Stage Live Timeline + Actions
                if (activeOrder != null) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Tracking Timeline (${activeOrder.orderCode})", style = MaterialTheme.typography.titleLarge)
                                ReceiptRow("Order ID", activeOrder.orderCode, mono = true)
                                ReceiptRow("Txn Ref", activeOrder.transactionRef, mono = true)
                                ReceiptRow("Card Used", activeOrder.cardSummary)

                                Spacer(Modifier.height(6.dp))

                                val eventMap = activeOrder.trackingEvents.associateBy { it.stage }
                                OrderStage.entries.forEach { stage ->
                                    val event = eventMap[stage.key]
                                    val reached = event != null
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (reached) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = stage.label,
                                            tint = if (reached) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${stage.stepNumber}. ${stage.label}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (reached) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (event != null && event.timestampMillis > 0L) {
                                                Text(
                                                    text = SimFormatters.formatMillis(event.timestampMillis),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                                    color = EmeraldSuccess
                                                )
                                            }
                                        }
                                    }
                                }

                                if (activeOrder.status in listOf("CANCELLED", "REFUNDED")) {
                                    Surface(
                                        color = AmberWarning.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Order Terminal Status: ${activeOrder.status} (${activeOrder.sellerResponse.ifEmpty { activeOrder.refundReason }})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AmberWarning,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(6.dp))

                                // Action Buttons
                                val isDemoStore = activeOrder.sellerOwnerUid == "demo_platform"
                                val currentStage = OrderStage.fromKey(activeOrder.status)
                                val isTerminal = activeOrder.status in listOf("CANCELLED", "REFUNDED", "COMPLETED")

                                if (isDemoStore && currentStage?.nextStage() != null && !isTerminal) {
                                    Button(
                                        onClick = { onAdvanceDemoOrder(activeOrder) },
                                        modifier = Modifier.fillMaxWidth().testTag("simulate_next_stage_btn")
                                    ) {
                                        Text("Advance Demo Stage -> ${currentStage.nextStage()!!.label}")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val inv = "SIMULATED INVOICE\nOrder: ${activeOrder.orderCode}\nSeller: ${activeOrder.sellerName}\nTotal: ${SimFormatters.formatSimUsd(activeOrder.totalAmount)}\nStatus: ${activeOrder.status}"
                                            shareDigitalReceipt(context, "Invoice ${activeOrder.orderCode}", inv)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Invoice")
                                    }

                                    if (activeOrder.status in listOf("ORDER_PLACED", "PAYMENT_SIMULATED")) {
                                        OutlinedButton(
                                            onClick = { onCancelOrder(activeOrder) },
                                            modifier = Modifier.weight(1f).testTag("cancel_order_btn")
                                        ) {
                                            Text("Cancel", color = RubyError)
                                        }
                                    } else if (activeOrder.refundStatus == "NONE" && activeOrder.status !in listOf("CANCELLED", "REFUNDED")) {
                                        OutlinedButton(
                                            onClick = { refundTarget = activeOrder },
                                            modifier = Modifier.weight(1f).testTag("request_refund_btn")
                                        ) {
                                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Refund")
                                        }
                                    }
                                }

                                if (isDemoStore && activeOrder.refundStatus == "REQUESTED") {
                                    Button(
                                        onClick = { onAutoApproveDemoRefund(activeOrder) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Simulate Demo Store Refund Approval")
                                    }
                                }

                                OutlinedButton(
                                    onClick = { reviewTarget = activeOrder },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Rate & Review ${activeOrder.sellerName}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    refundTarget?.let { order ->
        var reason by remember { mutableStateOf("Item not needed in simulator") }
        AlertDialog(
            onDismissRequest = { refundTarget = null },
            title = { Text("Request Simulated Refund") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Refund Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    refundTarget = null
                    onRequestRefund(order, reason)
                }) { Text("Submit Refund") }
            },
            dismissButton = {
                TextButton(onClick = { refundTarget = null }) { Text("Cancel") }
            }
        )
    }

    reviewTarget?.let { order ->
        val seller = sellers.firstOrNull { it.id == order.sellerId }
        var rating by remember { mutableFloatStateOf(5f) }
        var comment by remember { mutableStateOf("Fast simulated delivery and smooth checkout!") }
        AlertDialog(
            onDismissRequest = { reviewTarget = null },
            title = { Text("Review ${order.sellerName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rating: ${rating.toInt()} / 5 ★", style = MaterialTheme.typography.titleMedium, color = MetallicGold)
                    Slider(value = rating, onValueChange = { rating = it }, valueRange = 1f..5f, steps = 3)
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Your Review") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    reviewTarget = null
                    if (seller != null) {
                        onSubmitReview(seller, order.id, rating.toInt(), comment)
                    }
                }) { Text("Post Review") }
            },
            dismissButton = {
                TextButton(onClick = { reviewTarget = null }) { Text("Cancel") }
            }
        )
    }
}
