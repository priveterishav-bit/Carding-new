package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.SimFormatters
import com.example.data.model.UserProfile
import com.example.data.model.VirtualCard
import com.example.ui.components.InteractiveVirtualCard
import com.example.ui.components.PinConfirmDialog
import com.example.ui.components.SimDisclaimerBanner
import com.example.ui.components.VaultCardDetailsBottomSheet
import com.example.ui.components.VaultNetworkLogo
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RubyError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MyCardsScreen(
    profile: UserProfile,
    cards: List<VirtualCard>,
    focusedCardId: String? = null,
    onFocusConsumed: () -> Unit = {},
    onToggleCardActive: (VirtualCard) -> Unit,
    onAddMoney: (VirtualCard, Double) -> Unit,
    onCustomizeAndUpgrade: (VirtualCard, String, String, Boolean) -> Unit,
    onQuickPay: (VirtualCard, Double, String, String) -> Unit,
    onOpenCardStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val pagerState = rememberPagerState(pageCount = { cards.size.coerceAtLeast(1) })

    LaunchedEffect(focusedCardId, cards.size) {
        if (focusedCardId != null && cards.isNotEmpty()) {
            val idx = cards.indexOfFirst { it.id == focusedCardId }
            if (idx >= 0) {
                pagerState.animateScrollToPage(idx)
            }
            onFocusConsumed()
        }
    }

    val selectedCard = cards.getOrNull(pagerState.currentPage) ?: cards.firstOrNull()
    val flipSignals = remember { mutableStateMapOf<String, Int>() }

    var showCustomizeDialog by remember { mutableStateOf(false) }
    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var showPayDialog by remember { mutableStateOf(false) }
    var inspectedCardId by remember { mutableStateOf<String?>(null) }
    var pendingPay by remember { mutableStateOf<Triple<VirtualCard, Double, String>?>(null) }
    val inspectedCard = remember(cards, inspectedCardId) {
        cards.firstOrNull { it.id == inspectedCardId }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("My Vault Cards", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Swipe 3D • Tap or Long-Press to Flip • ${profile.coins} Coins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onOpenCardStore,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("cards_open_store_btn")
                ) {
                    Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Card Store")
                }
            }
        }

        item {
            SimDisclaimerBanner(
                text = "VIRTUAL VAULT CARDS • NO REAL MONEY • NOT FOR REAL PAYMENTS",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (cards.isNotEmpty() && selectedCard != null) {
            // 3D Perspective Horizontal Card Carousel
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        pageSpacing = 12.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cards_3d_carousel")
                    ) { page ->
                        val card = cards[page]
                        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                        val scale = 1f - (0.12f * absOffset)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    rotationY = (pageOffset * -20f).coerceIn(-28f, 28f)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f - (0.32f * absOffset)
                                    cameraDistance = 16f * density
                                }
                        ) {
                            InteractiveVirtualCard(
                                card = card,
                                globalUnlimitedMode = profile.unlimitedMode,
                                externalFlipSignal = flipSignals[card.id] ?: 0,
                                onCardClick = { inspectedCardId = card.id },
                                onReactivateClick = { onToggleCardActive(card) }
                            )
                        }
                    }

                    // Carousel Controls, Dots & 3D Flip Trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (pagerState.currentPage > 0) {
                                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                    }
                                },
                                enabled = pagerState.currentPage > 0,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous card")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                cards.forEachIndexed { idx, _ ->
                                    val active = idx == pagerState.currentPage
                                    Box(
                                        modifier = Modifier
                                            .height(8.dp)
                                            .width(if (active) 22.dp else 8.dp)
                                            .clip(CircleShape)
                                            .background(if (active) MetallicGold else MaterialTheme.colorScheme.outline)
                                            .clickable { scope.launch { pagerState.animateScrollToPage(idx) } }
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (pagerState.currentPage < cards.lastIndex) {
                                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                    }
                                },
                                enabled = pagerState.currentPage < cards.lastIndex,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next card")
                            }
                        }

                        Surface(
                            onClick = {
                                flipSignals[selectedCard.id] = (flipSignals[selectedCard.id] ?: 0) + 1
                            },
                            color = ElectricCyan.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("carousel_flip_3d_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = "Flip 3D Card", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Flip 3D (${pagerState.currentPage + 1}/${cards.size})", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                            }
                        }
                    }
                }
            }

            // Slide-to-Check Animated Credentials Bar
            item {
                SlideToCheckCredentialsPanel(
                    card = selectedCard,
                    onOpenFullSheet = { inspectedCardId = selectedCard.id },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Card Activation & Deactivation Control Panel
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selectedCard.cardActive) EmeraldSuccess.copy(alpha = 0.45f) else RubyError.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (selectedCard.cardActive) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Card Activation Status",
                                tint = if (selectedCard.cardActive) EmeraldSuccess else RubyError
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (selectedCard.cardActive) "Card Active (Ready to Use)" else "CARD DEACTIVATED",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedCard.cardActive) EmeraldSuccess else RubyError
                                )
                                Text(
                                    text = if (selectedCard.cardActive) "Toggle off to lock virtual purchases on this card."
                                    else "Virtual purchases disabled. Toggle on to reactivate.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = selectedCard.cardActive,
                            onCheckedChange = { onToggleCardActive(selectedCard) },
                            modifier = Modifier.testTag("card_active_toggle")
                        )
                    }
                }
            }

            // Action Grid for Selected Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showPayDialog = true },
                        enabled = selectedCard.cardActive,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("card_quick_pay_btn")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pay")
                    }
                    OutlinedButton(
                        onClick = { showAddMoneyDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("card_top_up_btn")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Top Up")
                    }
                    OutlinedButton(
                        onClick = { showCustomizeDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("card_customize_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Upgrade")
                    }
                }
            }
        }

        // Card Collection Switcher
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Your Collection (${cards.size})", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                    val isSelected = card.id == selectedCard?.id
                    val cardAccent = remember(card.accentColorHex) {
                        try {
                            Color(android.graphics.Color.parseColor(card.accentColorHex))
                        } catch (_: Exception) {
                            MetallicGold
                        }
                    }
                    Surface(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                            inspectedCardId = card.id
                        },
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.4.dp,
                            when {
                                !card.cardActive -> RubyError
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline
                            }
                        ),
                        modifier = Modifier
                            .width(200.dp)
                            .testTag("collection_card_tile_${card.id}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = card.cardTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(4.dp))
                                VaultNetworkLogo(
                                    tierKey = card.tierKey,
                                    accentColor = if (card.cardActive) cardAccent else RubyError,
                                    compact = true
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (card.cardActive) SimFormatters.formatCompactUsd(card.balance) else "DEACTIVATED",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (card.cardActive) MetallicGold else RubyError
                            )
                            Text(
                                text = "•••• ${card.cardNumber.takeLast(4)} • EXP ${card.expiryDisplay}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddMoneyDialog && selectedCard != null) {
        AddVirtualMoneyDialog(
            cards = listOf(selectedCard),
            onDismiss = { showAddMoneyDialog = false },
            onConfirm = { card, amt ->
                showAddMoneyDialog = false
                onAddMoney(card, amt)
            }
        )
    }

    if (showPayDialog && selectedCard != null) {
        QuickVirtualPaymentDialog(
            cards = listOf(selectedCard),
            onDismiss = { showPayDialog = false },
            onConfirm = { card, amt, merch ->
                showPayDialog = false
                if (profile.pinEnabled) {
                    pendingPay = Triple(card, amt, merch)
                } else {
                    onQuickPay(card, amt, merch, "cardpay_${System.currentTimeMillis()}")
                }
            }
        )
    }

    pendingPay?.let { (card, amt, merch) ->
        PinConfirmDialog(
            expectedPin = profile.simPin,
            onDismiss = { pendingPay = null },
            onConfirmed = {
                pendingPay = null
                onQuickPay(card, amt, merch, "cardpay_${System.currentTimeMillis()}")
            }
        )
    }

    if (showCustomizeDialog && selectedCard != null) {
        CustomizeUpgradeCardDialog(
            card = selectedCard,
            coins = profile.coins,
            onDismiss = { showCustomizeDialog = false },
            onSave = { name, hex, upgrade ->
                showCustomizeDialog = false
                onCustomizeAndUpgrade(selectedCard, name, hex, upgrade)
            }
        )
    }

    inspectedCard?.let { card ->
        VaultCardDetailsBottomSheet(
            card = card,
            globalUnlimitedMode = profile.unlimitedMode,
            onDismiss = { inspectedCardId = null },
            onPayClick = { target ->
                val idx = cards.indexOfFirst { it.id == target.id }
                if (idx >= 0) scope.launch { pagerState.animateScrollToPage(idx) }
                inspectedCardId = null
                showPayDialog = true
            },
            onTopUpClick = { target ->
                val idx = cards.indexOfFirst { it.id == target.id }
                if (idx >= 0) scope.launch { pagerState.animateScrollToPage(idx) }
                inspectedCardId = null
                showAddMoneyDialog = true
            },
            onToggleActiveClick = { target ->
                onToggleCardActive(target)
            },
            secondaryActionLabel = "Upgrade",
            onSecondaryActionClick = { target ->
                val idx = cards.indexOfFirst { it.id == target.id }
                if (idx >= 0) scope.launch { pagerState.animateScrollToPage(idx) }
                inspectedCardId = null
                showCustomizeDialog = true
            }
        )
    }
}

