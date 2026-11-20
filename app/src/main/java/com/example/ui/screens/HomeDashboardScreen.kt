package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.SimFormatters
import com.example.data.model.TransactionRecord
import com.example.data.model.UserProfile
import com.example.data.model.VirtualCard
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.components.PinConfirmDialog
import com.example.ui.components.SimDisclaimerBanner
import com.example.ui.components.VaultCardDetailsBottomSheet
import com.example.ui.components.VaultNetworkLogo
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RubyError

@Composable
fun HomeDashboardScreen(
    profile: UserProfile,
    cards: List<VirtualCard>,
    transactions: List<TransactionRecord>,
    accountStatusLabel: String,
    onOpenAccountSwitch: () -> Unit,
    onToggleUnlimited: (Boolean) -> Unit,
    onToggleCardActive: (VirtualCard) -> Unit,
    onAddMoney: (VirtualCard, Double) -> Unit,
    onQuickPay: (VirtualCard, Double, String, String) -> Unit,
    onResetBalances: () -> Unit,
    onClaimDailyCoins: () -> Unit,
    onScanQrClick: () -> Unit,
    onNavigateToCards: () -> Unit,
    onNavigateToCardStore: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onSwitchToSellerMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var showQuickPayDialog by remember { mutableStateOf(false) }
    var dialogInitialCard by remember { mutableStateOf<VirtualCard?>(null) }
    var inspectedCardId by remember { mutableStateOf<String?>(null) }
    var selectedReceipt by remember { mutableStateOf<TransactionRecord?>(null) }
    var pendingPayParams by remember { mutableStateOf<Triple<VirtualCard, Double, String>?>(null) }
    val inspectedCard = remember(cards, inspectedCardId) {
        cards.firstOrNull { it.id == inspectedCardId }
    }

    val activeCards = remember(cards) { cards.filter { it.cardActive } }
    val hasUnlimitedCard = remember(cards, profile.unlimitedMode) {
        profile.unlimitedMode || activeCards.any { it.unlimitedBalance }
    }
    val totalBalance = remember(cards) {
        cards.filter { !it.unlimitedBalance }.sumOf { it.balance }
    }
    val availableBalance = remember(activeCards) {
        activeCards.filter { !it.unlimitedBalance }.sumOf { it.balance }
    }
    val totalTopUps = remember(transactions) {
        transactions.filter { it.type == "TOP_UP" || it.type == "PAYMENT" }.sumOf { it.amount }
    }

    val todayKey = remember { SimFormatters.todayKey() }
    val canClaimDaily = profile.lastDailyClaimDay != todayKey

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
                    Text(
                        text = "Welcome, ${profile.displayName}",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$accountStatusLabel • ${profile.coins} Coins",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onOpenAccountSwitch() }
                    )
                }
                Surface(
                    onClick = onSwitchToSellerMode,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
                    modifier = Modifier.testTag("switch_seller_mode_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Switch to Seller Mode",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Seller Mode",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        item {
            SimDisclaimerBanner()
        }

        // Luxury Hero Balance Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0A1224), Color(0xFF152544), Color(0xFF1D1A0E))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Account Balance",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MetallicGold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (hasUnlimitedCard) "UNLIMITED SIM $" else SimFormatters.formatSimUsd(totalBalance),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AllInclusive,
                                        contentDescription = "Unlimited Mode",
                                        tint = if (profile.unlimitedMode) MetallicGold else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Switch(
                                        checked = profile.unlimitedMode,
                                        onCheckedChange = onToggleUnlimited,
                                        modifier = Modifier.testTag("unlimited_mode_switch")
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatMiniTile(
                                label = "Account Balance (Active)",
                                value = if (hasUnlimitedCard) "Unlimited" else SimFormatters.formatCompactUsd(availableBalance),
                                accent = ElectricCyan,
                                modifier = Modifier.weight(1f)
                            )
                            StatMiniTile(
                                label = "Payment Volume",
                                value = SimFormatters.formatCompactUsd(profile.totalSpent),
                                accent = MetallicGold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    dialogInitialCard = null
                                    showAddMoneyDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MetallicGold,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("add_virtual_money_btn")
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Card Top-Up", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = onNavigateToCardStore,
                                border = BorderStroke(1.dp, ElectricCyan),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("buy_new_card_btn")
                            ) {
                                Icon(Icons.Default.AddCard, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Buy Card", color = ElectricCyan, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Primary Scan QR CTA + Daily Coins + Quick Actions Row
        item {
            Button(
                onClick = onScanQrClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("scan_qr_button")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR", modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Scan QR (All UPI • PayPal • Dollar Pay)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    icon = Icons.Default.Payment,
                    label = "Quick Pay",
                    tint = ElectricCyan,
                    onClick = {
                        dialogInitialCard = null
                        showQuickPayDialog = true
                    },
                    modifier = Modifier.weight(1f).testTag("quick_pay_tile")
                )
                QuickActionTile(
                    icon = Icons.Default.CardGiftcard,
                    label = if (canClaimDaily) "Claim +500" else "Claimed",
                    tint = if (canClaimDaily) MetallicGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onClaimDailyCoins,
                    modifier = Modifier.weight(1f).testTag("claim_daily_coins_tile")
                )
                QuickActionTile(
                    icon = Icons.Default.SupportAgent,
                    label = "Support AI",
                    tint = EmeraldSuccess,
                    onClick = onNavigateToSupport,
                    modifier = Modifier.weight(1f).testTag("home_support_tile")
                )
                QuickActionTile(
                    icon = Icons.Default.Refresh,
                    label = "Reset Vault",
                    tint = RubyError,
                    onClick = onResetBalances,
                    modifier = Modifier.weight(1f).testTag("reset_sim_tile")
                )
            }
        }

        // My Cards Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Cards (${cards.size})", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onNavigateToCards) {
                    Text("Manage All")
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(cards, key = { it.id }) { card ->
                    val cardAccent = remember(card.accentColorHex) {
                        try {
                            Color(android.graphics.Color.parseColor(card.accentColorHex))
                        } catch (_: Exception) {
                            MetallicGold
                        }
                    }
                    Surface(
                        onClick = { inspectedCardId = card.id },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (card.cardActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else RubyError
                        ),
                        modifier = Modifier
                            .width(220.dp)
                            .testTag("home_card_tile_${card.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = card.cardTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(6.dp))
                                VaultNetworkLogo(
                                    tierKey = card.tierKey,
                                    accentColor = if (card.cardActive) cardAccent else RubyError,
                                    compact = true
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = if (card.cardActive) {
                                    SimFormatters.formatSimUsd(card.balance, card.unlimitedBalance || profile.unlimitedMode)
                                } else {
                                    "CARD DEACTIVATED"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (card.cardActive) MaterialTheme.colorScheme.onSurface else RubyError
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "•••• ${card.cardNumber.takeLast(4)} • Tap for details",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Spending Analytics Bar
        item {
            val denom = (profile.totalSpent + totalTopUps).coerceAtLeast(1.0)
            val spendRatio = (profile.totalSpent / denom).toFloat().coerceIn(0f, 1f)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vault Card Spending Analytics", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Payments: ${SimFormatters.formatCompactUsd(profile.totalSpent)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MetallicGold
                        )
                        Text(
                            "Top-Ups & Credits: ${SimFormatters.formatCompactUsd(totalTopUps)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldSuccess
                        )
                    }
                    LinearProgressIndicator(
                        progress = { spendRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MetallicGold,
                        trackColor = EmeraldSuccess.copy(alpha = 0.25f)
                    )
                }
            }
        }

        // Recent Transactions
        item {
            Text("Recent Transactions (${transactions.size})", style = MaterialTheme.typography.titleLarge)
        }

        if (transactions.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No simulated transactions yet. Try Quick Pay, Scan & Pay, or Marketplace!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(transactions.take(15), key = { it.id }) { txn ->
                TransactionRowItem(
                    txn = txn,
                    onClick = { selectedReceipt = txn }
                )
            }
        }
    }

    if (showAddMoneyDialog && cards.isNotEmpty()) {
        AddVirtualMoneyDialog(
            cards = cards,
            initialCard = dialogInitialCard,
            onDismiss = { showAddMoneyDialog = false },
            onConfirm = { card, amount ->
                showAddMoneyDialog = false
                onAddMoney(card, amount)
            }
        )
    }

    if (showQuickPayDialog && cards.isNotEmpty()) {
        QuickVirtualPaymentDialog(
            cards = cards,
            initialCard = dialogInitialCard,
            onDismiss = { showQuickPayDialog = false },
            onConfirm = { card, amount, merchant ->
                showQuickPayDialog = false
                if (profile.pinEnabled) {
                    pendingPayParams = Triple(card, amount, merchant)
                } else {
                    val key = "qpay_${System.currentTimeMillis()}"
                    onQuickPay(card, amount, merchant, key)
                }
            }
        )
    }

    pendingPayParams?.let { (card, amount, merchant) ->
        PinConfirmDialog(
            expectedPin = profile.simPin,
            onDismiss = { pendingPayParams = null },
            onConfirmed = {
                pendingPayParams = null
                val key = "qpay_${System.currentTimeMillis()}"
                onQuickPay(card, amount, merchant, key)
            }
        )
    }

    selectedReceipt?.let { txn ->
        DigitalReceiptDialog(transaction = txn, onDismiss = { selectedReceipt = null })
    }

    inspectedCard?.let { card ->
        VaultCardDetailsBottomSheet(
            card = card,
            globalUnlimitedMode = profile.unlimitedMode,
            onDismiss = { inspectedCardId = null },
            onPayClick = { target ->
                inspectedCardId = null
                dialogInitialCard = target
                showQuickPayDialog = true
            },
            onTopUpClick = { target ->
                inspectedCardId = null
                dialogInitialCard = target
                showAddMoneyDialog = true
            },
            onToggleActiveClick = { target ->
                onToggleCardActive(target)
            },
            secondaryActionLabel = "Manage All",
            onSecondaryActionClick = {
                inspectedCardId = null
                onNavigateToCards()
            }
        )
    }
}

@Composable
private fun StatMiniTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.06f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = accent)
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.height(82.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionRowItem(
    txn: TransactionRecord,
    onClick: () -> Unit
) {
    val isCredit = txn.type in listOf("TOP_UP", "PAYMENT", "DAILY_REWARD", "SELLER_PAYOUT")
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        (if (isCredit) EmeraldSuccess else MetallicGold).copy(alpha = 0.16f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = txn.type,
                    tint = if (isCredit) EmeraldSuccess else MetallicGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txn.merchantName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${txn.transactionRef} • ${txn.cardSummary} • SIMULATED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (txn.currencyLabel == "SIM COINS") {
                        "+${txn.amount.toLong()} Coins"
                    } else {
                        "${if (isCredit) "+" else "-"}${SimFormatters.formatCompactUsd(txn.amount)}"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isCredit) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Receipt",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun AddVirtualMoneyDialog(
    cards: List<VirtualCard>,
    initialCard: VirtualCard? = null,
    onDismiss: () -> Unit,
    onConfirm: (VirtualCard, Double) -> Unit
) {
    var selectedCard by remember(cards, initialCard) {
        mutableStateOf(initialCard?.let { init -> cards.firstOrNull { it.id == init.id } } ?: cards.first())
    }
    var amountText by remember { mutableStateOf("5000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Card Top-Up") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select a virtual card to top up its Account Balance (SIM USD):", style = MaterialTheme.typography.bodyMedium)
                cards.forEach { card ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (card.id == selectedCard.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedCard = card }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(card.cardTitle, style = MaterialTheme.typography.bodyMedium)
                        Text(SimFormatters.formatCompactUsd(card.balance), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1000", "10000", "100000").forEach { preset ->
                        OutlinedButton(
                            onClick = { amountText = preset },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("+$${preset.toInt() / 1000}K", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Custom Amount (SIM USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) onConfirm(selectedCard, amt)
                }
            ) {
                Text("Top Up Account Balance")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun QuickVirtualPaymentDialog(
    cards: List<VirtualCard>,
    initialCard: VirtualCard? = null,
    onDismiss: () -> Unit,
    onConfirm: (VirtualCard, Double, String) -> Unit
) {
    var selectedCard by remember(cards, initialCard) {
        mutableStateOf(
            initialCard?.let { init -> cards.firstOrNull { it.id == init.id } }
                ?: cards.firstOrNull { it.cardActive }
                ?: cards.first()
        )
    }
    var merchantName by remember { mutableStateOf("CyberMart Demo Store") }
    var amountText by remember { mutableStateOf("149.99") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay with Vault Card (Virtual)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SimDisclaimerBanner("VIRTUAL PAYMENT • NO REAL MONEY • NOT FOR REAL NETWORKS")
                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { merchantName = it },
                    label = { Text("Shop / Merchant Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (SIM USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Select Virtual Card:", style = MaterialTheme.typography.labelLarge)
                cards.forEach { card ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (card.id == selectedCard.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedCard = card }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (card.cardActive) card.cardTitle else "${card.cardTitle} (DEACTIVATED)",
                            color = if (card.cardActive) MaterialTheme.colorScheme.onSurface else RubyError,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = SimFormatters.formatCompactUsd(card.balance),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                if (error != null) {
                    Text(error!!, color = RubyError, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    when {
                        !selectedCard.cardActive -> error = "Selected card is DEACTIVATED. Activate it in My Cards first."
                        amt <= 0.0 -> error = "Enter a valid amount > $0"
                        merchantName.isBlank() -> error = "Enter a merchant name"
                        else -> onConfirm(selectedCard, amt, merchantName.trim())
                    }
                }
            ) {
                Text("Pay (Simulated)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
