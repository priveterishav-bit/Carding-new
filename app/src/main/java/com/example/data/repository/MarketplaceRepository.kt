package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.SimFormatters
import com.example.core.SimQrCodec
import com.example.core.handleFirestoreError
import com.example.data.model.CartItem
import com.example.data.model.MarketplaceProduct
import com.example.data.model.OrderStage
import com.example.data.model.PurchaseSuccessReceipt
import com.example.data.model.SellerProfile
import com.example.data.model.SellerReview
import com.example.data.model.SimulatorOrder
import com.example.data.model.TrackingEvent
import com.example.data.model.UserProfile
import com.example.data.model.VirtualCard
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

class MarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google.")
    }

    fun observeSellers(): Flow<List<SellerProfile>> =
        db.collection("sellers")
            .whereEqualTo("demoListed", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SellerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.completedOrdersCount }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "sellers")
                }
                throw error
            }

    fun observeProducts(): Flow<List<MarketplaceProduct>> =
        db.collection("products")
            .whereEqualTo("demoListed", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MarketplaceProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.price }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "products")
                }
                throw error
            }

    fun observeBuyerOrders(buyerUid: String): Flow<List<SimulatorOrder>> =
        db.collection("orders")
            .whereEqualTo("buyerUid", buyerUid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SimulatorOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == buyerUid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "orders")
                }
                throw error
            }

    fun observeSellerOrders(sellerOwnerUid: String): Flow<List<SimulatorOrder>> =
        db.collection("orders")
            .whereEqualTo("sellerOwnerUid", sellerOwnerUid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SimulatorOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == sellerOwnerUid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "orders")
                }
                throw error
            }

    fun observeReviews(): Flow<List<SellerReview>> =
        db.collection("reviews")
            .whereEqualTo("demoListed", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SellerReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "reviews")
                }
                throw error
            }

    suspend fun ensureDemoMarketplaceSeeded(): Result<Unit> {
        if (auth.currentUser == null) return Result.success(Unit)
        return try {
            val checkSnap = db.collection("sellers").document("demo_store_apex").get().await()
            if (checkSnap.exists()) return Result.success(Unit)

            val demoStores = listOf(
                Triple("demo_store_apex", "Apex Tech Gear", "Electronics") to Pair("#38BDF8", "Curated simulated gadgets, smartwatches & audio drops."),
                Triple("demo_store_luxe", "Luxe Watch Vault", "Luxury") to Pair("#F5C451", "Virtual collector timepieces & titanium accessories."),
                Triple("demo_store_neon", "Neon Studio Audio", "Audio") to Pair("#A855F7", "Studio headphones, DACs & spatial sound gear."),
                Triple("demo_store_obsidian", "Obsidian Apparel", "Lifestyle") to Pair("#10B981", "Minimalist monochrome techwear & travel packs.")
            )

            demoStores.forEachIndexed { idx, (idNameCat, toneDesc) ->
                val (id, name, cat) = idNameCat
                val (tone, desc) = toneDesc
                db.collection("sellers").document(id).set(
                    mapOf(
                        "sellerOwnerUid" to "demo_platform",
                        "sellerCode" to "SEL-DEMO0${idx + 1}",
                        "storeName" to name,
                        "description" to desc,
                        "category" to cat,
                        "logoToneHex" to tone,
                        "demoVerified" to true,
                        "demoListed" to true,
                        "qrPayload" to SimQrCodec.encode(id, name),
                        "ratingSum" to 24L,
                        "reviewCount" to 5L,
                        "completedOrdersCount" to (18L + idx * 7L),
                        "virtualEarnings" to (4200.0 + idx * 1850.0),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
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

            demoProducts.forEachIndexed { idx, (ids, details) ->
                val (prodId, sellerId, storeName) = ids
                val (prodName, desc, price) = details
                db.collection("products").document(prodId).set(
                    mapOf(
                        "sellerId" to sellerId,
                        "sellerOwnerUid" to "demo_platform",
                        "storeName" to storeName,
                        "name" to prodName,
                        "description" to desc,
                        "category" to if (idx < 2) "Electronics" else if (idx < 4) "Luxury" else if (idx < 6) "Audio" else "Lifestyle",
                        "price" to price,
                        "stockCount" to 50L,
                        "iconKey" to when (idx) {
                            0, 2 -> "watch"
                            4, 5 -> "headphones"
                            6 -> "backpack"
                            else -> "devices"
                        },
                        "demoListed" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "sellers/demo_store_apex")
            Result.failure(e)
        }
    }

    suspend fun processVirtualPayment(
        profile: UserProfile,
        card: VirtualCard,
        amount: Double,
        merchantName: String,
        seller: SellerProfile?,
        cartItems: List<CartItem>,
        idempotencyKey: String
    ): Result<PurchaseSuccessReceipt> {
        val uid = requireUserId()
        if (!card.cardActive) {
            return Result.failure(IllegalStateException("CARD DEACTIVATED: Reactivate this card in My Cards before making simulated purchases."))
        }
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Payment amount must be greater than $0."))
        }

        val userRef = db.collection("users").document(uid)
        val cardRef = userRef.collection("cards").document(card.id)
        val txnRef = userRef.collection("transactions").document(idempotencyKey)
        val txnCode = SimFormatters.generateRefCode("TXN")
        val nowMillis = System.currentTimeMillis()
        val cardSummary = "${card.cardTitle} •••• ${card.cardNumber.takeLast(4)}"
        val resolvedMerchant = seller?.storeName ?: merchantName.trim().ifEmpty { "Demo Merchant" }.take(80)
        val itemsSummary = if (cartItems.isNotEmpty()) {
            cartItems.joinToString(", ") { "${it.quantity}x ${it.product.name}" }.take(240)
        } else {
            "QR / Direct Virtual Payment to $resolvedMerchant".take(240)
        }
        val itemCount = cartItems.sumOf { it.quantity }.coerceAtLeast(1).toLong()

        return try {
            val orderDocRef = if (seller != null) db.collection("orders").document() else null
            val orderCode = if (seller != null) SimFormatters.generateRefCode("ORD") else null

            // Prevent duplicate payment via deterministic idempotencyKey document check
            val existingTxn = txnRef.get().await()
            if (existingTxn.exists()) {
                return Result.failure(IllegalStateException("Duplicate simulated payment prevented (Ref: $idempotencyKey)."))
            }

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val cardSnap = tx.get(cardRef)
                val sellerSnap = if (seller != null) tx.get(db.collection("sellers").document(seller.id)) else null

                val activeNow = cardSnap.getBoolean("cardActive") ?: true
                if (!activeNow) throw IllegalStateException("CARD DEACTIVATED")
                val currentBalance = cardSnap.getDouble("balance") ?: 0.0
                val cardUnlimited = (cardSnap.getBoolean("unlimitedBalance") ?: false) || (userSnap.getBoolean("unlimitedMode") ?: false)

                val currentSpent = userSnap.getDouble("totalSpent") ?: 0.0
                val currentCoins = userSnap.getLong("coins") ?: 0L

                val nextBalance = if (cardUnlimited) currentBalance else (currentBalance + amount)
                tx.update(
                    cardRef,
                    mapOf(
                        "balance" to nextBalance,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    userRef,
                    mapOf(
                        "totalSpent" to (currentSpent + amount),
                        "coins" to (currentCoins + 25L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    txnRef,
                    mapOf(
                        "userId" to uid,
                        "transactionRef" to txnCode,
                        "idempotencyKey" to idempotencyKey,
                        "type" to "PAYMENT",
                        "amount" to amount,
                        "currencyLabel" to "SIM USD",
                        "merchantName" to resolvedMerchant,
                        "sellerId" to (seller?.id ?: ""),
                        "sellerName" to (seller?.storeName ?: resolvedMerchant),
                        "cardId" to card.id,
                        "cardSummary" to cardSummary,
                        "orderId" to (orderDocRef?.id ?: ""),
                        "status" to "SIMULATED_SUCCESS",
                        "note" to itemsSummary.take(200),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                if (seller != null && sellerSnap != null && sellerSnap.exists() && orderDocRef != null && orderCode != null) {
                    val prevEarnings = sellerSnap.getDouble("virtualEarnings") ?: 0.0
                    val prevOrders = sellerSnap.getLong("completedOrdersCount") ?: 0L
                    tx.update(
                        db.collection("sellers").document(seller.id),
                        mapOf(
                            "virtualEarnings" to (prevEarnings + amount),
                            "completedOrdersCount" to (prevOrders + 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val initialEvents = listOf(
                        mapOf(
                            "stage" to OrderStage.ORDER_PLACED.key,
                            "label" to OrderStage.ORDER_PLACED.label,
                            "timestampMillis" to nowMillis
                        ),
                        mapOf(
                            "stage" to OrderStage.PAYMENT_SIMULATED.key,
                            "label" to OrderStage.PAYMENT_SIMULATED.label,
                            "timestampMillis" to (nowMillis + 50L)
                        )
                    )
                    tx.set(
                        orderDocRef,
                        mapOf(
                            "orderCode" to orderCode,
                            "buyerUid" to uid,
                            "buyerName" to profile.displayName,
                            "sellerId" to seller.id,
                            "sellerOwnerUid" to seller.sellerOwnerUid,
                            "sellerName" to seller.storeName,
                            "itemsSummary" to itemsSummary,
                            "itemCount" to itemCount,
                            "totalAmount" to amount,
                            "cardId" to card.id,
                            "cardSummary" to cardSummary,
                            "transactionRef" to txnCode,
                            "status" to OrderStage.PAYMENT_SIMULATED.key,
                            "trackingEvents" to initialEvents,
                            "refundStatus" to "NONE",
                            "refundReason" to "",
                            "sellerResponse" to "",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            Result.success(
                PurchaseSuccessReceipt(
                    transactionRef = txnCode,
                    orderId = orderDocRef?.id,
                    orderCode = orderCode,
                    amount = amount,
                    sellerName = resolvedMerchant,
                    buyerName = profile.displayName,
                    cardSummary = cardSummary,
                    timestampMillis = nowMillis,
                    itemsSummary = itemsSummary
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/transactions/$idempotencyKey")
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateSellerStore(
        existingSeller: SellerProfile?,
        storeName: String,
        description: String,
        category: String,
        logoToneHex: String
    ): Result<SellerProfile> {
        val uid = requireUserId()
        val cleanName = storeName.trim().take(60)
        if (cleanName.length < 2) return Result.failure(IllegalArgumentException("Store name must be at least 2 characters."))
        val sellerId = existingSeller?.id?.takeIf { it.isNotBlank() } ?: "seller_${uid.take(24)}"
        val path = "sellers/$sellerId"
        return try {
            val qrPayload = SimQrCodec.encode(sellerId, cleanName)
            if (existingSeller == null) {
                val sellerCode = "SEL-${(10000 + abs(uid.hashCode()) % 89999)}"
                val payload = mapOf(
                    "sellerOwnerUid" to uid,
                    "sellerCode" to sellerCode,
                    "storeName" to cleanName,
                    "description" to description.trim().take(300),
                    "category" to category.trim().ifEmpty { "General" }.take(40),
                    "logoToneHex" to logoToneHex,
                    "demoVerified" to true,
                    "demoListed" to true,
                    "qrPayload" to qrPayload,
                    "ratingSum" to 0L,
                    "reviewCount" to 0L,
                    "completedOrdersCount" to 0L,
                    "virtualEarnings" to 0.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("sellers").document(sellerId).set(payload).await()
                db.collection("users").document(uid).update(
                    mapOf("sellerAccountId" to sellerId, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
                val created = db.collection("sellers").document(sellerId).get().await()
                    .toObject(SellerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: SellerProfile(id = sellerId, sellerOwnerUid = uid, sellerCode = sellerCode, storeName = cleanName, qrPayload = qrPayload)
                Result.success(created)
            } else {
                db.collection("sellers").document(sellerId).update(
                    mapOf(
                        "storeName" to cleanName,
                        "description" to description.trim().take(300),
                        "category" to category.trim().ifEmpty { "General" }.take(40),
                        "logoToneHex" to logoToneHex,
                        "qrPayload" to qrPayload,
                        "demoListed" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(existingSeller.copy(storeName = cleanName, description = description, category = category, logoToneHex = logoToneHex, qrPayload = qrPayload))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveSellerProduct(
        seller: SellerProfile,
        existingProduct: MarketplaceProduct?,
        name: String,
        description: String,
        category: String,
        price: Double,
        stockCount: Long
    ): Result<Unit> {
        val uid = requireUserId()
        val cleanName = name.trim().take(80)
        if (cleanName.length < 2) return Result.failure(IllegalArgumentException("Product name too short."))
        if (price <= 0.0 || price > 10_000_000.0) return Result.failure(IllegalArgumentException("Enter a valid price."))
        return try {
            if (existingProduct == null) {
                val docRef = db.collection("products").document()
                docRef.set(
                    mapOf(
                        "sellerId" to seller.id,
                        "sellerOwnerUid" to uid,
                        "storeName" to seller.storeName,
                        "name" to cleanName,
                        "description" to description.trim().take(300),
                        "category" to category.trim().ifEmpty { seller.category }.take(40),
                        "price" to price,
                        "stockCount" to stockCount.coerceAtLeast(1L),
                        "iconKey" to "shopping",
                        "demoListed" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                db.collection("products").document(existingProduct.id).update(
                    mapOf(
                        "name" to cleanName,
                        "description" to description.trim().take(300),
                        "category" to category.trim().ifEmpty { seller.category }.take(40),
                        "price" to price,
                        "stockCount" to stockCount.coerceAtLeast(0L),
                        "iconKey" to existingProduct.iconKey,
                        "demoListed" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "products")
            Result.failure(e)
        }
    }

    suspend fun advanceOrderStage(order: SimulatorOrder, nextStage: OrderStage): Result<Unit> {
        requireUserId()
        val path = "orders/${order.id}"
        return try {
            val updatedEvents = (order.trackingEvents + TrackingEvent(
                stage = nextStage.key,
                label = nextStage.label,
                timestampMillis = System.currentTimeMillis()
            )).takeLast(20).map {
                mapOf("stage" to it.stage, "label" to it.label, "timestampMillis" to it.timestampMillis)
            }
            db.collection("orders").document(order.id).update(
                mapOf(
                    "status" to nextStage.key,
                    "trackingEvents" to updatedEvents,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelBuyerOrder(order: SimulatorOrder): Result<Unit> {
        val uid = requireUserId()
        if (order.status !in listOf("ORDER_PLACED", "PAYMENT_SIMULATED")) {
            return Result.failure(IllegalStateException("Order can only be cancelled before seller confirmation."))
        }
        val path = "orders/${order.id}"
        return try {
            val updatedEvents = (order.trackingEvents + TrackingEvent(
                stage = "CANCELLED",
                label = "Cancelled by Buyer",
                timestampMillis = System.currentTimeMillis()
            )).takeLast(20).map {
                mapOf("stage" to it.stage, "label" to it.label, "timestampMillis" to it.timestampMillis)
            }
            db.collection("orders").document(order.id).update(
                mapOf(
                    "status" to "CANCELLED",
                    "trackingEvents" to updatedEvents,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            restoreBuyerRefundIfNeeded(uid, order, reason = "Cancelled simulator order ${order.orderCode}")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun requestOrderRefund(order: SimulatorOrder, reason: String): Result<Unit> {
        requireUserId()
        val cleanReason = reason.trim().ifEmpty { "Simulator refund request" }.take(300)
        val path = "orders/${order.id}"
        return try {
            db.collection("orders").document(order.id).update(
                mapOf(
                    "refundStatus" to "REQUESTED",
                    "refundReason" to cleanReason,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveOrderRefund(order: SimulatorOrder, approve: Boolean, sellerResponse: String): Result<Unit> {
        val uid = requireUserId()
        val cleanReply = sellerResponse.trim().ifEmpty {
            if (approve) "Simulated refund approved. Balance restored." else "Refund declined by seller."
        }.take(300)
        val path = "orders/${order.id}"
        return try {
            val nextEvents = (order.trackingEvents + TrackingEvent(
                stage = if (approve) "REFUNDED" else order.status,
                label = if (approve) "Refund Approved & Credited" else "Refund Declined",
                timestampMillis = System.currentTimeMillis()
            )).takeLast(20).map {
                mapOf("stage" to it.stage, "label" to it.label, "timestampMillis" to it.timestampMillis)
            }
            db.collection("orders").document(order.id).update(
                mapOf(
                    "status" to if (approve) "REFUNDED" else order.status,
                    "refundStatus" to if (approve) "APPROVED" else "DECLINED",
                    "sellerResponse" to cleanReply,
                    "trackingEvents" to nextEvents,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (approve && order.buyerUid == uid) {
                restoreBuyerRefundIfNeeded(uid, order, reason = cleanReply)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // Automatically restores buyer virtual card balance once when an order is REFUNDED / CANCELLED
    suspend fun restoreBuyerRefundIfNeeded(buyerUid: String, order: SimulatorOrder, reason: String = "Approved refund"): Result<Unit> {
        if (auth.currentUser?.uid != buyerUid) return Result.success(Unit)
        val refundTxnId = "refund_${order.id}"
        val userRef = db.collection("users").document(buyerUid)
        val refundTxnRef = userRef.collection("transactions").document(refundTxnId)
        return try {
            val existing = refundTxnRef.get().await()
            if (existing.exists()) return Result.success(Unit)
            refundTxnRef.set(
                mapOf(
                    "userId" to buyerUid,
                    "transactionRef" to SimFormatters.generateRefCode("RFD"),
                    "idempotencyKey" to refundTxnId,
                    "type" to "REFUND_CREDIT",
                    "amount" to order.totalAmount,
                    "currencyLabel" to "SIM USD",
                    "merchantName" to "Refund • ${order.sellerName}",
                    "sellerId" to order.sellerId,
                    "sellerName" to order.sellerName,
                    "cardId" to order.cardId,
                    "cardSummary" to order.cardSummary,
                    "orderId" to order.id,
                    "status" to "REFUNDED",
                    "note" to reason.take(200),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$buyerUid/transactions/$refundTxnId")
            Result.failure(e)
        }
    }

    suspend fun submitSellerReview(
        profile: UserProfile,
        seller: SellerProfile,
        orderId: String,
        rating: Int,
        comment: String
    ): Result<Unit> {
        val uid = requireUserId()
        val boundedRating = rating.coerceIn(1, 5).toLong()
        return try {
            val sellerRef = db.collection("sellers").document(seller.id)
            val reviewRef = db.collection("reviews").document()
            db.runTransaction { tx ->
                val sellerSnap = tx.get(sellerRef)
                val curSum = sellerSnap.getLong("ratingSum") ?: 0L
                val curCount = sellerSnap.getLong("reviewCount") ?: 0L
                tx.update(
                    sellerRef,
                    mapOf(
                        "ratingSum" to (curSum + boundedRating),
                        "reviewCount" to (curCount + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    reviewRef,
                    mapOf(
                        "sellerId" to seller.id,
                        "orderId" to orderId,
                        "buyerUid" to uid,
                        "buyerName" to profile.displayName,
                        "rating" to boundedRating,
                        "comment" to comment.trim().take(300),
                        "demoListed" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "reviews")
            Result.failure(e)
        }
    }
}
