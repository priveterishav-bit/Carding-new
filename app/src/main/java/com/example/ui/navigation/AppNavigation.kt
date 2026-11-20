package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.UiState
import com.example.data.model.VirtualCard
import com.example.data.repository.AccountSessionPrefs
import com.example.data.repository.CloudSimulatorBackend
import com.example.data.repository.LocalAccountMode
import com.example.data.repository.LocalSimulatorBackend
import com.example.data.repository.MarketplaceRepository
import com.example.data.repository.SupportAiRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.WalletRepository
import com.example.ui.auth.AccountOptionsDialog
import com.example.ui.components.PinConfirmDialog
import com.example.ui.components.PinLockGateScreen
import com.example.ui.components.VaultCardDetailsBottomSheet
import com.example.ui.screens.AddVirtualMoneyDialog
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.MyCardsScreen
import com.example.ui.screens.OrderTrackingScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.PurchaseSuccessScreen
import com.example.ui.screens.QuickVirtualPaymentDialog
import com.example.ui.screens.ScanAndPayScreen
import com.example.ui.screens.SellerDashboardScreen
import com.example.ui.screens.SupportCenterScreen
import com.example.ui.theme.VaultPlayTheme
import com.example.ui.viewmodel.SimulatorViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    SCAN_PAY("Scan & Pay", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner, "nav_scan_pay"),
    MARKETPLACE("Market", Icons.Filled.Storefront, Icons.Outlined.Storefront, "nav_marketplace"),
    CARDS("Cards", Icons.Filled.CreditCard, Icons.Outlined.CreditCard, "nav_cards"),
    PROFILE("Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "nav_profile")
}

