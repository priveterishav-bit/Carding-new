package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Vault Member",
    val customerId: String = "VP-100001",
    val coins: Long = 1500L,
    val unlimitedMode: Boolean = false,
    val unlimitedBonusBalance: Double = 0.0,
    val totalSpent: Double = 0.0,
    val pinEnabled: Boolean = false,
    val simPin: String = "1234",
    val soundEnabled: Boolean = true,
    val lastDailyClaimDay: String = "",
    val sellerAccountId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VirtualCard(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tierKey: String = "STARTER",
    val cardTitle: String = "Starter Classic",
    val cardNetworkLabel: String = "VAULT CLASSIC DEMO",
    val cardholderName: String = "DEMO HOLDER",
    val cardNumber: String = "9901 0000 0000 1000",
    val expiryDisplay: String = "12/99",
    val cvvDisplay: String = "904",
    val balance: Double = 0.0,
    val unlimitedBalance: Boolean = false,
    val cardActive: Boolean = true,
    val accentColorHex: String = "#38BDF8",
    val upgradeLevel: Long = 1L,
    val animatedStyle: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TransactionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val transactionRef: String = "",
    val idempotencyKey: String = "",
    val type: String = "PAYMENT",
    val amount: Double = 0.0,
    val currencyLabel: String = "SIM USD",
    val merchantName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val cardId: String = "",
    val cardSummary: String = "",
    val orderId: String = "",
    val status: String = "SIMULATED_SUCCESS",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ticketCode: String = "",
    val subject: String = "",
    val description: String = "",
    val category: String = "General",
    val status: String = "OPEN",
    val resolutionNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SellerProfile(
    @DocumentId val id: String = "",
    val sellerOwnerUid: String = "",
    val sellerCode: String = "",
    val storeName: String = "",
    val description: String = "",
    val category: String = "General",
    val logoToneHex: String = "#F5C451",
    val demoVerified: Boolean = true,
    val demoListed: Boolean = true,
    val qrPayload: String = "",
    val ratingSum: Long = 0L,
    val reviewCount: Long = 0L,
    val completedOrdersCount: Long = 0L,
    val virtualEarnings: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val averageRating: Double
        get() = if (reviewCount <= 0L) 5.0 else ratingSum.toDouble() / reviewCount.toDouble()
}

@Keep
@IgnoreExtraProperties
data class MarketplaceProduct(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val sellerOwnerUid: String = "",
    val storeName: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "Digital",
    val price: Double = 10.0,
    val stockCount: Long = 50L,
    val iconKey: String = "shopping",
    val demoListed: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TrackingEvent(
    val stage: String = "ORDER_PLACED",
    val label: String = "Order Placed",
    val timestampMillis: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class SimulatorOrder(
    @DocumentId val id: String = "",
    val orderCode: String = "",
    val buyerUid: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerOwnerUid: String = "",
    val sellerName: String = "",
    val itemsSummary: String = "",
    val itemCount: Long = 1L,
    val totalAmount: Double = 0.0,
    val cardId: String = "",
    val cardSummary: String = "",
    val transactionRef: String = "",
    val status: String = "ORDER_PLACED",
    val trackingEvents: List<TrackingEvent> = emptyList(),
    val refundStatus: String = "NONE",
    val refundReason: String = "",
    val sellerResponse: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SellerReview(
    @DocumentId val id: String = "",
    val sellerId: String = "",
    val orderId: String = "",
    val buyerUid: String = "",
    val buyerName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val demoListed: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class OrderStage(val key: String, val label: String, val stepNumber: Int) {
    ORDER_PLACED("ORDER_PLACED", "Order Placed", 1),
    PAYMENT_SIMULATED("PAYMENT_SIMULATED", "Payment Simulated Successfully", 2),
    SELLER_CONFIRMED("SELLER_CONFIRMED", "Seller Confirmed", 3),
    PROCESSING("PROCESSING", "Processing", 4),
    READY_FOR_DELIVERY("READY_FOR_DELIVERY", "Ready for Delivery", 5),
    SHIPPED("SHIPPED", "Shipped", 6),
    OUT_FOR_DELIVERY("OUT_FOR_DELIVERY", "Out for Delivery", 7),
    DELIVERED("DELIVERED", "Delivered", 8),
    COMPLETED("COMPLETED", "Completed", 9);

    fun nextStage(): OrderStage? {
        val nextIndex = ordinal + 1
        return entries.getOrNull(nextIndex)
    }

    companion object {
        fun fromKey(key: String): OrderStage? = entries.firstOrNull { it.key == key }
    }
}

data class CardCatalogTier(
    val tierKey: String,
    val title: String,
    val networkLabel: String,
    val startingBalance: Double,
    val unlimited: Boolean,
    val coinPrice: Long,
    val binPrefix: String,
    val defaultAccentHex: String,
    val isAnimated: Boolean,
    val badgeText: String
)

object CardCatalog {
    val tiers = listOf(
        CardCatalogTier(
            tierKey = "STARTER",
            title = "Starter Card",
            networkLabel = "VAULT STARTER DEMO",
            startingBalance = 0.0,
            unlimited = false,
            coinPrice = 100L,
            binPrefix = "9901",
            defaultAccentHex = "#38BDF8",
            isAnimated = false,
            badgeText = "STARTER • $0 START"
        ),
        CardCatalogTier(
            tierKey = "VISA_STYLE",
            title = "Azure Crest Demo",
            networkLabel = "AZURE WAVE DEMO",
            startingBalance = 0.0,
            unlimited = false,
            coinPrice = 250L,
            binPrefix = "9902",
            defaultAccentHex = "#2563EB",
            isAnimated = false,
            badgeText = "CLASSIC INSPIRED"
        ),
        CardCatalogTier(
            tierKey = "MC_STYLE",
            title = "Nova Dual Demo",
            networkLabel = "NOVA CREST DEMO",
            startingBalance = 0.0,
            unlimited = false,
            coinPrice = 300L,
            binPrefix = "9903",
            defaultAccentHex = "#EA580C",
            isAnimated = false,
            badgeText = "DUAL ORB INSPIRED"
        ),
        CardCatalogTier(
            tierKey = "SILVER",
            title = "Silver Card",
            networkLabel = "SILVER TIER DEMO",
            startingBalance = 0.0,
            unlimited = false,
            coinPrice = 400L,
            binPrefix = "9904",
            defaultAccentHex = "#94A3B8",
            isAnimated = false,
            badgeText = "SILVER TIER"
        ),
        CardCatalogTier(
            tierKey = "GOLD",
            title = "Gold Card",
            networkLabel = "AURELIA GOLD DEMO",
            startingBalance = 0.0,
            unlimited = false,
            coinPrice = 750L,
            binPrefix = "9905",
            defaultAccentHex = "#F5C451",
            isAnimated = true,
            badgeText = "GOLD TIER"
        ),
        CardCatalogTier(
            tierKey = "PLATINUM",
            title = "Platinum Card",
            networkLabel = "TITANIUM PLATINUM DEMO",
            startingBalance = 0.0,
            unlimited = false,
            coinPrice = 1_200L,
            binPrefix = "9906",
            defaultAccentHex = "#E2E8F0",
            isAnimated = true,
            badgeText = "PLATINUM TIER"
        ),
        CardCatalogTier(
            tierKey = "DIAMOND",
            title = "Diamond Card",
            networkLabel = "PRISM DIAMOND DEMO",
            startingBalance = 0.0,
            unlimited = false,
            coinPrice = 2_000L,
            binPrefix = "9907",
            defaultAccentHex = "#22D3EE",
            isAnimated = true,
            badgeText = "LIMITED EDITION"
        ),
        CardCatalogTier(
            tierKey = "BLACK_ELITE",
            title = "Black Elite Card",
            networkLabel = "OBSIDIAN ELITE DEMO",
            startingBalance = 0.0,
            unlimited = false,
            coinPrice = 3_500L,
            binPrefix = "9908",
            defaultAccentHex = "#FBBF24",
            isAnimated = true,
            badgeText = "ELITE TIER"
        ),
        CardCatalogTier(
            tierKey = "INFINITY",
            title = "Unlimited Infinity Card",
            networkLabel = "QUANTUM INFINITY DEMO",
            startingBalance = 0.0,
            unlimited = false,
            coinPrice = 5_000L,
            binPrefix = "9909",
            defaultAccentHex = "#A855F7",
            isAnimated = true,
            badgeText = "QUANTUM HOLO"
        )
    )
}

data class CartItem(
    val product: MarketplaceProduct,
    val quantity: Int = 1
)

data class PurchaseSuccessReceipt(
    val transactionRef: String,
    val orderId: String?,
    val orderCode: String?,
    val amount: Double,
    val sellerName: String,
    val buyerName: String,
    val cardSummary: String,
    val timestampMillis: Long,
    val itemsSummary: String
)

data class SupportChatMessage(
    val id: String,
    val senderType: SupportSenderType,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

enum class SupportSenderType {
    USER,
    AUTOMATED_AI,
    HUMAN_QUEUE_INFO
}
