package com.example.data.repository

import com.example.data.model.CardCatalogTier
import com.example.data.model.CartItem
import com.example.data.model.MarketplaceProduct
import com.example.data.model.OrderStage
import com.example.data.model.PurchaseSuccessReceipt
import com.example.data.model.SellerProfile
import com.example.data.model.SellerReview
import com.example.data.model.SimulatorOrder
import com.example.data.model.SupportTicket
import com.example.data.model.TransactionRecord
import com.example.data.model.UserProfile
import com.example.data.model.VirtualCard
import kotlinx.coroutines.flow.Flow

interface SimulatorBackend {
    fun observeUserProfile(): Flow<UserProfile?>
    fun observeCards(): Flow<List<VirtualCard>>
    fun observeTransactions(): Flow<List<TransactionRecord>>
    fun observeTickets(): Flow<List<SupportTicket>>
    fun observeSellers(): Flow<List<SellerProfile>>
    fun observeProducts(): Flow<List<MarketplaceProduct>>
    fun observeBuyerOrders(): Flow<List<SimulatorOrder>>
    fun observeSellerOrders(): Flow<List<SimulatorOrder>>
    fun observeReviews(): Flow<List<SellerReview>>

    suspend fun bootstrap(initialDisplayName: String)
    suspend fun toggleUnlimitedMode(enabled: Boolean): Result<Unit>
    suspend fun addVirtualMoney(card: VirtualCard, amount: Double): Result<Unit>
    suspend fun resetSimulatorBalances(cards: List<VirtualCard>): Result<Unit>
    suspend fun buyCardFromStore(
        profile: UserProfile,
        tier: CardCatalogTier,
        customHolderName: String,
        customAccentHex: String
    ): Result<VirtualCard>
    suspend fun toggleCardActive(cardId: String, active: Boolean): Result<Unit>
    suspend fun customizeAndUpgradeCard(
        profile: UserProfile,
        card: VirtualCard,
        newHolderName: String,
        newAccentHex: String,
        upgrade: Boolean
    ): Result<Unit>
    suspend fun claimDailyCoins(profile: UserProfile): Result<Long>
    suspend fun updateSettings(pinEnabled: Boolean, simPin: String, soundEnabled: Boolean): Result<Unit>
    suspend fun createSupportTicket(subject: String, category: String, description: String): Result<String>
    suspend fun updateSupportTicketStatus(ticketId: String, status: String, resolutionNote: String): Result<Unit>

    suspend fun processVirtualPayment(
        profile: UserProfile,
        card: VirtualCard,
        amount: Double,
        merchantName: String,
        seller: SellerProfile?,
        cartItems: List<CartItem>,
        idempotencyKey: String
    ): Result<PurchaseSuccessReceipt>

    suspend fun createOrUpdateSellerStore(
        existingSeller: SellerProfile?,
        storeName: String,
        description: String,
        category: String,
        logoToneHex: String
    ): Result<SellerProfile>

    suspend fun saveSellerProduct(
        seller: SellerProfile,
        existingProduct: MarketplaceProduct?,
        name: String,
        description: String,
        category: String,
        price: Double,
        stockCount: Long
    ): Result<Unit>

    suspend fun advanceOrderStage(order: SimulatorOrder, nextStage: OrderStage): Result<Unit>
    suspend fun cancelBuyerOrder(order: SimulatorOrder): Result<Unit>
    suspend fun requestOrderRefund(order: SimulatorOrder, reason: String): Result<Unit>
    suspend fun resolveOrderRefund(order: SimulatorOrder, approve: Boolean, sellerResponse: String): Result<Unit>
    suspend fun restoreBuyerRefundIfNeeded(order: SimulatorOrder, reason: String): Result<Unit>
    suspend fun submitSellerReview(
        profile: UserProfile,
        seller: SellerProfile,
        orderId: String,
        rating: Int,
        comment: String
    ): Result<Unit>
}