private enum class SecondaryRoute {
    NONE,
    ORDER_TRACKING,
    SUPPORT_CENTER
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun VaultPlayApp() {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val prefsRepo = remember(appContext) { UserPreferencesRepository(appContext) }
    val darkMode by prefsRepo.darkModeFlow.collectAsStateWithLifecycle(initialValue = true)
    val accountSession by prefsRepo.accountSessionFlow.collectAsStateWithLifecycle(initialValue = AccountSessionPrefs())
    val authUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val sessionKey = if (authUser != null) {
        "cloud_${authUser!!.uid}"
    } else {
        accountSession.localAccountKey
    }

    VaultPlayTheme(darkTheme = darkMode) {
        key(sessionKey) {
            SimulatorShell(
                sessionKey = sessionKey,
                cloudUser = authUser,
                localSession = accountSession,
                darkMode = darkMode,
                prefsRepo = prefsRepo
            )
        }
    }
}

@Composable
private fun SimulatorShell(
    sessionKey: String,
    cloudUser: FirebaseUser?,
    localSession: AccountSessionPrefs,
    darkMode: Boolean,
    prefsRepo: UserPreferencesRepository
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val scope = rememberCoroutineScope()

    val currentUserId = cloudUser?.uid ?: localSession.localAccountKey
    val initialDisplayName = cloudUser?.displayName?.takeIf { it.isNotBlank() }
        ?: localSession.effectiveDisplayName

    val accountStatusLabel = when {
        cloudUser != null -> "Google Cloud Sync"
        localSession.mode == LocalAccountMode.PHONE && localSession.phoneNumber.isNotBlank() ->
            "Phone: ${localSession.phoneNumber}"
        else -> "Guest (No Login)"
    }
    val isGuestMode = cloudUser == null && localSession.mode == LocalAccountMode.GUEST

    val viewModel: SimulatorViewModel = viewModel(
        key = sessionKey,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val backend = if (cloudUser != null) {
                    val db = FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
                    CloudSimulatorBackend(
                        currentUserId = cloudUser.uid,
                        walletRepo = WalletRepository(db),
                        marketRepo = MarketplaceRepository(db)
                    )
                } else {
                    LocalSimulatorBackend(
                        context = appContext,
                        accountKey = localSession.localAccountKey
                    )
                }
                return SimulatorViewModel(
                    backend = backend,
                    supportAiRepo = SupportAiRepository(),
                    prefsRepo = prefsRepo,
                    currentUserId = currentUserId,
                    initialDisplayName = initialDisplayName
                ) as T
            }
        }
    )

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val cardsState by viewModel.cardsState.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val ticketsState by viewModel.ticketsState.collectAsStateWithLifecycle()
    val sellersState by viewModel.sellersState.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val buyerOrdersState by viewModel.buyerOrdersState.collectAsStateWithLifecycle()
    val sellerOrdersState by viewModel.sellerOrdersState.collectAsStateWithLifecycle()
    val reviewsState by viewModel.reviewsState.collectAsStateWithLifecycle()

    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val latestReceipt by viewModel.latestReceipt.collectAsStateWithLifecycle()
    val newlyPurchasedCard by viewModel.newlyPurchasedCard.collectAsStateWithLifecycle()
    val selectedOrderId by viewModel.selectedOrderId.collectAsStateWithLifecycle()
    val sellerMode by viewModel.sellerMode.collectAsStateWithLifecycle()
    val pinUnlocked by viewModel.pinUnlockedThisSession.collectAsStateWithLifecycle()
    val supportMessages by viewModel.supportMessages.collectAsStateWithLifecycle()
    val aiStreamingText by viewModel.aiStreamingText.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val mySellerProfile by viewModel.mySellerProfile.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var marketplaceInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var secondaryRoute by rememberSaveable { mutableStateOf(SecondaryRoute.NONE) }
    var autoStartCameraTrigger by rememberSaveable { mutableIntStateOf(0) }
    var focusedCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAccountOptions by remember { mutableStateOf(false) }
    var purchaseQuickPayTarget by remember { mutableStateOf<VirtualCard?>(null) }
    var purchaseTopUpTarget by remember { mutableStateOf<VirtualCard?>(null) }
    var pendingPurchasePayPin by remember { mutableStateOf<Triple<VirtualCard, Double, String>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        val msg = statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearStatusMessage()
    }

    val switchToGuestMode: () -> Unit = {
        scope.launch {
            withContext(NonCancellable) {
                if (cloudUser != null) {
                    try {
                        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                    } catch (_: Exception) {
                    }
                    Firebase.auth.signOut()
                }
                prefsRepo.switchToGuestMode()
            }
        }
    }

    when (val profileRes = userProfileState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("Initializing Vault Card...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Unable to load vault wallet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        profileRes.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = switchToGuestMode) {
                        Text("Switch to Guest Account")
                    }
                }
            }
        }
        is UiState.Success -> {
            val profile = profileRes.data
            if (profile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                return
            }

            if (profile.pinEnabled && !pinUnlocked) {
                PinLockGateScreen(
                    expectedPin = profile.simPin,
                    onUnlocked = { viewModel.unlockPinSession() }
                )
                return
            }

            val receipt = latestReceipt
            if (receipt != null) {
                BackHandler { viewModel.clearLatestReceipt() }
                PurchaseSuccessScreen(
                    receipt = receipt,
                    onTrackOrder = { orderId ->
                        viewModel.clearLatestReceipt()
                        viewModel.selectOrderForTracking(orderId)
                        secondaryRoute = SecondaryRoute.ORDER_TRACKING
                    },
                    onDone = { viewModel.clearLatestReceipt() }
                )
                return
            }

            val cards = (cardsState as? UiState.Success)?.data ?: emptyList()
            val transactions = (transactionsState as? UiState.Success)?.data ?: emptyList()
            val tickets = (ticketsState as? UiState.Success)?.data ?: emptyList()
            val sellers = (sellersState as? UiState.Success)?.data ?: emptyList()
            val products = (productsState as? UiState.Success)?.data ?: emptyList()
            val buyerOrders = (buyerOrdersState as? UiState.Success)?.data ?: emptyList()
            val sellerOrders = (sellerOrdersState as? UiState.Success)?.data ?: emptyList()
            val reviews = (reviewsState as? UiState.Success)?.data ?: emptyList()

            val showMainChrome = !sellerMode && secondaryRoute == SecondaryRoute.NONE

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!useRail && showMainChrome) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 4.dp
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            if (tab == MainTab.MARKETPLACE && currentTab != MainTab.MARKETPLACE) {
                                                marketplaceInitialTab = 0
                                            }
                                            currentTab = tab
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        ),
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useRail && showMainChrome) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = {
                                            if (tab == MainTab.MARKETPLACE && currentTab != MainTab.MARKETPLACE) {
                                                marketplaceInitialTab = 0
                                            }
                                            currentTab = tab
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        ),
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            when {
                                secondaryRoute == SecondaryRoute.ORDER_TRACKING -> {
                                    BackHandler { secondaryRoute = SecondaryRoute.NONE }
                                    OrderTrackingScreen(
                                        orders = buyerOrders,
                                        sellers = sellers,
                                        selectedOrderId = selectedOrderId,
                                        onSelectOrder = { viewModel.selectOrderForTracking(it) },
                                        onAdvanceDemoOrder = { viewModel.advanceOrderStage(it) },
                                        onCancelOrder = { viewModel.cancelOrder(it) },
                                        onRequestRefund = { order, reason -> viewModel.requestRefund(order, reason) },
                                        onAutoApproveDemoRefund = { order ->
                                            viewModel.resolveRefund(order, approve = true, response = "Demo store auto-approved simulated refund.")
                                        },
                                        onSubmitReview = { seller, orderId, rating, comment ->
                                            viewModel.submitReview(seller, orderId, rating, comment)
                                        },
                                        onBack = { secondaryRoute = SecondaryRoute.NONE }
                                    )
                                }

                                secondaryRoute == SecondaryRoute.SUPPORT_CENTER -> {
                                    BackHandler { secondaryRoute = SecondaryRoute.NONE }
                                    SupportCenterScreen(
                                        messages = supportMessages,
                                        streamingText = aiStreamingText,
                                        aiError = aiError,
                                        tickets = tickets,
                                        onSendAiMessage = { viewModel.sendSupportChatMessage(it) },
                                        onRequestHumanNotice = { viewModel.appendHumanEscalationNotice() },
                                        onCreateTicket = { subject, category, desc ->
                                            viewModel.createSupportTicket(subject, category, desc)
                                        },
                                        onAdvanceTicket = { viewModel.advanceSupportTicket(it) },
                                        onBack = { secondaryRoute = SecondaryRoute.NONE }
                                    )
                                }

                                sellerMode -> {
                                    BackHandler { viewModel.setSellerMode(false) }
                                    SellerDashboardScreen(
                                        sellerProfile = mySellerProfile,
                                        products = products,
                                        incomingOrders = sellerOrders,
                                        onSaveStore = { name, desc, cat, tone ->
                                            viewModel.saveSellerStore(name, desc, cat, tone)
                                        },
                                        onSaveProduct = { existing, name, desc, cat, price, stock ->
                                            viewModel.saveSellerProduct(existing, name, desc, cat, price, stock)
                                        },
                                        onAdvanceOrderStage = { viewModel.advanceOrderStage(it) },
                                        onResolveRefund = { order, approve, response ->
                                            viewModel.resolveRefund(order, approve, response)
                                        },
                                        onSwitchToBuyerMode = { viewModel.setSellerMode(false) }
                                    )
                                }

                                else -> {
                                    when (currentTab) {
                                        MainTab.HOME -> HomeDashboardScreen(
                                            profile = profile,
                                            cards = cards,
                                            transactions = transactions,
                                            accountStatusLabel = accountStatusLabel,
                                            onOpenAccountSwitch = { showAccountOptions = true },
                                            onToggleUnlimited = { viewModel.toggleUnlimitedMode(it) },
                                            onToggleCardActive = { viewModel.toggleCardActive(it) },
                                            onAddMoney = { card, amt -> viewModel.addVirtualMoney(card, amt) },
                                            onQuickPay = { card, amt, merchant, key ->
                                                viewModel.executeVirtualPayment(
                                                    card = card,
                                                    amount = amt,
                                                    merchantName = merchant,
                                                    idempotencyKey = key,
                                                    onSuccess = {}
                                                )
                                            },
                                            onResetBalances = { viewModel.resetSimulator() },
                                            onClaimDailyCoins = { viewModel.claimDailyCoins() },
                                            onScanQrClick = {
                                                autoStartCameraTrigger += 1
                                                currentTab = MainTab.SCAN_PAY
                                            },
                                            onNavigateToCards = { currentTab = MainTab.CARDS },
                                            onNavigateToCardStore = {
                                                marketplaceInitialTab = 1
                                                currentTab = MainTab.MARKETPLACE
                                            },
                                            onNavigateToSupport = { secondaryRoute = SecondaryRoute.SUPPORT_CENTER },
                                            onSwitchToSellerMode = { viewModel.setSellerMode(true) }
                                        )

                                        MainTab.SCAN_PAY -> ScanAndPayScreen(
                                            profile = profile,
                                            cards = cards,
                                            sellers = sellers,
                                            autoStartCameraTrigger = autoStartCameraTrigger,
                                            onExecuteDirectPayment = { card, amt, merchantLabel, seller, key ->
                                                viewModel.executeVirtualPayment(
                                                    card = card,
                                                    amount = amt,
                                                    merchantName = merchantLabel,
                                                    seller = seller,
                                                    idempotencyKey = key,
                                                    onSuccess = {}
                                                )
                                            }
                                        )

                                        MainTab.MARKETPLACE -> MarketplaceScreen(
                                            profile = profile,
                                            cards = cards,
                                            sellers = sellers,
                                            products = products,
                                            reviews = reviews,
                                            cartItems = cartItems,
                                            initialSubTab = marketplaceInitialTab,
                                            onAddToCart = { viewModel.addToCart(it) },
                                            onUpdateCartQty = { prodId, delta -> viewModel.updateCartQuantity(prodId, delta) },
                                            onCheckoutCart = { card, seller, items, key ->
                                                val total = items.sumOf { it.product.price * it.quantity }
                                                viewModel.executeVirtualPayment(
                                                    card = card,
                                                    amount = total,
                                                    merchantName = seller.storeName,
                                                    seller = seller,
                                                    cartItems = items,
                                                    idempotencyKey = key,
                                                    onSuccess = {}
                                                )
                                            },
                                            onBuyCard = { tier, holderName, accentHex ->
                                                viewModel.buyCardFromStore(tier, holderName, accentHex)
                                            },
                                            onOpenOrderTracking = {
                                                secondaryRoute = SecondaryRoute.ORDER_TRACKING
                                            }
                                        )

                                        MainTab.CARDS -> MyCardsScreen(
                                            profile = profile,
                                            cards = cards,
                                            focusedCardId = focusedCardId,
                                            onFocusConsumed = { focusedCardId = null },
                                            onToggleCardActive = { viewModel.toggleCardActive(it) },
                                            onAddMoney = { card, amt -> viewModel.addVirtualMoney(card, amt) },
                                            onCustomizeAndUpgrade = { card, holder, hex, upgrade ->
                                                viewModel.customizeAndUpgradeCard(card, holder, hex, upgrade)
                                            },
                                            onQuickPay = { card, amt, merchant, key ->
                                                viewModel.executeVirtualPayment(
                                                    card = card,
                                                    amount = amt,
                                                    merchantName = merchant,
                                                    idempotencyKey = key,
                                                    onSuccess = {}
                                                )
                                            },
                                            onOpenCardStore = {
                                                marketplaceInitialTab = 1
                                                currentTab = MainTab.MARKETPLACE
                                            }
                                        )

                                        MainTab.PROFILE -> ProfileAndSettingsScreen(
                                            profile = profile,
                                            accountStatusLabel = accountStatusLabel,
                                            isGuestMode = isGuestMode,
                                            cards = cards,
                                            orders = buyerOrders,
                                            transactions = transactions,
                                            tickets = tickets,
                                            darkMode = darkMode,
                                            onToggleDarkMode = { viewModel.setDarkMode(it) },
                                            onUpdateSettings = { pinOn, pin, soundOn ->
                                                viewModel.updateSettings(pinOn, pin, soundOn)
                                            },
                                            onClaimDailyCoins = { viewModel.claimDailyCoins() },
                                            onResetSimulator = { viewModel.resetSimulator() },
                                            onSwitchToSellerMode = { viewModel.setSellerMode(true) },
                                            onOpenCards = { currentTab = MainTab.CARDS },
                                            onOpenOrders = { secondaryRoute = SecondaryRoute.ORDER_TRACKING },
                                            onOpenSupport = { secondaryRoute = SecondaryRoute.SUPPORT_CENTER },
                                            onOpenAccountSwitch = { showAccountOptions = true },
                                            onSwitchToGuest = switchToGuestMode
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            newlyPurchasedCard?.let { purchased ->
                val liveCard = cards.firstOrNull { it.id == purchased.id } ?: purchased
                VaultCardDetailsBottomSheet(
                    card = liveCard,
                    globalUnlimitedMode = profile.unlimitedMode,
                    onDismiss = { viewModel.consumeNewlyPurchasedCard() },
                    onPayClick = { target ->
                        viewModel.consumeNewlyPurchasedCard()
                        purchaseQuickPayTarget = target
                    },
                    onTopUpClick = { target ->
                        viewModel.consumeNewlyPurchasedCard()
                        purchaseTopUpTarget = target
                    },
                    onToggleActiveClick = { target ->
                        viewModel.toggleCardActive(target)
                    },
                    secondaryActionLabel = "Open 3D Cards",
                    onSecondaryActionClick = { target ->
                        viewModel.consumeNewlyPurchasedCard()
                        focusedCardId = target.id
                        currentTab = MainTab.CARDS
                    }
                )
            }

            purchaseTopUpTarget?.let { target ->
                AddVirtualMoneyDialog(
                    cards = if (cards.any { it.id == target.id }) cards else listOf(target) + cards,
                    initialCard = target,
                    onDismiss = { purchaseTopUpTarget = null },
                    onConfirm = { card, amt ->
                        purchaseTopUpTarget = null
                        viewModel.addVirtualMoney(card, amt)
                    }
                )
            }

            purchaseQuickPayTarget?.let { target ->
                QuickVirtualPaymentDialog(
                    cards = if (cards.any { it.id == target.id }) cards else listOf(target) + cards,
                    initialCard = target,
                    onDismiss = { purchaseQuickPayTarget = null },
                    onConfirm = { card, amt, merchant ->
                        purchaseQuickPayTarget = null
                        if (profile.pinEnabled) {
                            pendingPurchasePayPin = Triple(card, amt, merchant)
                        } else {
                            viewModel.executeVirtualPayment(
                                card = card,
                                amount = amt,
                                merchantName = merchant,
                                idempotencyKey = "newcard_${System.currentTimeMillis()}",
                                onSuccess = {}
                            )
                        }
                    }
                )
            }

            pendingPurchasePayPin?.let { (card, amt, merchant) ->
                PinConfirmDialog(
                    expectedPin = profile.simPin,
                    onDismiss = { pendingPurchasePayPin = null },
                    onConfirmed = {
                        pendingPurchasePayPin = null
                        viewModel.executeVirtualPayment(
                            card = card,
                            amount = amt,
                            merchantName = merchant,
                            idempotencyKey = "newcard_${System.currentTimeMillis()}",
                            onSuccess = {}
                        )
                    }
                )
            }

            if (showAccountOptions) {
                AccountOptionsDialog(
                    currentModeLabel = accountStatusLabel,
                    isCloudSignedIn = cloudUser != null,
                    initialPhone = localSession.phoneNumber,
                    initialName = localSession.phoneDisplayName,
                    onContinueGuest = switchToGuestMode,
                    onLoginWithPhone = { phone, name ->
                        scope.launch {
                            if (cloudUser != null) {
                                try {
                                    CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                                } catch (_: Exception) {
                                }
                                Firebase.auth.signOut()
                            }
                            prefsRepo.setPhoneProfile(phone, name)
                        }
                    },
                    onGoogleCloudSuccess = {},
                    onSignOutGoogle = switchToGuestMode,
                    onDismiss = { showAccountOptions = false }
                )
            }
        }
    }
}
