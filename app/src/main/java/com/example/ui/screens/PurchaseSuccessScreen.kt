package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.SimFormatters
import com.example.core.saveReceiptToAppFiles
import com.example.core.shareDigitalReceipt
import com.example.data.model.PurchaseSuccessReceipt
import com.example.ui.components.ReceiptRow
import com.example.ui.components.SimDisclaimerBanner
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MetallicGold

@Composable
fun PurchaseSuccessScreen(
    receipt: PurchaseSuccessReceipt,
    onTrackOrder: (String) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val tickScale = remember { Animatable(0f) }
    val ringScale = remember { Animatable(0.6f) }
    var savedMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(receipt.transactionRef) {
        ringScale.animateTo(1.08f, animationSpec = tween(420))
        tickScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    val receiptPlainText = remember(receipt) {
        buildString {
            appendLine("=== VAULT CARD VIRTUAL RECEIPT ===")
            appendLine("VIRTUAL IN-APP PAYMENT – NO REAL MONEY TRANSFERRED")
            appendLine("Transaction ID: ${receipt.transactionRef}")
            if (receipt.orderCode != null) appendLine("Order ID: ${receipt.orderCode}")
            appendLine("Seller / Store: ${receipt.sellerName}")
            appendLine("Buyer: ${receipt.buyerName}")
            appendLine("Items: ${receipt.itemsSummary}")
            appendLine("Amount Paid: ${SimFormatters.formatSimUsd(receipt.amount)} SIM USD")
            appendLine("Payment Method: ${receipt.cardSummary}")
            appendLine("Date & Time: ${SimFormatters.formatMillis(receipt.timestampMillis)}")
            appendLine("==================================")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        Color(0xFF06251C),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    color = EmeraldSuccess.copy(alpha = 0.16f),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(116.dp)
                        .scale(ringScale.value)
                ) {}
                Surface(
                    color = EmeraldSuccess,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(84.dp)
                        .scale(tickScale.value)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Payment Simulated Successfully",
                            tint = Color.Black,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Text(
                text = "Simulated Payment Successful!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = EmeraldSuccess,
                textAlign = TextAlign.Center
            )

            Text(
                text = SimFormatters.formatSimUsd(receipt.amount),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MetallicGold
            )

            SimDisclaimerBanner("SIMULATED PAYMENT – NO REAL MONEY TRANSFERRED")

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReceiptRow("Seller / Store", receipt.sellerName)
                    ReceiptRow("Buyer Name", receipt.buyerName)
                    ReceiptRow("Transaction ID", receipt.transactionRef, mono = true)
                    if (receipt.orderCode != null) {
                        ReceiptRow("Order ID", receipt.orderCode, mono = true)
                    }
                    ReceiptRow("Payment Method", receipt.cardSummary)
                    ReceiptRow("Date & Time", SimFormatters.formatMillis(receipt.timestampMillis))
                    ReceiptRow("Details", receipt.itemsSummary)
                }
            }

            if (savedMsg != null) {
                Text(savedMsg!!, color = EmeraldSuccess, style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val file = saveReceiptToAppFiles(context, "${receipt.transactionRef}.txt", receiptPlainText)
                        savedMsg = if (file != null) "Demo receipt saved & ready to share!" else null
                        shareDigitalReceipt(context, "Demo Receipt ${receipt.transactionRef}", receiptPlainText)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("download_demo_receipt_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Receipt")
                }

                if (receipt.orderId != null) {
                    Button(
                        onClick = { onTrackOrder(receipt.orderId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp).testTag("success_track_order_btn")
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Track Order")
                    }
                }
            }

            TextButton(
                onClick = onDone,
                modifier = Modifier.testTag("success_done_btn")
            ) {
                Text("Back to Wallet", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