class CloudSimulatorBackend(
    private val currentUserId: String,
    private val walletRepo: WalletRepository,
    private val marketRepo: MarketplaceRepository
) : SimulatorBackend {
    override fun observeUserProfile(): Flow<UserProfile?> = walletRepo.observeUserProfile(currentUserId)
    override fun observeCards(): Flow<List<VirtualCard>> = walletRepo.observeCards(currentUserId)
    override fun observeTransactions(): Flow<List<TransactionRecord>> = walletRepo.observeTransactions(currentUserId)
    override fun observeTickets(): Flow<List<SupportTicket>> = walletRepo.observeTickets(currentUserId)
    override fun observeSellers(): Flow<List<SellerProfile>> = marketRepo.observeSellers()
    override fun observeProducts(): Flow<List<MarketplaceProduct>> = marketRepo.observeProducts()
    override fun observeBuyerOrders(): Flow<List<SimulatorOrder>> = marketRepo.observeBuyerOrders(currentUserId)
    override fun observeSellerOrders(): Flow<List<SimulatorOrder>> = marketRepo.observeSellerOrders(currentUserId)
    override fun observeReviews(): Flow<List<SellerReview>> = marketRepo.observeReviews()

    override suspend fun bootstrap(initialDisplayName: String) {
        walletRepo.ensureUserBootstrapped(initialDisplayName)
        marketRepo.ensureDemoMarketplaceSeeded()
    }

    override suspend fun toggleUnlimitedMode(enabled: Boolean) = walletRepo.toggleUnlimitedMode(enabled)
    override suspend fun addVirtualMoney(card: VirtualCard, amount: Double) = walletRepo.addVirtualMoney(card, amount)
    override suspend fun resetSimulatorBalances(cards: List<VirtualCard>) = walletRepo.resetSimulatorBalances(cards)

    override suspend fun buyCardFromStore(
        profile: UserProfile,
        tier: CardCatalogTier,
        customHolderName: String,
        customAccentHex: String
    ): Result<VirtualCard> = walletRepo.buyCardFromStore(profile, tier, customHolderName, customAccentHex)

    override suspend fun toggleCardActive(cardId: String, active: Boolean) = walletRepo.toggleCardActive(cardId, active)

    override suspend fun customizeAndUpgradeCard(
        profile: UserProfile,
        card: VirtualCard,
        newHolderName: String,
        newAccentHex: String,
        upgrade: Boolean
    ) = walletRepo.customizeAndUpgradeCard(profile, card, newHolderName, newAccentHex, upgrade)

    override suspend fun claimDailyCoins(profile: UserProfile) = walletRepo.claimDailyCoins(profile)

    override suspend fun updateSettings(pinEnabled: Boolean, simPin: String, soundEnabled: Boolean) =
        walletRepo.updateSettings(pinEnabled, simPin, soundEnabled)

    override suspend fun createSupportTicket(subject: String, category: String, description: String) =
        walletRepo.createSupportTicket(subject, category, description)

    override suspend fun updateSupportTicketStatus(ticketId: String, status: String, resolutionNote: String) =
        walletRepo.updateSupportTicketStatus(ticketId, status, resolutionNote)

    override suspend fun processVirtualPayment(
        profile: UserProfile,
        card: VirtualCard,
        amount: Double,
        merchantName: String,
        seller: SellerProfile?,
        cartItems: List<CartItem>,
        idempotencyKey: String
    ) = marketRepo.processVirtualPayment(profile, card, amount, merchantName, seller, cartItems, idempotencyKey)

    override suspend fun createOrUpdateSellerStore(
        existingSeller: SellerProfile?,
        storeName: String,
        description: String,
        category: String,
        logoToneHex: String
    ) = marketRepo.createOrUpdateSellerStore(existingSeller, storeName, description, category, logoToneHex)

    override suspend fun saveSellerProduct(
        seller: SellerProfile,
        existingProduct: MarketplaceProduct?,
        name: String,
        description: String,
        category: String,
        price: Double,
        stockCount: Long
    ) = marketRepo.saveSellerProduct(seller, existingProduct, name, description, category, price, stockCount)

    override suspend fun advanceOrderStage(order: SimulatorOrder, nextStage: OrderStage) =
        marketRepo.advanceOrderStage(order, nextStage)

    override suspend fun cancelBuyerOrder(order: SimulatorOrder) = marketRepo.cancelBuyerOrder(order)
    override suspend fun requestOrderRefund(order: SimulatorOrder, reason: String) = marketRepo.requestOrderRefund(order, reason)
    override suspend fun resolveOrderRefund(order: SimulatorOrder, approve: Boolean, sellerResponse: String) =
        marketRepo.resolveOrderRefund(order, approve, sellerResponse)

    override suspend fun restoreBuyerRefundIfNeeded(order: SimulatorOrder, reason: String) =
        marketRepo.restoreBuyerRefundIfNeeded(currentUserId, order, reason)

    override suspend fun submitSellerReview(
        profile: UserProfile,
        seller: SellerProfile,
        orderId: String,
        rating: Int,
        comment: String
    ) = marketRepo.submitSellerReview(profile, seller, orderId, rating, comment)
}
