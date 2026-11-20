package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.SimFormatters
import com.example.core.SimQrCodec
import com.example.core.saveReceiptToAppFiles
import com.example.core.shareDigitalReceipt
import com.example.data.model.TransactionRecord
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MetallicGold

@Composable
fun SimDisclaimerBanner(
    text: String = "VIRTUAL VAULT • NO REAL MONEY • NOT FOR REAL PAYMENTS",
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun DemoVerifiedBadge(modifier: Modifier = Modifier) {
    Surface(
        color = ElectricCyan.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Demo Verified Seller",
                tint = ElectricCyan,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "DEMO VERIFIED",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan
            )
        }
    }
}

@Composable
fun QrCodeImage(
    payload: String,
    sizeDp: Int = 180,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(payload) {
        SimQrCodec.generateQrBitmap(payload, 512)
    }
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, MetallicGold.copy(alpha = 0.6f)),
        modifier = modifier.size(sizeDp.dp)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Simulator QR Code for $payload",
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        )
    }
}

@Composable
fun PinConfirmDialog(
    expectedPin: String,
    title: String = "Confirm Vault PIN",
    subtitle: String = "Enter your 4-digit Vault PIN to authorize this virtual payment.",
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = "Simulator PIN Lock") },
        title = { Text(title) },
        text = {
            Column {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = entered,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) entered = it },
                    label = { Text("4-Digit PIN (Default: 1234)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_confirm_input")
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (entered == expectedPin) {
                        onConfirmed()
                    } else {
                        error = "Incorrect Vault PIN"
                    }
                },
                modifier = Modifier.testTag("pin_confirm_button")
            ) {
                Text("Verify PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PinLockGateScreen(
    expectedPin: String,
    onUnlocked: () -> Unit
) {
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Vault Card Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("Vault Card PIN Lock", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Enter your 4-digit Vault PIN to access your virtual wallet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = entered,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) entered = it },
                    label = { Text("4-Digit Vault PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (entered == expectedPin) onUnlocked() else error = "Incorrect PIN (check Profile settings or default 1234)"
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Unlock Vault Card")
                }
            }
        }
    }
}

@Composable
fun DigitalReceiptDialog(
    transaction: TransactionRecord,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var savedMsg by remember { mutableStateOf<String?>(null) }

    val receiptText = remember(transaction) {
        buildString {
            appendLine("=== VAULT CARD DIGITAL RECEIPT ===")
            appendLine("VIRTUAL TRANSACTION — NO REAL MONEY")
            appendLine("Transaction ID: ${transaction.transactionRef}")
            appendLine("Type: ${transaction.type}")
            appendLine("Merchant / Seller: ${transaction.merchantName}")
            if (transaction.sellerId.isNotBlank()) appendLine("Seller ID: ${transaction.sellerId}")
            appendLine("Amount: ${SimFormatters.formatSimUsd(transaction.amount)} (${transaction.currencyLabel})")
            appendLine("Card / Source: ${transaction.cardSummary}")
            appendLine("Status: ${transaction.status}")
            appendLine("Timestamp: ${SimFormatters.formatTimestamp(transaction.createdAt)}")
            appendLine("Note: ${transaction.note}")
            appendLine("=====================================")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldSuccess)
                Spacer(Modifier.width(8.dp))
                Text("Vault Card Receipt")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SimDisclaimerBanner("VIRTUAL RECEIPT • NOT FOR REAL PAYMENTS")
                ReceiptRow("Txn ID", transaction.transactionRef, mono = true)
                ReceiptRow("Merchant", transaction.merchantName)
                if (transaction.sellerId.isNotBlank()) ReceiptRow("Seller ID", transaction.sellerId, mono = true)
                ReceiptRow("Amount", SimFormatters.formatSimUsd(transaction.amount))
                ReceiptRow("Method", transaction.cardSummary)
                ReceiptRow("Status", transaction.status)
                ReceiptRow("Date", SimFormatters.formatTimestamp(transaction.createdAt))
                if (savedMsg != null) {
                    Text(savedMsg!!, color = EmeraldSuccess, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val file = saveReceiptToAppFiles(context, "${transaction.transactionRef}.txt", receiptText)
                    savedMsg = if (file != null) "Saved receipt locally!" else null
                    shareDigitalReceipt(context, "Vault Card Receipt ${transaction.transactionRef}", receiptText)
                }
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export Receipt")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ReceiptRow(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = if (mono) {
                MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.SemiBold)
            } else {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            },
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
