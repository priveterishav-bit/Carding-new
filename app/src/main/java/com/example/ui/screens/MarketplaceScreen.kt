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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.SimFormatters
import com.example.data.model.CardCatalog
import com.example.data.model.CardCatalogTier
import com.example.data.model.CartItem
import com.example.data.model.MarketplaceProduct
import com.example.data.model.SellerProfile
import com.example.data.model.SellerReview
import com.example.data.model.UserProfile
import com.example.data.model.VirtualCard
import com.example.ui.components.DemoVerifiedBadge
import com.example.ui.components.PinConfirmDialog
import com.example.ui.components.QrCodeImage
import com.example.ui.components.SimDisclaimerBanner
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RubyError
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    profile: UserProfile,
    cards: List<VirtualCard>,
    sellers: List<SellerProfile>,
    products: List<MarketplaceProduct>,
    reviews: List<SellerReview>,
    cartItems: List<CartItem>,
    initialSubTab: Int = 0,
    onAddToCart: (MarketplaceProduct) -> Unit,
    onUpdateCartQty: (String, Int) -> Unit,
    onCheckoutCart: (VirtualCard, SellerProfile, List<CartItem>, String) -> Unit,
    onBuyCard: (CardCatalogTier, String, String) -> Unit,
    onOpenOrderTracking: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    var searchQuery by remember { mutableStateOf("") }
    var inspectedSeller by remember { mutableStateOf<SellerProfile?>(null) }
    var showCartDialog by remember { mutableStateOf(false) }
    var pendingCheckout by remember { mutableStateOf<Pair<VirtualCard, List<CartItem>>?>(null) }
    var cardToCustomize by remember { mutableStateOf<CardCatalogTier?>(null) }

    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.storeName.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredSellers = remember(sellers, searchQuery) {
        if (searchQuery.isBlank()) sellers
        else sellers.filter {
            it.storeName.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Marketplace", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${profile.coins} Coins • Simulated Stores & Cards",
                        style = MaterialTheme.typography.labelSmall,
                        color = MetallicGold
                    )
                }
                OutlinedButton(
                    onClick = onOpenOrderTracking,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("open_orders_tracking_btn")
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Orders")
                }
            }

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Stores & Shop") },
                    modifier = Modifier.testTag("marketplace_stores_tab")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Card Store") },
                    modifier = Modifier.testTag("marketplace_cards_tab")
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = if (cartItems.isNotEmpty()) 96.dp else 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search products") },
                            placeholder = { Text("Search demo stores & products...") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("marketplace_search_input")
                        )
                    }

                    item {
                        Text("Verified Demo Sellers (${filteredSellers.size})", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredSellers, key = { it.id }) { seller ->
                                SellerStoreCard(
                                    seller = seller,
                                    onClick = { inspectedSeller = seller }
                                )
                            }
                        }
                    }

                    item {
                        Text("Featured Products (${filteredProducts.size})", style = MaterialTheme.typography.titleLarge)
                    }

                    items(filteredProducts, key = { it.id }) { prod ->
                        ProductRowCard(
                            product = prod,
                            onAddToCart = { onAddToCart(prod) },
                            onInspectStore = {
                                inspectedSeller = sellers.firstOrNull { it.id == prod.sellerId }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SimDisclaimerBanner("PURCHASE DEMO CARDS WITH FREE IN-APP COINS • Balance: ${profile.coins} Coins")
                    }
                    items(CardCatalog.tiers, key = { it.tierKey }) { tier ->
                        CardCatalogRow(
                            tier = tier,
                            userCoins = profile.coins,
                            onBuyClick = { cardToCustomize = tier }
                        )
                    }
                }
            }
        }

        // Floating Cart Bar
        if (cartItems.isNotEmpty() && selectedTab == 0) {
            val total = cartItems.sumOf { it.product.price * it.quantity }
            val count = cartItems.sumOf { it.quantity }
            Surface(
                onClick = { showCartDialog = true },
                color = MetallicGold,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("floating_cart_bar")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.Black)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "$count Item(s) in Cart",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }
                    Text(
                        text = "Checkout ${SimFormatters.formatSimUsd(total)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                }
            }
        }
    }

    inspectedSeller?.let { seller ->
        SellerProfileModal(
            seller = seller,
            products = products.filter { it.sellerId == seller.id },
            reviews = reviews.filter { it.sellerId == seller.id },
            onAddToCart = onAddToCart,
            onDismiss = { inspectedSeller = null }
        )
    }

    if (showCartDialog) {
        CartCheckoutDialog(
            cartItems = cartItems,
            cards = cards,
            unlimitedMode = profile.unlimitedMode,
            onUpdateQty = onUpdateCartQty,
            onDismiss = { showCartDialog = false },
            onProceedCheckout = { card, sellerItems ->
                showCartDialog = false
                if (profile.pinEnabled) {
                    pendingCheckout = card to sellerItems
                } else {
                    val sellerId = sellerItems.first().product.sellerId
                    val seller = sellers.firstOrNull { it.id == sellerId } ?: return@CartCheckoutDialog
                    onCheckoutCart(card, seller, sellerItems, "cart_${System.currentTimeMillis()}")
                }
            }
        )
    }

    pendingCheckout?.let { (card, sellerItems) ->
        PinConfirmDialog(
            expectedPin = profile.simPin,
            onDismiss = { pendingCheckout = null },
            onConfirmed = {
                pendingCheckout = null
                val sellerId = sellerItems.first().product.sellerId
                val seller = sellers.firstOrNull { it.id == sellerId } ?: return@PinConfirmDialog
                onCheckoutCart(card, seller, sellerItems, "cart_${System.currentTimeMillis()}")
            }
        )
    }

    cardToCustomize?.let { tier ->
        BuyCardCustomizationDialog(
            tier = tier,
            defaultHolder = profile.displayName,
            onDismiss = { cardToCustomize = null },
            onConfirmBuy = { holderName, accentHex ->
                cardToCustomize = null
                onBuyCard(tier, holderName, accentHex)
            }
        )
    }
}

