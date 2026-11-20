package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.SimSoundEffects
import com.example.data.model.CardCatalogTier
import com.example.data.model.CartItem
import com.example.data.model.MarketplaceProduct
import com.example.data.model.OrderStage
import com.example.data.model.PurchaseSuccessReceipt
import com.example.data.model.SellerProfile
import com.example.data.model.SellerReview
import com.example.data.model.SimulatorOrder
import com.example.data.model.SupportChatMessage
import com.example.data.model.SupportSenderType
import com.example.data.model.SupportTicket
import com.example.data.model.TransactionRecord
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VirtualCard
import com.example.data.repository.SimulatorBackend
import com.example.data.repository.SupportAiRepository
import com.example.data.repository.UserPreferencesRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SimulatorViewModel(
    private val backend: SimulatorBackend,
    private val supportAiRepo: SupportAiRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val currentUserId: String,
    private val initialDisplayName: String
) : ViewModel() {

    private val _sellerMode = MutableStateFlow(false)
    val sellerMode: StateFlow<Boolean> = _sellerMode.asStateFlow()

    private val _pinUnlockedThisSession = MutableStateFlow(false)
    val pinUnlockedThisSession: StateFlow<Boolean> = _pinUnlockedThisSession.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _latestReceipt = MutableStateFlow<PurchaseSuccessReceipt?>(null)
    val latestReceipt: StateFlow<PurchaseSuccessReceipt?> = _latestReceipt.asStateFlow()

    private val _newlyPurchasedCard = MutableStateFlow<VirtualCard?>(null)
    val newlyPurchasedCard: StateFlow<VirtualCard?> = _newlyPurchasedCard.asStateFlow()

    private val _selectedOrderId = MutableStateFlow<String?>(null)
    val selectedOrderId: StateFlow<String?> = _selectedOrderId.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isActionBusy = MutableStateFlow(false)
    val isActionBusy: StateFlow<Boolean> = _isActionBusy.asStateFlow()

    val darkMode: StateFlow<Boolean> = prefsRepo.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val userProfileState: StateFlow<UiState<UserProfile?>> = backend.observeUserProfile()
        .asUiState("Failed to load user profile")

    val cardsState: StateFlow<UiState<List<VirtualCard>>> = backend.observeCards()
        .asUiState("Failed to load virtual cards")

    val transactionsState: StateFlow<UiState<List<TransactionRecord>>> = backend.observeTransactions()
        .asUiState("Failed to load transactions")

    val ticketsState: StateFlow<UiState<List<SupportTicket>>> = backend.observeTickets()
        .asUiState("Failed to load support tickets")

    val sellersState: StateFlow<UiState<List<SellerProfile>>> = backend.observeSellers()
        .asUiState("Failed to load marketplace sellers")

    val productsState: StateFlow<UiState<List<MarketplaceProduct>>> = backend.observeProducts()
        .asUiState("Failed to load marketplace products")

    val buyerOrdersState: StateFlow<UiState<List<SimulatorOrder>>> = backend.observeBuyerOrders()
        .asUiState("Failed to load your orders")

    val sellerOrdersState: StateFlow<UiState<List<SimulatorOrder>>> = backend.observeSellerOrders()
        .asUiState("Failed to load seller orders")

    val reviewsState: StateFlow<UiState<List<SellerReview>>> = backend.observeReviews()
        .asUiState("Failed to load seller reviews")

    // AI Support Chatbot State
    private val _supportMessages = MutableStateFlow(
        listOf(
            SupportChatMessage(
                id = "welcome_ai",
                senderType = SupportSenderType.AUTOMATED_AI,
                text = "Hello! I am the Automated AI Support Assistant for Vault Card. Ask me about virtual cards, card deactivation, direct QR / UPI payments, virtual refunds, or order tracking. Need manual follow-up? Switch to the Tickets tab to create a support ticket."
            )
        )
    )
    val supportMessages: StateFlow<List<SupportChatMessage>> = _supportMessages.asStateFlow()

    private val _aiStreamingText = MutableStateFlow<String?>(null)
    val aiStreamingText: StateFlow<String?> = _aiStreamingText.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val currentProfile: UserProfile?
        get() = (userProfileState.value as? UiState.Success)?.data

    private val currentCards: List<VirtualCard>
        get() = (cardsState.value as? UiState.Success)?.data ?: emptyList()

    private val soundEnabled: Boolean
        get() = currentProfile?.soundEnabled ?: true

    val cartTotal: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { it.product.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val mySellerProfile: StateFlow<SellerProfile?> = combine(sellersState, userProfileState) { sellersRes, _ ->
        val sellers = (sellersRes as? UiState.Success)?.data ?: emptyList()
        sellers.firstOrNull { it.sellerOwnerUid == currentUserId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    init {
        viewModelScope.launch {
            backend.bootstrap(initialDisplayName)
        }
        // Watch buyer orders for seller-approved refunds and automatically credit buyer virtual balance once
        viewModelScope.launch {
            buyerOrdersState.collect { state ->
                val orders = (state as? UiState.Success)?.data ?: return@collect
                orders.filter { it.status == "REFUNDED" && it.refundStatus == "APPROVED" }
                    .forEach { order ->
                        backend.restoreBuyerRefundIfNeeded(order, order.sellerResponse.ifEmpty { "Refund Approved" })
                    }
            }
        }
    }

    private fun <T> Flow<T>.asUiState(fallbackError: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, fallbackError, root)
                emit(UiState.Error(root.message ?: fallbackError))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun clearStatusMessage() { _statusMessage.value = null }
    fun clearLatestReceipt() { _latestReceipt.value = null }
    fun selectOrderForTracking(orderId: String?) { _selectedOrderId.value = orderId }
    fun setSellerMode(enabled: Boolean) { _sellerMode.value = enabled }
    fun unlockPinSession() { _pinUnlockedThisSession.value = true }
    fun setDarkMode(enabled: Boolean) { viewModelScope.launch { prefsRepo.setDarkMode(enabled) } }

    fun toggleUnlimitedMode(enabled: Boolean) {
        viewModelScope.launch {
            backend.toggleUnlimitedMode(enabled)
                .onSuccess {
                    SimSoundEffects.playTapTone(soundEnabled)
                    _statusMessage.value = if (enabled) "Unlimited Virtual Mode activated!" else "Standard card limits restored."
                }
                .onFailure { _statusMessage.value = it.message }
        }
    }

    fun addVirtualMoney(card: VirtualCard, amount: Double) {
        viewModelScope.launch {
            _isActionBusy.value = true
            backend.addVirtualMoney(card, amount)
                .onSuccess {
                    SimSoundEffects.playSuccessChime(soundEnabled)
                    _statusMessage.value = "Card top-up successful! Account Balance updated for ${card.cardTitle}."
                }
                .onFailure { _statusMessage.value = it.message }
            _isActionBusy.value = false
        }
    }

    fun resetSimulator() {
        viewModelScope.launch {
            _isActionBusy.value = true
            backend.resetSimulatorBalances(currentCards)
                .onSuccess {
                    SimSoundEffects.playTapTone(soundEnabled)
                    _statusMessage.value = "Account Balance reset to $0.00 across all cards."
                }
                .onFailure { _statusMessage.value = it.message }
            _isActionBusy.value = false
        }
    }

    fun buyCardFromStore(tier: CardCatalogTier, holderName: String, accentHex: String) {
        val profile = currentProfile ?: return
        viewModelScope.launch {
            _isActionBusy.value = true
            backend.buyCardFromStore(profile, tier, holderName, accentHex)
                .onSuccess { newCard ->
                    SimSoundEffects.playSuccessChime(soundEnabled)
                    _newlyPurchasedCard.value = newCard
                    _statusMessage.value = "${tier.title} unlocked & ready to use!"
                }
                .onFailure { _statusMessage.value = it.message }
            _isActionBusy.value = false
        }
    }

    fun consumeNewlyPurchasedCard() {
        _newlyPurchasedCard.value = null
    }

    fun toggleCardActive(card: VirtualCard) {
        viewModelScope.launch {
            val next = !card.cardActive
            backend.toggleCardActive(card.id, next)
                .onSuccess {
                    SimSoundEffects.playTapTone(soundEnabled)
                    _statusMessage.value = if (next) "${card.cardTitle} activated." else "${card.cardTitle} deactivated (purchases locked)."
                }
                .onFailure { _statusMessage.value = it.message }
        }
    }

    fun customizeAndUpgradeCard(card: VirtualCard, holderName: String, accentHex: String, upgrade: Boolean) {
        val profile = currentProfile ?: return
        viewModelScope.launch {
            _isActionBusy.value = true
            backend.customizeAndUpgradeCard(profile, card, holderName, accentHex, upgrade)
                .onSuccess {
                    SimSoundEffects.playSuccessChime(soundEnabled)
                    _statusMessage.value = if (upgrade) "${card.cardTitle} upgraded to Level ${(card.upgradeLevel + 1L).coerceAtMost(10L)}!" else "${card.cardTitle} customization saved."
                }
                .onFailure { _statusMessage.value = it.message }
            _isActionBusy.value = false
        }
    }

    fun claimDailyCoins() {
        val profile = currentProfile ?: return
        viewModelScope.launch {
            backend.claimDailyCoins(profile)
                .onSuccess { reward ->
                    SimSoundEffects.playSuccessChime(soundEnabled)
                    _statusMessage.value = "Claimed +$reward Daily Vault Coins!"
                }
                .onFailure { _statusMessage.value = it.message }
        }
    }

    fun updateSettings(pinEnabled: Boolean, simPin: String, soundOn: Boolean) {
        viewModelScope.launch {
            backend.updateSettings(pinEnabled, simPin, soundOn)
                .onSuccess { _statusMessage.value = "Vault Card preferences saved." }
                .onFailure { _statusMessage.value = it.message }
        }
    }

    fun addToCart(product: MarketplaceProduct) {
        _cartItems.update { list ->
            val existing = list.firstOrNull { it.product.id == product.id }
            if (existing != null) {
                list.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
            } else {
                list + CartItem(product, 1)
            }
        }
        SimSoundEffects.playTapTone(soundEnabled)
        _statusMessage.value = "Added ${product.name} to cart"
    }

    fun updateCartQuantity(productId: String, delta: Int) {
        _cartItems.update { list ->
            list.mapNotNull { item ->
                if (item.product.id == productId) {
                    val nextQty = item.quantity + delta
                    if (nextQty <= 0) null else item.copy(quantity = nextQty)
                } else item
            }
        }
    }

    fun clearCart() { _cartItems.value = emptyList() }

    fun executeVirtualPayment(
        card: VirtualCard,
        amount: Double,
        merchantName: String,
        seller: SellerProfile? = null,
        cartItems: List<CartItem> = emptyList(),
        idempotencyKey: String,
        onSuccess: (PurchaseSuccessReceipt) -> Unit
    ) {
        val profile = currentProfile ?: return
        viewModelScope.launch {
            _isActionBusy.value = true
            backend.processVirtualPayment(profile, card, amount, merchantName, seller, cartItems, idempotencyKey)
                .onSuccess { receipt ->
                    SimSoundEffects.playSuccessChime(soundEnabled)
                    if (cartItems.isNotEmpty()) {
                        val checkedOutIds = cartItems.map { it.product.id }.toSet()
                        _cartItems.update { cur -> cur.filterNot { it.product.id in checkedOutIds } }
                    }
                    _latestReceipt.value = receipt
                    onSuccess(receipt)
                }
                .onFailure { _statusMessage.value = it.message ?: "Simulated payment failed" }
            _isActionBusy.value = false
        }
    }

    fun saveSellerStore(storeName: String, description: String, category: String, logoToneHex: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            backend.createOrUpdateSellerStore(mySellerProfile.value, storeName, description, category, logoToneHex)
                .onSuccess {
                    SimSoundEffects.playSuccessChime(soundEnabled)
                    _statusMessage.value = "Verified Demo Seller Store saved!"
                }
                .onFailure { _statusMessage.value = it.message }
            _isActionBusy.value = false
        }
    }

    fun saveSellerProduct(existing: MarketplaceProduct?, name: String, description: String, category: String, price: Double, stock: Long) {
        val seller = mySellerProfile.value ?: run {
            _statusMessage.value = "Create your Seller Store first."
            return
        }
        viewModelScope.launch {
            _isActionBusy.value = true
            backend.saveSellerProduct(seller, existing, name, description, category, price, stock)
                .onSuccess {
                    SimSoundEffects.playSuccessChime(soundEnabled)
                    _statusMessage.value = if (existing == null) "Product added to store!" else "Product updated!"
                }
                .onFailure { _statusMessage.value = it.message }
            _isActionBusy.value = false
        }
    }

    fun advanceOrderStage(order: SimulatorOrder) {
        val current = OrderStage.fromKey(order.status) ?: OrderStage.PAYMENT_SIMULATED
        val next = current.nextStage() ?: return
        viewModelScope.launch {
            backend.advanceOrderStage(order, next)
                .onSuccess {
                    SimSoundEffects.playTapTone(soundEnabled)
                    _statusMessage.value = "Order ${order.orderCode} updated to ${next.label}"
                }
                .onFailure { _statusMessage.value = it.message }
        }
    }

    fun cancelOrder(order: SimulatorOrder) {
        viewModelScope.launch {
            backend.cancelBuyerOrder(order)
                .onSuccess {
                    SimSoundEffects.playTapTone(soundEnabled)
                    _statusMessage.value = "Order ${order.orderCode} cancelled."
                }
                .onFailure { _statusMessage.value = it.message }
        }
    }

    fun requestRefund(order: SimulatorOrder, reason: String) {
        viewModelScope.launch {
            backend.requestOrderRefund(order, reason)
                .onSuccess {
                    SimSoundEffects.playTapTone(soundEnabled)
                    _statusMessage.value = "Refund request submitted for ${order.orderCode}."
                }
                .onFailure { _statusMessage.value = it.message }
        }
    }

    fun resolveRefund(order: SimulatorOrder, approve: Boolean, response: String) {
        viewModelScope.launch {
            backend.resolveOrderRefund(order, approve, response)
                .onSuccess {
                    SimSoundEffects.playSuccessChime(soundEnabled)
                    _statusMessage.value = if (approve) "Refund approved in order log." else "Refund request declined."
                }
                .onFailure { _statusMessage.value = it.message }
        }
    }

    fun submitReview(seller: SellerProfile, orderId: String, rating: Int, comment: String) {
        val profile = currentProfile ?: return
        viewModelScope.launch {
            backend.submitSellerReview(profile, seller, orderId, rating, comment)
                .onSuccess {
                    SimSoundEffects.playSuccessChime(soundEnabled)
                    _statusMessage.value = "Review posted for ${seller.storeName}!"
                }
                .onFailure { _statusMessage.value = it.message }
        }
    }

    fun createSupportTicket(subject: String, category: String, description: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            backend.createSupportTicket(subject, category, description)
                .onSuccess { code ->
                    SimSoundEffects.playSuccessChime(soundEnabled)
                    _statusMessage.value = "Support Ticket $code created (Status: OPEN)."
                }
                .onFailure { _statusMessage.value = it.message }
            _isActionBusy.value = false
        }
    }

    fun advanceSupportTicket(ticket: SupportTicket) {
        val nextStatus = when (ticket.status) {
            "OPEN" -> "IN_PROGRESS"
            "IN_PROGRESS" -> "RESOLVED"
            else -> "RESOLVED"
        }
        val note = if (nextStatus == "IN_PROGRESS") {
            "Human support queue reviewing ticket ${ticket.ticketCode}."
        } else {
            "Resolved inside Vault Card Support Center."
        }
        viewModelScope.launch {
            backend.updateSupportTicketStatus(ticket.id, nextStatus, note)
                .onSuccess { _statusMessage.value = "Ticket ${ticket.ticketCode} -> $nextStatus" }
                .onFailure { _statusMessage.value = it.message }
        }
    }

    fun sendSupportChatMessage(userText: String) {
        val clean = userText.trim()
        if (clean.isEmpty() || _aiStreamingText.value != null) return
        _aiError.value = null
        val userMsg = SupportChatMessage(
            id = "usr_${System.currentTimeMillis()}",
            senderType = SupportSenderType.USER,
            text = clean
        )
        val historyPairs = _supportMessages.value
            .filter { it.senderType != SupportSenderType.HUMAN_QUEUE_INFO }
            .map { (if (it.senderType == SupportSenderType.USER) "user" else "model") to it.text }
        _supportMessages.update { it + userMsg }

        viewModelScope.launch {
            _aiStreamingText.value = ""
            try {
                var finalReply = ""
                supportAiRepo.streamSupportReply(clean, historyPairs).collect { partial ->
                    finalReply = partial
                    _aiStreamingText.value = partial
                }
                _aiStreamingText.value = null
                if (finalReply.isNotBlank()) {
                    _supportMessages.update {
                        it + SupportChatMessage(
                            id = "ai_${System.currentTimeMillis()}",
                            senderType = SupportSenderType.AUTOMATED_AI,
                            text = finalReply
                        )
                    }
                } else {
                    _aiError.value = "AI Support returned an empty response."
                }
            } catch (e: TimeoutCancellationException) {
                _aiStreamingText.value = null
                _aiError.value = "Support AI timed out after 60s. Please retry or file a Support Ticket."
            } catch (e: CancellationException) {
                _aiStreamingText.value = null
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Support chat error", e)
                _aiStreamingText.value = null
                _aiError.value = e.localizedMessage ?: "Failed to reach Automated AI Support."
            }
        }
    }

    fun appendHumanEscalationNotice() {
        _supportMessages.update {
            it + SupportChatMessage(
                id = "human_${System.currentTimeMillis()}",
                senderType = SupportSenderType.HUMAN_QUEUE_INFO,
                text = "Human Agent Escalation: Automated AI responses are separate from human review. Open the 'Create Ticket' tab to submit a ticket ID (SUP-XXXXXX) for tracked resolution."
            )
        }
    }

    private companion object {
        const val TAG = "SimulatorVM"
    }
}