@Composable
private fun SlideToCheckCredentialsPanel(
    card: VirtualCard,
    onOpenFullSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sliderProgress by remember(card.id) { mutableFloatStateOf(0f) }
    val isUnlocked = sliderProgress >= 0.72f
    val animatedFill by animateFloatAsState(
        targetValue = sliderProgress,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "reveal_slider_fill"
    )
    var copiedMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiedMsg) {
        if (copiedMsg != null) {
            delay(1600)
            copiedMsg = null
        }
    }

    fun copyValue(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        copiedMsg = "$label copied"
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (isUnlocked) MetallicGold.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = if (isUnlocked) MetallicGold else ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isUnlocked) "Card Details Unlocked (Vault)" else "Slide to Check Number, Expiry & CVV",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isUnlocked) MetallicGold else MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(
                    onClick = { sliderProgress = if (isUnlocked) 0f else 1f },
                    modifier = Modifier.testTag("slide_quick_toggle_btn")
                ) {
                    Text(if (isUnlocked) "Hide" else "Quick Check")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                ElectricCyan.copy(alpha = 0.14f + 0.18f * animatedFill),
                                MetallicGold.copy(alpha = 0.08f + 0.24f * animatedFill)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Slider(
                    value = sliderProgress,
                    onValueChange = { sliderProgress = it },
                    onValueChangeFinished = {
                        sliderProgress = if (sliderProgress >= 0.65f) 1f else 0f
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = if (isUnlocked) MetallicGold else ElectricCyan,
                        activeTrackColor = MetallicGold,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("slide_reveal_credentials_slider")
                )
            }

            AnimatedVisibility(
                visible = isUnlocked,
                enter = expandVertically(spring(dampingRatio = 0.8f)) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                AnimatedContent(
                    targetState = card,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "card_cred_switch"
                ) { activeCard ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Vault Card Number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = activeCard.cardNumber,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                            IconButton(
                                onClick = { copyValue("Card number", activeCard.cardNumber) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy card number", tint = MetallicGold, modifier = Modifier.size(18.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                onClick = { copyValue("Expiry", activeCard.expiryDisplay) },
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Expiry", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = activeCard.expiryDisplay,
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                                        color = ElectricCyan
                                    )
                                }
                            }
                            Surface(
                                onClick = { copyValue("CVV", activeCard.cvvDisplay) },
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Vault CVV", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = activeCard.cvvDisplay,
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                                        color = MetallicGold
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onOpenFullSheet,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Text("Full Sheet", maxLines = 1)
                            }
                        }

                        if (copiedMsg != null) {
                            Text(
                                text = "$copiedMsg (virtual card testing only)",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizeUpgradeCardDialog(
    card: VirtualCard,
    coins: Long,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit
) {
    var holderName by remember { mutableStateOf(card.cardholderName) }
    var accentHex by remember { mutableStateOf(card.accentColorHex) }
    val swatches = listOf("#F5C451", "#38BDF8", "#10B981", "#A855F7", "#FB7185", "#E2E8F0")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize & Upgrade Card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = holderName,
                    onValueChange = { holderName = it },
                    label = { Text("Cardholder Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Accent Finish:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    swatches.forEach { hex ->
                        val c = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (accentHex == hex) 3.dp else 1.dp,
                                    color = if (accentHex == hex) Color.White else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { accentHex = hex }
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MetallicGold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Upgrade (+200 Coins): Unlocks animated holographic sheen & +$5,000 SIM bonus. Your coins: $coins",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onSave(holderName, accentHex, false) }) {
                    Text("Save Look")
                }
                Button(
                    onClick = { onSave(holderName, accentHex, true) },
                    enabled = coins >= 200L && card.upgradeLevel < 10L
                ) {
                    Text("Upgrade (200)")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