@Composable
private fun SellerStoreCard(
    seller: SellerProfile,
    onClick: () -> Unit
) {
    val tone = remember(seller.logoToneHex) {
        try { Color(android.graphics.Color.parseColor(seller.logoToneHex)) } catch (_: Exception) { MetallicGold }
    }
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, tone.copy(alpha = 0.5f)),
        modifier = Modifier.width(235.dp).testTag("seller_card_${seller.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(tone.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = tone)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(seller.storeName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(seller.sellerCode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DemoVerifiedBadge()
            Text(
                seller.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${String.format(Locale.US, "%.1f", seller.averageRating)} • ${seller.completedOrdersCount} orders",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ProductRowCard(
    product: MarketplaceProduct,
    onAddToCart: () -> Unit,
    onInspectStore: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ElectricCyan.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = ElectricCyan)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${product.storeName} • ${product.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    modifier = Modifier.clickable { onInspectStore() }
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = SimFormatters.formatSimUsd(product.price),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MetallicGold
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onAddToCart,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("add_to_cart_${product.id}")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add to Cart", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }
    }
}

@Composable
private fun CardCatalogRow(
    tier: CardCatalogTier,
    userCoins: Long,
    onBuyClick: () -> Unit
) {
    val accent = remember(tier.defaultAccentHex) {
        Color(android.graphics.Color.parseColor(tier.defaultAccentHex))
    }
    val canAfford = userCoins >= tier.coinPrice
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tier.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = "${tier.networkLabel} • ${tier.badgeText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
                Text(
                    text = "Account Balance: ${SimFormatters.formatSimUsd(tier.startingBalance)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onBuyClick,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) accent else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("buy_tier_${tier.tierKey}")
            ) {
                Text("${tier.coinPrice} Coins")
            }
        }
    }
}

