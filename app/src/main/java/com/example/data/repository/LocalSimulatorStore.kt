package com.example.data.repository

import android.content.Context
import com.example.core.SimFormatters
import com.example.core.SimQrCodec
import com.example.data.model.CardCatalogTier
import com.example.data.model.CartItem
import com.example.data.model.MarketplaceProduct
import com.example.data.model.OrderStage
import com.example.data.model.PurchaseSuccessReceipt
import com.example.data.model.SellerProfile
import com.example.data.model.SellerReview
import com.example.data.model.SimulatorOrder
import com.example.data.model.SupportTicket
import com.example.data.model.TrackingEvent
import com.example.data.model.TransactionRecord
import com.example.data.model.UserProfile
import com.example.data.model.VirtualCard
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Date
import kotlin.math.abs

class LocalSimulatorBackend(
    context: Context,
    private val accountKey: String
) : SimulatorBackend {

    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private val walletFile = File(appContext.filesDir, "vaultplay_wallet_${accountKey.replace(Regex("[^a-zA-Z0-9_]"), "_")}.json")
    private val marketFile = File(appContext.filesDir, "vaultplay_local_marketplace.json")

    private val profileFlow = MutableStateFlow<UserProfile?>(null)
    private val cardsFlow = MutableStateFlow<List<VirtualCard>>(emptyList())
    private val transactionsFlow = MutableStateFlow<List<TransactionRecord>>(emptyList())
    private val ticketsFlow = MutableStateFlow<List<SupportTicket>>(emptyList())

    private val sellersFlow = MutableStateFlow<List<SellerProfile>>(emptyList())
    private val productsFlow = MutableStateFlow<List<MarketplaceProduct>>(emptyList())
    private val ordersFlow = MutableStateFlow<List<SimulatorOrder>>(emptyList())
    private val reviewsFlow = MutableStateFlow<List<SellerReview>>(emptyList())

    override fun observeUserProfile(): Flow<UserProfile?> = profileFlow
    override fun observeCards(): Flow<List<VirtualCard>> = cardsFlow
    override fun observeTransactions(): Flow<List<TransactionRecord>> = transactionsFlow
    override fun observeTickets(): Flow<List<SupportTicket>> = ticketsFlow
    override fun observeSellers(): Flow<List<SellerProfile>> = sellersFlow
    override fun observeProducts(): Flow<List<MarketplaceProduct>> = productsFlow
    override fun observeBuyerOrders(): Flow<List<SimulatorOrder>> =
        ordersFlow.map { list -> list.filter { it.buyerUid == accountKey } }
    override fun observeSellerOrders(): Flow<List<SimulatorOrder>> =
        ordersFlow.map { list -> list.filter { it.sellerOwnerUid == accountKey } }
    override fun observeReviews(): Flow<List<SellerReview>> = reviewsFlow

    override suspend fun bootstrap(initialDisplayName: String) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                loadOrSeedWalletLocked(initialDisplayName)
                loadOrSeedMarketplaceLocked()
            }
        }
    }

    override suspend fun toggleUnlimitedMode(enabled: Boolean): Result<Unit> = mutex.withLock {
        val p = profileFlow.value ?: return@withLock Result.failure(IllegalStateException("Wallet initializing"))
        profileFlow.value = p.copy(unlimitedMode = enabled, updatedAt = Timestamp.now())
        persistWalletLocked()
        Result.success(Unit)
    }

    override suspend fun addVirtualMoney(card: VirtualCard, amount: Double): Result<Unit> = mutex.withLock {
        if (amount <= 0.0 || amount > 10_000_000.0) {
            return@withLock Result.failure(IllegalArgumentException("Amount must be between $1 and $10,000,000 SIM"))
        }
        val cards = cardsFlow.value
        cardsFlow.value = cards.map {
            if (it.id == card.id) it.copy(balance = it.balance + amount, updatedAt = Timestamp.now()) else it
        }
        val txn = TransactionRecord(
            id = "topup_${System.currentTimeMillis()}",
            userId = accountKey,
            transactionRef = SimFormatters.generateRefCode("TOP"),
            idempotencyKey = "topup_${System.currentTimeMillis()}",
            type = "TOP_UP",
            amount = amount,
            currencyLabel = "SIM USD",
            merchantName = "Virtual Vault Top-Up",
            sellerId = "",
            sellerName = "Vault Card Mint",
            cardId = card.id,
            cardSummary = "${card.cardTitle} •••• ${card.cardNumber.takeLast(4)}",
            orderId = "",
            status = "SIMULATED_SUCCESS",
            note = "Simulated virtual dollar mint",
            createdAt = Timestamp.now()
        )
        transactionsFlow.value = listOf(txn) + transactionsFlow.value
        persistWalletLocked()
        Result.success(Unit)
    }

    override suspend fun resetSimulatorBalances(cards: List<VirtualCard>): Result<Unit> = mutex.withLock {
        val p = profileFlow.value ?: return@withLock Result.success(Unit)
        profileFlow.value = p.copy(
            unlimitedMode = false,
            unlimitedBonusBalance = 0.0,
            totalSpent = 0.0,
            updatedAt = Timestamp.now()
        )
        cardsFlow.value = cardsFlow.value.map { card ->
            card.copy(balance = 0.0, unlimitedBalance = false, updatedAt = Timestamp.now())
        }
        persistWalletLocked()
        Result.success(Unit)
    }

    override suspend fun buyCardFromStore(
        profile: UserProfile,
        tier: CardCatalogTier,
        customHolderName: String,
        customAccentHex: String
    ): Result<VirtualCard> = mutex.withLock {
        val cur = profileFlow.value ?: return@withLock Result.failure(IllegalStateException("Profile missing"))
        if (cur.coins < tier.coinPrice) {
            return@withLock Result.failure(IllegalStateException("Not enough vault coins. Claim daily coins or complete payments!"))
        }
        val cleanHolder = customHolderName.trim().ifEmpty { cur.displayName }.uppercase().take(48)
        val cardId = "card_${System.currentTimeMillis()}"
        val newCard = VirtualCard(
            id = cardId,
            userId = accountKey,
            tierKey = tier.tierKey,
            cardTitle = tier.title,
            cardNetworkLabel = tier.networkLabel,
            cardholderName = cleanHolder,
            cardNumber = SimFormatters.generateDemoCardNumber(tier.binPrefix),
            expiryDisplay = SimFormatters.generateDemoExpiry(),
            cvvDisplay = SimFormatters.generateDemoCvv(),
            balance = tier.startingBalance,
            unlimitedBalance = tier.unlimited,
            cardActive = true,
            accentColorHex = customAccentHex,
            upgradeLevel = 1L,
            animatedStyle = tier.isAnimated,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        val txnId = "cardbuy_${System.currentTimeMillis()}"
        val txn = TransactionRecord(
            id = txnId,
            userId = accountKey,
            transactionRef = SimFormatters.generateRefCode("CRD"),
            idempotencyKey = txnId,
            type = "CARD_PURCHASE",
            amount = tier.startingBalance,
            currencyLabel = "SIM USD",
            merchantName = "Card Store • ${tier.title}",
            sellerId = "",
            sellerName = "Vault Card Mint",
            cardId = cardId,
            cardSummary = tier.title,
            orderId = "",
            status = "SIMULATED_SUCCESS",
            note = "Purchased for ${tier.coinPrice} vault coins",
            createdAt = Timestamp.now()
        )
        profileFlow.value = cur.copy(coins = cur.coins - tier.coinPrice, updatedAt = Timestamp.now())
        cardsFlow.value = listOf(newCard) + cardsFlow.value
        transactionsFlow.value = listOf(txn) + transactionsFlow.value
        persistWalletLocked()
        Result.success(newCard)
    }

    override suspend fun toggleCardActive(cardId: String, active: Boolean): Result<Unit> = mutex.withLock {
        cardsFlow.value = cardsFlow.value.map {
            if (it.id == cardId) it.copy(cardActive = active, updatedAt = Timestamp.now()) else it
        }
        persistWalletLocked()
        Result.success(Unit)
    }

    override suspend fun customizeAndUpgradeCard(
        profile: UserProfile,
        card: VirtualCard,
        newHolderName: String,
        newAccentHex: String,
        upgrade: Boolean
    ): Result<Unit> = mutex.withLock {
        val cur = profileFlow.value ?: return@withLock Result.failure(IllegalStateException("Profile missing"))
        val upgradeCost = 200L
        if (upgrade && cur.coins < upgradeCost) {
            return@withLock Result.failure(IllegalStateException("Upgrade requires $upgradeCost Vault Coins."))
        }
        if (upgrade && card.upgradeLevel >= 10L) {
            return@withLock Result.failure(IllegalStateException("Card is already at max level 10."))
        }
        val cleanHolder = newHolderName.trim().ifEmpty { card.cardholderName }.uppercase().take(48)
        if (upgrade) {
            profileFlow.value = cur.copy(coins = cur.coins - upgradeCost, updatedAt = Timestamp.now())
        }
        cardsFlow.value = cardsFlow.value.map {
            if (it.id == card.id) {
                val nextLevel = if (upgrade) (it.upgradeLevel + 1L).coerceAtMost(10L) else it.upgradeLevel
                it.copy(
                    cardholderName = cleanHolder,
                    accentColorHex = newAccentHex,
                    upgradeLevel = nextLevel,
                    animatedStyle = it.animatedStyle || nextLevel >= 2L,
                    updatedAt = Timestamp.now()
                )
            } else it
        }
        persistWalletLocked()
        Result.success(Unit)
    }

    override suspend fun claimDailyCoins(profile: UserProfile): Result<Long> = mutex.withLock {
        val cur = profileFlow.value ?: return@withLock Result.failure(IllegalStateException("Profile missing"))
        val today = SimFormatters.todayKey()
        if (cur.lastDailyClaimDay == today) {
            return@withLock Result.failure(IllegalStateException("Daily reward already claimed today! Come back tomorrow."))
        }
        val reward = 500L
        val txnId = "daily_$today"
        val txn = TransactionRecord(
            id = txnId,
            userId = accountKey,
            transactionRef = SimFormatters.generateRefCode("REW"),
            idempotencyKey = txnId,
            type = "DAILY_REWARD",
            amount = reward.toDouble(),
            currencyLabel = "SIM COINS",
            merchantName = "Daily Reward",
            sellerId = "",
            sellerName = "Vault Card Rewards",
            cardId = "",
            cardSummary = "Coin Vault",
            orderId = "",
            status = "SIMULATED_SUCCESS",
            note = "Daily +$reward Vault Coins",
            createdAt = Timestamp.now()
        )
        profileFlow.value = cur.copy(
            coins = cur.coins + reward,
            lastDailyClaimDay = today,
            updatedAt = Timestamp.now()
        )
        transactionsFlow.value = listOf(txn) + transactionsFlow.value
        persistWalletLocked()
        Result.success(reward)
    }

    override suspend fun updateSettings(pinEnabled: Boolean, simPin: String, soundEnabled: Boolean): Result<Unit> = mutex.withLock {
        val cur = profileFlow.value ?: return@withLock Result.failure(IllegalStateException("Profile missing"))
        val cleanPin = simPin.filter(Char::isDigit).take(4).padStart(4, '0')
        profileFlow.value = cur.copy(
            pinEnabled = pinEnabled,
            simPin = cleanPin,
            soundEnabled = soundEnabled,
            updatedAt = Timestamp.now()
        )
        persistWalletLocked()
        Result.success(Unit)
    }

    override suspend fun createSupportTicket(subject: String, category: String, description: String): Result<String> = mutex.withLock {
        val cleanSub = subject.trim().take(120)
        val cleanDesc = description.trim().take(1000)
        if (cleanSub.length < 3) return@withLock Result.failure(IllegalArgumentException("Subject must be at least 3 characters."))
        if (cleanDesc.length < 5) return@withLock Result.failure(IllegalArgumentException("Please describe your issue (5+ characters)."))
        val code = SimFormatters.generateRefCode("SUP")
        val ticket = SupportTicket(
            id = "tkt_${System.currentTimeMillis()}",
            userId = accountKey,
            ticketCode = code,
            subject = cleanSub,
            description = cleanDesc,
            category = category.take(40),
            status = "OPEN",
            resolutionNote = "Queued for Vault Card support review.",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        ticketsFlow.value = listOf(ticket) + ticketsFlow.value
        persistWalletLocked()
        Result.success(code)
    }

    override suspend fun updateSupportTicketStatus(ticketId: String, status: String, resolutionNote: String): Result<Unit> = mutex.withLock {
        ticketsFlow.value = ticketsFlow.value.map {
            if (it.id == ticketId) it.copy(status = status, resolutionNote = resolutionNote.take(500), updatedAt = Timestamp.now()) else it
        }
        persistWalletLocked()
        Result.success(Unit)
    }

    override suspend fun processVirtualPayment(
        profile: UserProfile,
        card: VirtualCard,
        amount: Double,
        merchantName: String,
        seller: SellerProfile?,
        cartItems: List<CartItem>,
        idempotencyKey: String
    ): Result<PurchaseSuccessReceipt> = mutex.withLock {
        val curProfile = profileFlow.value ?: profile
        val curCard = cardsFlow.value.firstOrNull { it.id == card.id } ?: card
        if (!curCard.cardActive) {
            return@withLock Result.failure(IllegalStateException("CARD DEACTIVATED: Reactivate this card in My Cards before making simulated purchases."))
        }
        if (amount <= 0.0) {
            return@withLock Result.failure(IllegalArgumentException("Payment amount must be greater than $0."))
        }
        val isUnlimited = curCard.unlimitedBalance || curProfile.unlimitedMode
        if (transactionsFlow.value.any { it.idempotencyKey == idempotencyKey }) {
            return@withLock Result.failure(IllegalStateException("Duplicate simulated payment prevented (Ref: $idempotencyKey)."))
        }

        val txnCode = SimFormatters.generateRefCode("TXN")
        val nowMillis = System.currentTimeMillis()
        val cardSummary = "${curCard.cardTitle} •••• ${curCard.cardNumber.takeLast(4)}"
        val resolvedMerchant = seller?.storeName ?: merchantName.trim().ifEmpty { "Direct QR Pay (No Details)" }.take(80)
        val itemsSummary = if (cartItems.isNotEmpty()) {
            cartItems.joinToString(", ") { "${it.quantity}x ${it.product.name}" }.take(240)
        } else {
            "Direct QR / UPI Payment • $resolvedMerchant".take(240)
        }
        val itemCount = cartItems.sumOf { it.quantity }.coerceAtLeast(1).toLong()

        val nextBalance = if (isUnlimited) curCard.balance else (curCard.balance + amount)
        cardsFlow.value = cardsFlow.value.map {
            if (it.id == curCard.id) it.copy(balance = nextBalance, updatedAt = Timestamp.now()) else it
        }
        profileFlow.value = curProfile.copy(
            totalSpent = curProfile.totalSpent + amount,
            coins = curProfile.coins + 25L,
            updatedAt = Timestamp.now()
        )

        var orderId: String? = null
        var orderCode: String? = null
        if (seller != null && sellersFlow.value.any { it.id == seller.id }) {
            orderId = "ord_${System.currentTimeMillis()}"
            orderCode = SimFormatters.generateRefCode("ORD")
            sellersFlow.value = sellersFlow.value.map {
                if (it.id == seller.id) {
                    it.copy(
                        virtualEarnings = it.virtualEarnings + amount,
                        completedOrdersCount = it.completedOrdersCount + 1L,
                        updatedAt = Timestamp.now()
                    )
                } else it
            }
            val newOrder = SimulatorOrder(
                id = orderId,
                orderCode = orderCode,
                buyerUid = accountKey,
                buyerName = curProfile.displayName,
                sellerId = seller.id,
                sellerOwnerUid = seller.sellerOwnerUid,
                sellerName = seller.storeName,
                itemsSummary = itemsSummary,
                itemCount = itemCount,
                totalAmount = amount,
                cardId = curCard.id,
                cardSummary = cardSummary,
                transactionRef = txnCode,
                status = OrderStage.PAYMENT_SIMULATED.key,
                trackingEvents = listOf(
                    TrackingEvent(OrderStage.ORDER_PLACED.key, OrderStage.ORDER_PLACED.label, nowMillis),
                    TrackingEvent(OrderStage.PAYMENT_SIMULATED.key, OrderStage.PAYMENT_SIMULATED.label, nowMillis + 50L)
                ),
                refundStatus = "NONE",
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            ordersFlow.value = listOf(newOrder) + ordersFlow.value
        }

        val txn = TransactionRecord(
            id = idempotencyKey,
            userId = accountKey,
            transactionRef = txnCode,
            idempotencyKey = idempotencyKey,
            type = "PAYMENT",
            amount = amount,
            currencyLabel = "SIM USD",
            merchantName = resolvedMerchant,
            sellerId = seller?.id ?: "",
            sellerName = seller?.storeName ?: resolvedMerchant,
            cardId = curCard.id,
            cardSummary = cardSummary,
            orderId = orderId ?: "",
            status = "SIMULATED_SUCCESS",
            note = itemsSummary.take(200),
            createdAt = Timestamp.now()
        )
        transactionsFlow.value = listOf(txn) + transactionsFlow.value

        persistWalletLocked()
        persistMarketplaceLocked()

        Result.success(
            PurchaseSuccessReceipt(
                transactionRef = txnCode,
                orderId = orderId,
                orderCode = orderCode,
                amount = amount,
                sellerName = resolvedMerchant,
                buyerName = curProfile.displayName,
                cardSummary = cardSummary,
                timestampMillis = nowMillis,
                itemsSummary = itemsSummary
            )
        )
    }

    override suspend fun createOrUpdateSellerStore(
        existingSeller: SellerProfile?,
        storeName: String,
        description: String,
        category: String,
        logoToneHex: String
    ): Result<SellerProfile> = mutex.withLock {
        val cleanName = storeName.trim().take(60)
        if (cleanName.length < 2) return@withLock Result.failure(IllegalArgumentException("Store name must be at least 2 characters."))
        val sellerId = existingSeller?.id?.takeIf { it.isNotBlank() } ?: "seller_${accountKey.take(20)}"
        val qrPayload = SimQrCodec.encode(sellerId, cleanName)
        val updatedSeller = if (existingSeller == null) {
            val sellerCode = "SEL-${(10000 + abs(accountKey.hashCode()) % 89999)}"
            val created = SellerProfile(
                id = sellerId,
                sellerOwnerUid = accountKey,
                sellerCode = sellerCode,
                storeName = cleanName,
                description = description.trim().take(300),
                category = category.trim().ifEmpty { "General" }.take(40),
                logoToneHex = logoToneHex,
                demoVerified = true,
                demoListed = true,
                qrPayload = qrPayload,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            sellersFlow.value = listOf(created) + sellersFlow.value
            profileFlow.value = profileFlow.value?.copy(sellerAccountId = sellerId, updatedAt = Timestamp.now())
            created
        } else {
            val edited = existingSeller.copy(
                storeName = cleanName,
                description = description.trim().take(300),
                category = category.trim().ifEmpty { "General" }.take(40),
                logoToneHex = logoToneHex,
                qrPayload = qrPayload,
                updatedAt = Timestamp.now()
            )
            sellersFlow.value = sellersFlow.value.map { if (it.id == sellerId) edited else it }
            edited
        }
        persistWalletLocked()
        persistMarketplaceLocked()
        Result.success(updatedSeller)
    }

    override suspend fun saveSellerProduct(
        seller: SellerProfile,
        existingProduct: MarketplaceProduct?,
        name: String,
        description: String,
        category: String,
        price: Double,
        stockCount: Long
    ): Result<Unit> = mutex.withLock {
        val cleanName = name.trim().take(80)
        if (cleanName.length < 2) return@withLock Result.failure(IllegalArgumentException("Product name too short."))
        if (price <= 0.0 || price > 10_000_000.0) return@withLock Result.failure(IllegalArgumentException("Enter a valid price."))
        if (existingProduct == null) {
            val prod = MarketplaceProduct(
                id = "prod_${System.currentTimeMillis()}",
                sellerId = seller.id,
                sellerOwnerUid = accountKey,
                storeName = seller.storeName,
                name = cleanName,
                description = description.trim().take(300),
                category = category.trim().ifEmpty { seller.category }.take(40),
                price = price,
                stockCount = stockCount.coerceAtLeast(1L),
                iconKey = "shopping",
                demoListed = true,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            productsFlow.value = listOf(prod) + productsFlow.value
        } else {
            productsFlow.value = productsFlow.value.map {
                if (it.id == existingProduct.id) {
                    it.copy(
                        name = cleanName,
                        description = description.trim().take(300),
                        category = category.trim().ifEmpty { seller.category }.take(40),
                        price = price,
                        stockCount = stockCount.coerceAtLeast(0L),
                        updatedAt = Timestamp.now()
                    )
                } else it
            }
        }
        persistMarketplaceLocked()
        Result.success(Unit)
    }

    override suspend fun advanceOrderStage(order: SimulatorOrder, nextStage: OrderStage): Result<Unit> = mutex.withLock {
        val nextEvents = (order.trackingEvents + TrackingEvent(nextStage.key, nextStage.label, System.currentTimeMillis())).takeLast(20)
        ordersFlow.value = ordersFlow.value.map {
            if (it.id == order.id) it.copy(status = nextStage.key, trackingEvents = nextEvents, updatedAt = Timestamp.now()) else it
        }
        persistMarketplaceLocked()
        Result.success(Unit)
    }

    override suspend fun cancelBuyerOrder(order: SimulatorOrder): Result<Unit> = mutex.withLock {
        if (order.status !in listOf("ORDER_PLACED", "PAYMENT_SIMULATED")) {
            return@withLock Result.failure(IllegalStateException("Order can only be cancelled before seller confirmation."))
        }
        val nextEvents = (order.trackingEvents + TrackingEvent("CANCELLED", "Cancelled by Buyer", System.currentTimeMillis())).takeLast(20)
        ordersFlow.value = ordersFlow.value.map {
            if (it.id == order.id) it.copy(status = "CANCELLED", trackingEvents = nextEvents, updatedAt = Timestamp.now()) else it
        }
        restoreRefundLocked(order, "Cancelled simulator order ${order.orderCode}")
        persistWalletLocked()
        persistMarketplaceLocked()
        Result.success(Unit)
    }

    override suspend fun requestOrderRefund(order: SimulatorOrder, reason: String): Result<Unit> = mutex.withLock {
        val cleanReason = reason.trim().ifEmpty { "Simulator refund request" }.take(300)
        ordersFlow.value = ordersFlow.value.map {
            if (it.id == order.id) it.copy(refundStatus = "REQUESTED", refundReason = cleanReason, updatedAt = Timestamp.now()) else it
        }
        persistMarketplaceLocked()
        Result.success(Unit)
    }

    override suspend fun resolveOrderRefund(order: SimulatorOrder, approve: Boolean, sellerResponse: String): Result<Unit> = mutex.withLock {
        val cleanReply = sellerResponse.trim().ifEmpty {
            if (approve) "Simulated refund approved. Balance restored." else "Refund declined by seller."
        }.take(300)
        val nextEvents = (order.trackingEvents + TrackingEvent(
            stage = if (approve) "REFUNDED" else order.status,
            label = if (approve) "Refund Approved & Credited" else "Refund Declined",
            timestampMillis = System.currentTimeMillis()
        )).takeLast(20)
        ordersFlow.value = ordersFlow.value.map {
            if (it.id == order.id) {
                it.copy(
                    status = if (approve) "REFUNDED" else it.status,
                    refundStatus = if (approve) "APPROVED" else "DECLINED",
                    sellerResponse = cleanReply,
                    trackingEvents = nextEvents,
                    updatedAt = Timestamp.now()
                )
            } else it
        }
        if (approve && order.buyerUid == accountKey) {
            restoreRefundLocked(order, cleanReply)
            persistWalletLocked()
        }
        persistMarketplaceLocked()
        Result.success(Unit)
    }

    override suspend fun restoreBuyerRefundIfNeeded(order: SimulatorOrder, reason: String): Result<Unit> = mutex.withLock {
        if (order.buyerUid != accountKey) return@withLock Result.success(Unit)
        restoreRefundLocked(order, reason)
        persistWalletLocked()
        Result.success(Unit)
    }

    override suspend fun submitSellerReview(
        profile: UserProfile,
        seller: SellerProfile,
        orderId: String,
        rating: Int,
        comment: String
    ): Result<Unit> = mutex.withLock {
        val bounded = rating.coerceIn(1, 5).toLong()
        sellersFlow.value = sellersFlow.value.map {
            if (it.id == seller.id) {
                it.copy(ratingSum = it.ratingSum + bounded, reviewCount = it.reviewCount + 1L, updatedAt = Timestamp.now())
            } else it
        }
        val rev = SellerReview(
            id = "rev_${System.currentTimeMillis()}",
            sellerId = seller.id,
            orderId = orderId,
            buyerUid = accountKey,
            buyerName = profile.displayName,
            rating = bounded,
            comment = comment.trim().take(300),
            demoListed = true,
            createdAt = Timestamp.now()
        )
        reviewsFlow.value = listOf(rev) + reviewsFlow.value
        persistMarketplaceLocked()
        Result.success(Unit)
    }

    private fun restoreRefundLocked(order: SimulatorOrder, reason: String) {
        val refundTxnId = "refund_${order.id}"
        if (transactionsFlow.value.any { it.idempotencyKey == refundTxnId }) return
        val txn = TransactionRecord(
            id = refundTxnId,
            userId = accountKey,
            transactionRef = SimFormatters.generateRefCode("RFD"),
            idempotencyKey = refundTxnId,
            type = "REFUND_CREDIT",
            amount = order.totalAmount,
            currencyLabel = "SIM USD",
            merchantName = "Refund • ${order.sellerName}",
            sellerId = order.sellerId,
            sellerName = order.sellerName,
            cardId = order.cardId,
            cardSummary = order.cardSummary,
            orderId = order.id,
            status = "REFUNDED",
            note = reason.take(200),
            createdAt = Timestamp.now()
        )
        transactionsFlow.value = listOf(txn) + transactionsFlow.value
    }

    private fun loadOrSeedWalletLocked(initialDisplayName: String) {
        if (walletFile.exists()) {
            try {
                val root = JSONObject(walletFile.readText())
                profileFlow.value = jsonToProfile(root.getJSONObject("profile"))
                val cardsArr = root.optJSONArray("cards") ?: JSONArray()
                cardsFlow.value = (0 until cardsArr.length()).map { jsonToCard(cardsArr.getJSONObject(it)) }
                val txnArr = root.optJSONArray("transactions") ?: JSONArray()
                transactionsFlow.value = (0 until txnArr.length()).map { jsonToTransaction(txnArr.getJSONObject(it)) }
                val tktArr = root.optJSONArray("tickets") ?: JSONArray()
                ticketsFlow.value = (0 until tktArr.length()).map { jsonToTicket(tktArr.getJSONObject(it)) }
                return
            } catch (_: Exception) {
            }
        }
        val cleanName = initialDisplayName.trim().ifEmpty { "Guest Member" }.take(60)
        val customerNum = 100000 + (abs(accountKey.hashCode()) % 899999)
        profileFlow.value = UserProfile(
            id = accountKey,
            userId = accountKey,
            displayName = cleanName,
            customerId = "VP-$customerNum",
            coins = 1500L,
            unlimitedMode = false,
            unlimitedBonusBalance = 0.0,
            totalSpent = 0.0,
            pinEnabled = false,
            simPin = "1234",
            soundEnabled = true,
            lastDailyClaimDay = "",
            sellerAccountId = "",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        cardsFlow.value = listOf(
            VirtualCard(
                id = "starter_default",
                userId = accountKey,
                tierKey = "STARTER",
                cardTitle = "Starter Card",
                cardNetworkLabel = "VAULT STARTER DEMO",
                cardholderName = cleanName.uppercase().take(48),
                cardNumber = SimFormatters.generateDemoCardNumber("9901"),
                expiryDisplay = SimFormatters.generateDemoExpiry(),
                cvvDisplay = SimFormatters.generateDemoCvv(),
                balance = 0.0,
                unlimitedBalance = false,
                cardActive = true,
                accentColorHex = "#38BDF8",
                upgradeLevel = 1L,
                animatedStyle = false,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        )
        transactionsFlow.value = emptyList()
        ticketsFlow.value = emptyList()
        persistWalletLocked()
    }

    private fun loadOrSeedMarketplaceLocked() {
        if (marketFile.exists()) {
            try {
                val root = JSONObject(marketFile.readText())
                val sArr = root.optJSONArray("sellers") ?: JSONArray()
                sellersFlow.value = (0 until sArr.length()).map { jsonToSeller(sArr.getJSONObject(it)) }
                val pArr = root.optJSONArray("products") ?: JSONArray()
                productsFlow.value = (0 until pArr.length()).map { jsonToProduct(pArr.getJSONObject(it)) }
                val oArr = root.optJSONArray("orders") ?: JSONArray()
                ordersFlow.value = (0 until oArr.length()).map { jsonToOrder(oArr.getJSONObject(it)) }
                val rArr = root.optJSONArray("reviews") ?: JSONArray()
                reviewsFlow.value = (0 until rArr.length()).map { jsonToReview(rArr.getJSONObject(it)) }
                if (sellersFlow.value.isNotEmpty()) return
            } catch (_: Exception) {
            }
        }

        val demoStores = listOf(
            Triple("demo_store_apex", "Apex Tech Gear", "Electronics") to Pair("#38BDF8", "Curated simulated gadgets, smartwatches & audio drops."),
            Triple("demo_store_luxe", "Luxe Watch Vault", "Luxury") to Pair("#F5C451", "Virtual collector timepieces & titanium accessories."),
            Triple("demo_store_neon", "Neon Studio Audio", "Audio") to Pair("#A855F7", "Studio headphones, DACs & spatial sound gear."),
            Triple("demo_store_obsidian", "Obsidian Apparel", "Lifestyle") to Pair("#10B981", "Minimalist monochrome techwear & travel packs.")
        )
        sellersFlow.value = demoStores.mapIndexed { idx, (idNameCat, toneDesc) ->
            val (id, name, cat) = idNameCat
            val (tone, desc) = toneDesc
            SellerProfile(
                id = id,
                sellerOwnerUid = "demo_platform",
                sellerCode = "SEL-DEMO0${idx + 1}",
                storeName = name,
                description = desc,
                category = cat,
                logoToneHex = tone,
                demoVerified = true,
                demoListed = true,
                qrPayload = SimQrCodec.encode(id, name),
                ratingSum = 24L,
                reviewCount = 5L,
                completedOrdersCount = 18L + idx * 7L,
                virtualEarnings = 4200.0 + idx * 1850.0,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        }
        val demoProducts = listOf(
            Triple("demo_prod_1", "demo_store_apex", "Apex Tech Gear") to Triple("Quantum Chrono Smartwatch", "AMOLED sapphire simulator edition watch.", 349.0),
            Triple("demo_prod_2", "demo_store_apex", "Apex Tech Gear") to Triple("AeroDock Wireless Pad", "Triple-coil anodized desk charger.", 129.0),
            Triple("demo_prod_3", "demo_store_luxe", "Luxe Watch Vault") to Triple("Aurelia Tourbillon Sim", "Gold-plated collector showcase timepiece.", 4850.0),
            Triple("demo_prod_4", "demo_store_luxe", "Luxe Watch Vault") to Triple("Carbon Valet Travel Case", "Hand-stitched simulated leather vault box.", 420.0),
            Triple("demo_prod_5", "demo_store_neon", "Neon Studio Audio") to Triple("Prism ANC Headphones", "Planar magnetic studio reference headset.", 599.0),
            Triple("demo_prod_6", "demo_store_neon", "Neon Studio Audio") to Triple("Pulse Tube Mini Amp", "Warm analog-style desk amplifier.", 280.0),
            Triple("demo_prod_7", "demo_store_obsidian", "Obsidian Apparel") to Triple("Stealth Modular Sling", "Weatherproof ballistic nylon pack.", 185.0),
            Triple("demo_prod_8", "demo_store_obsidian", "Obsidian Apparel") to Triple("Titanium Key Organizer", "Machined aerospace grade pocket tool.", 75.0)
        )
        productsFlow.value = demoProducts.mapIndexed { idx, (ids, details) ->
            val (prodId, sellerId, storeName) = ids
            val (prodName, desc, price) = details
            MarketplaceProduct(
                id = prodId,
                sellerId = sellerId,
                sellerOwnerUid = "demo_platform",
                storeName = storeName,
                name = prodName,
                description = desc,
                category = if (idx < 2) "Electronics" else if (idx < 4) "Luxury" else if (idx < 6) "Audio" else "Lifestyle",
                price = price,
                stockCount = 50L,
                iconKey = "shopping",
                demoListed = true,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
        }
        ordersFlow.value = emptyList()
        reviewsFlow.value = emptyList()
        persistMarketplaceLocked()
    }

    private fun persistWalletLocked() {
        try {
            val root = JSONObject().apply {
                profileFlow.value?.let { put("profile", profileToJson(it)) }
                put("cards", JSONArray().apply { cardsFlow.value.forEach { put(cardToJson(it)) } })
                put("transactions", JSONArray().apply { transactionsFlow.value.forEach { put(transactionToJson(it)) } })
                put("tickets", JSONArray().apply { ticketsFlow.value.forEach { put(ticketToJson(it)) } })
            }
            walletFile.writeText(root.toString())
        } catch (_: Exception) {
        }
    }

    private fun persistMarketplaceLocked() {
        try {
            val root = JSONObject().apply {
                put("sellers", JSONArray().apply { sellersFlow.value.forEach { put(sellerToJson(it)) } })
                put("products", JSONArray().apply { productsFlow.value.forEach { put(productToJson(it)) } })
                put("orders", JSONArray().apply { ordersFlow.value.forEach { put(orderToJson(it)) } })
                put("reviews", JSONArray().apply { reviewsFlow.value.forEach { put(reviewToJson(it)) } })
            }
            marketFile.writeText(root.toString())
        } catch (_: Exception) {
        }
    }

    private fun tsToMillis(ts: Timestamp?): Long = ts?.toDate()?.time ?: System.currentTimeMillis()
    private fun millisToTs(ms: Long): Timestamp = Timestamp(Date(if (ms > 0L) ms else System.currentTimeMillis()))

    private fun profileToJson(p: UserProfile) = JSONObject().apply {
        put("id", p.id); put("userId", p.userId); put("displayName", p.displayName); put("customerId", p.customerId)
        put("coins", p.coins); put("unlimitedMode", p.unlimitedMode); put("unlimitedBonusBalance", p.unlimitedBonusBalance)
        put("totalSpent", p.totalSpent); put("pinEnabled", p.pinEnabled); put("simPin", p.simPin)
        put("soundEnabled", p.soundEnabled); put("lastDailyClaimDay", p.lastDailyClaimDay); put("sellerAccountId", p.sellerAccountId)
        put("createdAt", tsToMillis(p.createdAt)); put("updatedAt", tsToMillis(p.updatedAt))
    }

    private fun jsonToProfile(o: JSONObject) = UserProfile(
        id = o.optString("id"), userId = o.optString("userId"), displayName = o.optString("displayName", "Guest Member"),
        customerId = o.optString("customerId", "VP-100001"), coins = o.optLong("coins", 1500L),
        unlimitedMode = o.optBoolean("unlimitedMode", false), unlimitedBonusBalance = o.optDouble("unlimitedBonusBalance", 0.0),
        totalSpent = o.optDouble("totalSpent", 0.0), pinEnabled = o.optBoolean("pinEnabled", false),
        simPin = o.optString("simPin", "1234"), soundEnabled = o.optBoolean("soundEnabled", true),
        lastDailyClaimDay = o.optString("lastDailyClaimDay", ""), sellerAccountId = o.optString("sellerAccountId", ""),
        createdAt = millisToTs(o.optLong("createdAt")), updatedAt = millisToTs(o.optLong("updatedAt"))
    )

    private fun cardToJson(c: VirtualCard) = JSONObject().apply {
        put("id", c.id); put("userId", c.userId); put("tierKey", c.tierKey); put("cardTitle", c.cardTitle)
        put("cardNetworkLabel", c.cardNetworkLabel); put("cardholderName", c.cardholderName); put("cardNumber", c.cardNumber)
        put("expiryDisplay", c.expiryDisplay); put("cvvDisplay", c.cvvDisplay); put("balance", c.balance)
        put("unlimitedBalance", c.unlimitedBalance); put("cardActive", c.cardActive); put("accentColorHex", c.accentColorHex)
        put("upgradeLevel", c.upgradeLevel); put("animatedStyle", c.animatedStyle)
        put("createdAt", tsToMillis(c.createdAt)); put("updatedAt", tsToMillis(c.updatedAt))
    }

    private fun jsonToCard(o: JSONObject) = VirtualCard(
        id = o.optString("id"), userId = o.optString("userId"), tierKey = o.optString("tierKey", "STARTER"),
        cardTitle = o.optString("cardTitle", "Starter Card"), cardNetworkLabel = o.optString("cardNetworkLabel", "VAULT STARTER DEMO"),
        cardholderName = o.optString("cardholderName", "DEMO HOLDER"), cardNumber = o.optString("cardNumber"),
        expiryDisplay = o.optString("expiryDisplay", "12/99"), cvvDisplay = o.optString("cvvDisplay", "904"),
        balance = o.optDouble("balance", 0.0), unlimitedBalance = o.optBoolean("unlimitedBalance", false),
        cardActive = o.optBoolean("cardActive", true), accentColorHex = o.optString("accentColorHex", "#38BDF8"),
        upgradeLevel = o.optLong("upgradeLevel", 1L), animatedStyle = o.optBoolean("animatedStyle", false),
        createdAt = millisToTs(o.optLong("createdAt")), updatedAt = millisToTs(o.optLong("updatedAt"))
    )

    private fun transactionToJson(t: TransactionRecord) = JSONObject().apply {
        put("id", t.id); put("userId", t.userId); put("transactionRef", t.transactionRef); put("idempotencyKey", t.idempotencyKey)
        put("type", t.type); put("amount", t.amount); put("currencyLabel", t.currencyLabel); put("merchantName", t.merchantName)
        put("sellerId", t.sellerId); put("sellerName", t.sellerName); put("cardId", t.cardId); put("cardSummary", t.cardSummary)
        put("orderId", t.orderId); put("status", t.status); put("note", t.note); put("createdAt", tsToMillis(t.createdAt))
    }

    private fun jsonToTransaction(o: JSONObject) = TransactionRecord(
        id = o.optString("id"), userId = o.optString("userId"), transactionRef = o.optString("transactionRef"),
        idempotencyKey = o.optString("idempotencyKey"), type = o.optString("type", "PAYMENT"),
        amount = o.optDouble("amount", 0.0), currencyLabel = o.optString("currencyLabel", "SIM USD"),
        merchantName = o.optString("merchantName"), sellerId = o.optString("sellerId"),
        sellerName = o.optString("sellerName"), cardId = o.optString("cardId"), cardSummary = o.optString("cardSummary"),
        orderId = o.optString("orderId"), status = o.optString("status", "SIMULATED_SUCCESS"),
        note = o.optString("note"), createdAt = millisToTs(o.optLong("createdAt"))
    )

    private fun ticketToJson(t: SupportTicket) = JSONObject().apply {
        put("id", t.id); put("userId", t.userId); put("ticketCode", t.ticketCode); put("subject", t.subject)
        put("description", t.description); put("category", t.category); put("status", t.status)
        put("resolutionNote", t.resolutionNote); put("createdAt", tsToMillis(t.createdAt)); put("updatedAt", tsToMillis(t.updatedAt))
    }

    private fun jsonToTicket(o: JSONObject) = SupportTicket(
        id = o.optString("id"), userId = o.optString("userId"), ticketCode = o.optString("ticketCode"),
        subject = o.optString("subject"), description = o.optString("description"), category = o.optString("category"),
        status = o.optString("status", "OPEN"), resolutionNote = o.optString("resolutionNote"),
        createdAt = millisToTs(o.optLong("createdAt")), updatedAt = millisToTs(o.optLong("updatedAt"))
    )

    private fun sellerToJson(s: SellerProfile) = JSONObject().apply {
        put("id", s.id); put("sellerOwnerUid", s.sellerOwnerUid); put("sellerCode", s.sellerCode)
        put("storeName", s.storeName); put("description", s.description); put("category", s.category)
        put("logoToneHex", s.logoToneHex); put("demoVerified", s.demoVerified); put("demoListed", s.demoListed)
        put("qrPayload", s.qrPayload); put("ratingSum", s.ratingSum); put("reviewCount", s.reviewCount)
        put("completedOrdersCount", s.completedOrdersCount); put("virtualEarnings", s.virtualEarnings)
        put("createdAt", tsToMillis(s.createdAt)); put("updatedAt", tsToMillis(s.updatedAt))
    }

    private fun jsonToSeller(o: JSONObject) = SellerProfile(
        id = o.optString("id"), sellerOwnerUid = o.optString("sellerOwnerUid"), sellerCode = o.optString("sellerCode"),
        storeName = o.optString("storeName"), description = o.optString("description"), category = o.optString("category"),
        logoToneHex = o.optString("logoToneHex", "#F5C451"), demoVerified = o.optBoolean("demoVerified", true),
        demoListed = o.optBoolean("demoListed", true), qrPayload = o.optString("qrPayload"),
        ratingSum = o.optLong("ratingSum"), reviewCount = o.optLong("reviewCount"),
        completedOrdersCount = o.optLong("completedOrdersCount"), virtualEarnings = o.optDouble("virtualEarnings"),
        createdAt = millisToTs(o.optLong("createdAt")), updatedAt = millisToTs(o.optLong("updatedAt"))
    )

    private fun productToJson(p: MarketplaceProduct) = JSONObject().apply {
        put("id", p.id); put("sellerId", p.sellerId); put("sellerOwnerUid", p.sellerOwnerUid)
        put("storeName", p.storeName); put("name", p.name); put("description", p.description)
        put("category", p.category); put("price", p.price); put("stockCount", p.stockCount)
        put("iconKey", p.iconKey); put("demoListed", p.demoListed)
        put("createdAt", tsToMillis(p.createdAt)); put("updatedAt", tsToMillis(p.updatedAt))
    }

    private fun jsonToProduct(o: JSONObject) = MarketplaceProduct(
        id = o.optString("id"), sellerId = o.optString("sellerId"), sellerOwnerUid = o.optString("sellerOwnerUid"),
        storeName = o.optString("storeName"), name = o.optString("name"), description = o.optString("description"),
        category = o.optString("category"), price = o.optDouble("price"), stockCount = o.optLong("stockCount"),
        iconKey = o.optString("iconKey", "shopping"), demoListed = o.optBoolean("demoListed", true),
        createdAt = millisToTs(o.optLong("createdAt")), updatedAt = millisToTs(o.optLong("updatedAt"))
    )

    private fun orderToJson(ord: SimulatorOrder) = JSONObject().apply {
        put("id", ord.id); put("orderCode", ord.orderCode); put("buyerUid", ord.buyerUid)
        put("buyerName", ord.buyerName); put("sellerId", ord.sellerId); put("sellerOwnerUid", ord.sellerOwnerUid)
        put("sellerName", ord.sellerName); put("itemsSummary", ord.itemsSummary); put("itemCount", ord.itemCount)
        put("totalAmount", ord.totalAmount); put("cardId", ord.cardId); put("cardSummary", ord.cardSummary)
        put("transactionRef", ord.transactionRef); put("status", ord.status)
        put("trackingEvents", JSONArray().apply {
            ord.trackingEvents.forEach { ev ->
                put(JSONObject().apply {
                    put("stage", ev.stage); put("label", ev.label); put("timestampMillis", ev.timestampMillis)
                })
            }
        })
        put("refundStatus", ord.refundStatus); put("refundReason", ord.refundReason); put("sellerResponse", ord.sellerResponse)
        put("createdAt", tsToMillis(ord.createdAt)); put("updatedAt", tsToMillis(ord.updatedAt))
    }

    private fun jsonToOrder(o: JSONObject): SimulatorOrder {
        val evArr = o.optJSONArray("trackingEvents") ?: JSONArray()
        val events = (0 until evArr.length()).map {
            val ev = evArr.getJSONObject(it)
            TrackingEvent(ev.optString("stage"), ev.optString("label"), ev.optLong("timestampMillis"))
        }
        return SimulatorOrder(
            id = o.optString("id"), orderCode = o.optString("orderCode"), buyerUid = o.optString("buyerUid"),
            buyerName = o.optString("buyerName"), sellerId = o.optString("sellerId"),
            sellerOwnerUid = o.optString("sellerOwnerUid"), sellerName = o.optString("sellerName"),
            itemsSummary = o.optString("itemsSummary"), itemCount = o.optLong("itemCount", 1L),
            totalAmount = o.optDouble("totalAmount"), cardId = o.optString("cardId"),
            cardSummary = o.optString("cardSummary"), transactionRef = o.optString("transactionRef"),
            status = o.optString("status"), trackingEvents = events, refundStatus = o.optString("refundStatus", "NONE"),
            refundReason = o.optString("refundReason"), sellerResponse = o.optString("sellerResponse"),
            createdAt = millisToTs(o.optLong("createdAt")), updatedAt = millisToTs(o.optLong("updatedAt"))
        )
    }

    private fun reviewToJson(r: SellerReview) = JSONObject().apply {
        put("id", r.id); put("sellerId", r.sellerId); put("orderId", r.orderId); put("buyerUid", r.buyerUid)
        put("buyerName", r.buyerName); put("rating", r.rating); put("comment", r.comment)
        put("demoListed", r.demoListed); put("createdAt", tsToMillis(r.createdAt))
    }

    private fun jsonToReview(o: JSONObject) = SellerReview(
        id = o.optString("id"), sellerId = o.optString("sellerId"), orderId = o.optString("orderId"),
        buyerUid = o.optString("buyerUid"), buyerName = o.optString("buyerName"), rating = o.optLong("rating", 5L),
        comment = o.optString("comment"), demoListed = o.optBoolean("demoListed", true),
        createdAt = millisToTs(o.optLong("createdAt"))
    )
}
