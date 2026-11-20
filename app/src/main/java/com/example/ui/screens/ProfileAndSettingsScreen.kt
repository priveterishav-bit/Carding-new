package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.SimFormatters
import com.example.data.model.SimulatorOrder
import com.example.data.model.SupportTicket
import com.example.data.model.TransactionRecord
import com.example.data.model.UserProfile
import com.example.data.model.VirtualCard
import com.example.ui.components.SimDisclaimerBanner
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RubyError

@Composable
fun ProfileAndSettingsScreen(
    profile: UserProfile,
    accountStatusLabel: String,
    isGuestMode: Boolean,
    cards: List<VirtualCard>,
    orders: List<SimulatorOrder>,
    transactions: List<TransactionRecord>,
    tickets: List<SupportTicket>,
    darkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onUpdateSettings: (Boolean, String, Boolean) -> Unit,
    onClaimDailyCoins: () -> Unit,
    onResetSimulator: () -> Unit,
    onSwitchToSellerMode: () -> Unit,
    onOpenCards: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAccountSwitch: () -> Unit,
    onSwitchToGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var policyDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showPinEditor by remember { mutableStateOf(false) }
    var showRefundHistory by remember { mutableStateOf(false) }

    val privacyText = stringResource(R.string.privacy_policy_body)
    val termsText = stringResource(R.string.terms_of_service_body)
    val dataSafetyText = stringResource(R.string.data_safety_body)

    val refundOrders = remember(orders) {
        orders.filter { it.refundStatus != "NONE" || it.status == "REFUNDED" }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Profile Header
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "User Profile Avatar",
                        tint = MetallicGold,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile.displayName, style = MaterialTheme.typography.titleLarge)
                        Text("$accountStatusLabel • ${profile.customerId}", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                        Text("${profile.coins} Vault Coins", style = MaterialTheme.typography.labelLarge, color = MetallicGold)
                    }
                    OutlinedButton(onClick = onClaimDailyCoins) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Daily")
                    }
                }
            }
        }

        item { SimDisclaimerBanner() }

        // Switch to Seller Mode Banner
        item {
            Surface(
                onClick = onSwitchToSellerMode,
                color = ElectricCyan.copy(alpha = 0.14f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth().testTag("profile_seller_mode_btn")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = ElectricCyan)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Open Seller Dashboard", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ElectricCyan)
                            Text("Manage store QR, products, orders & refunds", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text("Switch ->", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                }
            }
        }

        // Hub Links
        item {
            Text("My Vault Card Hub", style = MaterialTheme.typography.titleLarge)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileNavRow(Icons.Default.CreditCard, "My Vault Cards", "${cards.size} cards (${cards.count { it.cardActive }} active)", onOpenCards)
                ProfileNavRow(Icons.Default.LocalShipping, "My Purchases & Tracking", "${orders.size} virtual orders", onOpenOrders)
                ProfileNavRow(Icons.AutoMirrored.Filled.ReceiptLong, "My Transactions", "${transactions.size} ledger entries", onOpenOrders)
                ProfileNavRow(Icons.Default.Replay, "Refunds & Disputes", "${refundOrders.size} refund records", { showRefundHistory = true })
                ProfileNavRow(Icons.Default.SupportAgent, "Customer Support Center", "${tickets.size} tickets • AI Chat & FAQs", onOpenSupport)
            }
        }

        // Account Settings
        item {
            Text("Vault Card Settings", style = MaterialTheme.typography.titleLarge)
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingSwitchRow(
                        icon = Icons.Default.Lock,
                        title = "Vault PIN Lock (${profile.simPin})",
                        subtitle = "Require 4-digit PIN on launch & payments",
                        checked = profile.pinEnabled,
                        onCheckedChange = { onUpdateSettings(it, profile.simPin, profile.soundEnabled) },
                        onLabelClick = { showPinEditor = true }
                    )
                    SettingSwitchRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Sound Effects",
                        subtitle = "Play chime on payments & rewards",
                        checked = profile.soundEnabled,
                        onCheckedChange = { onUpdateSettings(profile.pinEnabled, profile.simPin, it) }
                    )
                    SettingSwitchRow(
                        icon = Icons.Default.DarkMode,
                        title = "Obsidian Dark Theme",
                        subtitle = "Black, deep sapphire & gold luxury interface",
                        checked = darkMode,
                        onCheckedChange = onToggleDarkMode
                    )
                    OutlinedButton(
                        onClick = onResetSimulator,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Account Balance", color = RubyError)
                    }
                }
            }
        }

        // Google Play Store Compliance & Legal
        item {
            Text("Legal & Play Store Disclosures", style = MaterialTheme.typography.titleLarge)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileNavRow(Icons.Default.Security, "Privacy Policy", "Data handling & authentication", {
                    policyDialog = "Privacy Policy" to privacyText
                })
                ProfileNavRow(Icons.Default.Gavel, "Terms & Conditions", "Fictional virtual-currency entertainment terms", {
                    policyDialog = "Terms & Conditions" to termsText
                })
                ProfileNavRow(Icons.Default.Description, "Data Safety Disclosure", "Accurate Play Store data disclosures", {
                    policyDialog = "Data Safety Disclosure" to dataSafetyText
                })
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenAccountSwitch,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("account_switch_btn")
                ) {
                    Text(if (isGuestMode) "Optional Login (Phone Number / Google)" else "Switch Account (Phone / Google / Guest)")
                }
                if (!isGuestMode) {
                    OutlinedButton(
                        onClick = onSwitchToGuest,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = RubyError, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Return to Guest Mode (No Login)", color = RubyError)
                    }
                }
            }
        }
    }

    policyDialog?.let { (title, body) ->
        AlertDialog(
            onDismissRequest = { policyDialog = null },
            title = { Text(title) },
            text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(onClick = { policyDialog = null }) { Text("Close") }
            }
        )
    }

    if (showPinEditor) {
        var newPin by remember { mutableStateOf(profile.simPin) }
        AlertDialog(
            onDismissRequest = { showPinEditor = false },
            title = { Text("Change 4-Digit Vault PIN") },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) newPin = it },
                    label = { Text("4-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length == 4) {
                            showPinEditor = false
                            onUpdateSettings(profile.pinEnabled, newPin, profile.soundEnabled)
                        }
                    }
                ) { Text("Save PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showPinEditor = false }) { Text("Cancel") }
            }
        )
    }

    if (showRefundHistory) {
        AlertDialog(
            onDismissRequest = { showRefundHistory = false },
            title = { Text("Refunds & Disputes History") },
            text = {
                if (refundOrders.isEmpty()) {
                    Text("No refund requests yet. You can request a simulated refund on any eligible order from Order Tracking.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        refundOrders.forEach { ord ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("${ord.orderCode} • ${SimFormatters.formatSimUsd(ord.totalAmount)}", style = MaterialTheme.typography.labelLarge)
                                    Text("Status: ${ord.refundStatus}", style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                    if (ord.sellerResponse.isNotBlank()) {
                                        Text("Seller: ${ord.sellerResponse}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showRefundHistory = false }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun ProfileNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onLabelClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onLabelClick != null) Modifier.clickable { onLabelClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