@Composable
private fun SellerProfileModal(
    seller: SellerProfile,
    products: List<MarketplaceProduct>,
    reviews: List<SellerReview>,
    onAddToCart: (MarketplaceProduct) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(seller.storeName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    DemoVerifiedBadge()
                }
                Text("Seller ID: ${seller.sellerCode} • ${seller.completedOrdersCount} Completed Orders", style = MaterialTheme.typography.labelSmall)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(380.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(seller.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        QrCodeImage(payload = seller.qrPayload, sizeDp = 140)
                    }
                }
                item {
                    Text("Products (${products.size})", style = MaterialTheme.typography.titleMedium)
                }
                items(products, key = { it.id }) { prod ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.name, style = MaterialTheme.typography.labelLarge)
                            Text(SimFormatters.formatSimUsd(prod.price), style = MaterialTheme.typography.labelSmall, color = MetallicGold)
                        }
                        TextButton(onClick = { onAddToCart(prod) }) { Text("+ Cart") }
                    }
                }
                item {
                    Text("Reviews (${reviews.size})", style = MaterialTheme.typography.titleMedium)
                }
                items(reviews.take(5), key = { it.id }) { rev ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("${rev.buyerName} • ${rev.rating}/5 ★", style = MaterialTheme.typography.labelLarge, color = MetallicGold)
                            if (rev.comment.isNotBlank()) {
                                Text(rev.comment, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun CartCheckoutDialog(
    cartItems: List<CartItem>,
    cards: List<VirtualCard>,
    unlimitedMode: Boolean,
    onUpdateQty: (String, Int) -> Unit,
    onDismiss: () -> Unit,
    onProceedCheckout: (VirtualCard, List<CartItem>) -> Unit
) {
    // Group checkout by first seller in cart for clean order creation
    val firstSellerId = cartItems.firstOrNull()?.product?.sellerId
    val sellerItems = cartItems.filter { it.product.sellerId == firstSellerId }
    val total = sellerItems.sumOf { it.product.price * it.quantity }
    var selectedCard by remember { mutableStateOf(cards.firstOrNull { it.cardActive } ?: cards.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vault Card Checkout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SimDisclaimerBanner("VIRTUAL CHECKOUT • NO REAL PAYMENT PROCESSED")
                sellerItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.product.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(SimFormatters.formatSimUsd(item.product.price * item.quantity), style = MaterialTheme.typography.labelSmall, color = MetallicGold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onUpdateQty(item.product.id, -1) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
                            }
                            Text("${item.quantity}", style = MaterialTheme.typography.labelLarge)
                            IconButton(onClick = { onUpdateQty(item.product.id, 1) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Increase quantity")
                            }
                        }
                    }
                }
                Text("Select Virtual Card:", style = MaterialTheme.typography.labelLarge)
                cards.forEach { card ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (card.id == selectedCard?.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
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
                            SimFormatters.formatSimUsd(card.balance, card.unlimitedBalance || unlimitedMode),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                Text(
                    "Total: ${SimFormatters.formatSimUsd(total)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldSuccess
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val card = selectedCard ?: return@Button
                    if (card.cardActive && sellerItems.isNotEmpty()) {
                        onProceedCheckout(card, sellerItems)
                    }
                },
                enabled = selectedCard != null && selectedCard!!.cardActive && sellerItems.isNotEmpty(),
                modifier = Modifier.testTag("confirm_cart_checkout_btn")
            ) {
                Text("Pay Now (Demo)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun BuyCardCustomizationDialog(
    tier: CardCatalogTier,
    defaultHolder: String,
    onDismiss: () -> Unit,
    onConfirmBuy: (String, String) -> Unit
) {
    var holderName by remember { mutableStateOf(defaultHolder.uppercase()) }
    var accentHex by remember { mutableStateOf(tier.defaultAccentHex) }
    val swatches = listOf(tier.defaultAccentHex, "#F5C451", "#38BDF8", "#10B981", "#A855F7")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize ${tier.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Cost: ${tier.coinPrice} Vault Coins • Account Balance: ${SimFormatters.formatSimUsd(tier.startingBalance)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = holderName,
                    onValueChange = { holderName = it },
                    label = { Text("Cardholder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Card Glow Accent:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    swatches.distinct().forEach { hex ->
                        val c = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
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
            }
        },
        confirmButton = {
            Button(onClick = { onConfirmBuy(holderName, accentHex) }) {
                Text("Mint Card (${tier.coinPrice} Coins)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
