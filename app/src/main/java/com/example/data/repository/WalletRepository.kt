package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.SimFormatters
import com.example.core.handleFirestoreError
import com.example.data.model.CardCatalogTier
import com.example.data.model.SupportTicket
import com.example.data.model.TransactionRecord
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

class WalletRepository(
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

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, "users/$userId")
                }
                throw error
            }

    fun observeCards(userId: String): Flow<List<VirtualCard>> =
        db.collection("users").document(userId).collection("cards")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(VirtualCard::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "users/$userId/cards")
                }
                throw error
            }

    fun observeTransactions(userId: String): Flow<List<TransactionRecord>> =
        db.collection("users").document(userId).collection("transactions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(TransactionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "users/$userId/transactions")
                }
                throw error
            }

    fun observeTickets(userId: String): Flow<List<SupportTicket>> =
        db.collection("users").document(userId).collection("tickets")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, "users/$userId/tickets")
                }
                throw error
            }

    suspend fun ensureUserBootstrapped(displayName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            if (!snap.exists()) {
                val cleanName = displayName.trim().ifEmpty { "Simulator Member" }.take(60)
                val customerNum = 100000 + (abs(uid.hashCode()) % 899999)
                val profileMap = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "customerId" to "VP-$customerNum",
                    "coins" to 1500L,
                    "unlimitedMode" to false,
                    "unlimitedBonusBalance" to 0.0,
                    "totalSpent" to 0.0,
                    "pinEnabled" to false,
                    "simPin" to "1234",
                    "soundEnabled" to true,
                    "lastDailyClaimDay" to "",
                    "sellerAccountId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(profileMap).await()

                val starterCardRef = userRef.collection("cards").document("starter_default")
                val cardMap = mapOf(
                    "userId" to uid,
                    "tierKey" to "STARTER",
                    "cardTitle" to "Starter Card",
                    "cardNetworkLabel" to "VAULT STARTER DEMO",
                    "cardholderName" to cleanName.uppercase().take(48),
                    "cardNumber" to SimFormatters.generateDemoCardNumber("9901"),
                    "expiryDisplay" to "12/99",
                    "cvvDisplay" to SimFormatters.generateDemoCvv(),
                    "balance" to 0.0,
                    "unlimitedBalance" to false,
                    "cardActive" to true,
                    "accentColorHex" to "#38BDF8",
                    "upgradeLevel" to 1L,
                    "animatedStyle" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                starterCardRef.set(cardMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleUnlimitedMode(enabled: Boolean): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "unlimitedMode" to enabled,
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

    suspend fun addVirtualMoney(card: VirtualCard, amount: Double): Result<Unit> {
        val uid = requireUserId()
        require(amount > 0.0 && amount <= 10_000_000.0) { "Amount must be between $1 and $10,000,000 SIM" }
        val cardPath = "users/$uid/cards/${card.id}"
        return try {
            val newBalance = card.balance + amount
            db.collection("users").document(uid).collection("cards").document(card.id)
                .update(
                    mapOf(
                        "balance" to newBalance,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            val txnId = "topup_${System.currentTimeMillis()}"
            val txnMap = mapOf(
                "userId" to uid,
                "transactionRef" to SimFormatters.generateRefCode("TOP"),
                "idempotencyKey" to txnId,
                "type" to "TOP_UP",
                "amount" to amount,
                "currencyLabel" to "SIM USD",
                "merchantName" to "Virtual Vault Top-Up",
                "sellerId" to "",
                "sellerName" to "VaultPlay Mint",
                "cardId" to card.id,
                "cardSummary" to "${card.cardTitle} •••• ${card.cardNumber.takeLast(4)}",
                "orderId" to "",
                "status" to "SIMULATED_SUCCESS",
                "note" to "Simulated virtual dollar mint",
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("transactions").document(txnId)
                .set(txnMap).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, cardPath)
            Result.failure(e)
        }
    }

    suspend fun resetSimulatorBalances(cards: List<VirtualCard>): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val batch = db.batch()
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "unlimitedMode" to false,
                    "unlimitedBonusBalance" to 0.0,
                    "totalSpent" to 0.0,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            cards.forEach { card ->
                batch.update(
                    db.collection("users").document(uid).collection("cards").document(card.id),
                    mapOf(
                        "balance" to 0.0,
                        "unlimitedBalance" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun buyCardFromStore(
        profile: UserProfile,
        tier: CardCatalogTier,
        customHolderName: String,
        customAccentHex: String
    ): Result<VirtualCard> {
        val uid = requireUserId()
        if (profile.coins < tier.coinPrice) {
            return Result.failure(IllegalStateException("Not enough vault coins. Claim daily coins or complete payments!"))
        }
        val path = "users/$uid/cards"
        return try {
            val userRef = db.collection("users").document(uid)
            val cardRef = userRef.collection("cards").document()
            val txnId = "cardbuy_${System.currentTimeMillis()}"
            val txnRef = userRef.collection("transactions").document(txnId)
            val cleanHolder = customHolderName.trim().ifEmpty { profile.displayName }.uppercase().take(48)
            val cardNumber = SimFormatters.generateDemoCardNumber(tier.binPrefix)
            val expiry = SimFormatters.generateDemoExpiry()
            val cvv = SimFormatters.generateDemoCvv()

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val currentCoins = userSnap.getLong("coins") ?: 0L
                if (currentCoins < tier.coinPrice) {
                    throw IllegalStateException("Insufficient vault coins.")
                }
                tx.update(
                    userRef,
                    mapOf(
                        "coins" to (currentCoins - tier.coinPrice),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    cardRef,
                    mapOf(
                        "userId" to uid,
                        "tierKey" to tier.tierKey,
                        "cardTitle" to tier.title,
                        "cardNetworkLabel" to tier.networkLabel,
                        "cardholderName" to cleanHolder,
                        "cardNumber" to cardNumber,
                        "expiryDisplay" to expiry,
                        "cvvDisplay" to cvv,
                        "balance" to tier.startingBalance,
                        "unlimitedBalance" to tier.unlimited,
                        "cardActive" to true,
                        "accentColorHex" to customAccentHex,
                        "upgradeLevel" to 1L,
                        "animatedStyle" to tier.isAnimated,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txnRef,
                    mapOf(
                        "userId" to uid,
                        "transactionRef" to SimFormatters.generateRefCode("CRD"),
                        "idempotencyKey" to txnId,
                        "type" to "CARD_PURCHASE",
                        "amount" to tier.startingBalance,
                        "currencyLabel" to "SIM USD",
                        "merchantName" to "Card Store • ${tier.title}",
                        "sellerId" to "",
                        "sellerName" to "Vault Card Mint",
                        "cardId" to cardRef.id,
                        "cardSummary" to tier.title,
                        "orderId" to "",
                        "status" to "SIMULATED_SUCCESS",
                        "note" to "Purchased for ${tier.coinPrice} vault coins",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            val createdCard = VirtualCard(
                id = cardRef.id,
                userId = uid,
                tierKey = tier.tierKey,
                cardTitle = tier.title,
                cardNetworkLabel = tier.networkLabel,
                cardholderName = cleanHolder,
                cardNumber = cardNumber,
                expiryDisplay = expiry,
                cvvDisplay = cvv,
                balance = tier.startingBalance,
                unlimitedBalance = tier.unlimited,
                cardActive = true,
                accentColorHex = customAccentHex,
                upgradeLevel = 1L,
                animatedStyle = tier.isAnimated
            )
            Result.success(createdCard)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleCardActive(cardId: String, active: Boolean): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/cards/$cardId"
        return try {
            db.collection("users").document(uid).collection("cards").document(cardId)
                .update(
                    mapOf(
                        "cardActive" to active,
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

    suspend fun customizeAndUpgradeCard(
        profile: UserProfile,
        card: VirtualCard,
        newHolderName: String,
        newAccentHex: String,
        upgrade: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val upgradeCost = 200L
        if (upgrade && profile.coins < upgradeCost) {
            return Result.failure(IllegalStateException("Upgrade requires $upgradeCost simulator coins."))
        }
        if (upgrade && card.upgradeLevel >= 10L) {
            return Result.failure(IllegalStateException("Card is already at max level 10."))
        }
        val path = "users/$uid/cards/${card.id}"
        return try {
            val userRef = db.collection("users").document(uid)
            val cardRef = userRef.collection("cards").document(card.id)
            val cleanHolder = newHolderName.trim().ifEmpty { card.cardholderName }.uppercase().take(48)
            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val currentCoins = userSnap.getLong("coins") ?: 0L
                val nextLevel = if (upgrade) (card.upgradeLevel + 1L).coerceAtMost(10L) else card.upgradeLevel
                if (upgrade) {
                    if (currentCoins < upgradeCost) throw IllegalStateException("Insufficient coins.")
                    tx.update(
                        userRef,
                        mapOf(
                            "coins" to (currentCoins - upgradeCost),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                tx.update(
                    cardRef,
                    mapOf(
                        "cardholderName" to cleanHolder,
                        "accentColorHex" to newAccentHex,
                        "upgradeLevel" to nextLevel,
                        "animatedStyle" to (card.animatedStyle || nextLevel >= 2L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun claimDailyCoins(profile: UserProfile): Result<Long> {
        val uid = requireUserId()
        val today = SimFormatters.todayKey()
        if (profile.lastDailyClaimDay == today) {
            return Result.failure(IllegalStateException("Daily reward already claimed today! Come back tomorrow."))
        }
        val reward = 500L
        val path = "users/$uid"
        return try {
            val userRef = db.collection("users").document(uid)
            val txnId = "daily_$today"
            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                val currentCoins = snap.getLong("coins") ?: 0L
                val lastDay = snap.getString("lastDailyClaimDay") ?: ""
                if (lastDay == today) throw IllegalStateException("Already claimed today.")
                tx.update(
                    userRef,
                    mapOf(
                        "coins" to (currentCoins + reward),
                        "lastDailyClaimDay" to today,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    userRef.collection("transactions").document(txnId),
                    mapOf(
                        "userId" to uid,
                        "transactionRef" to SimFormatters.generateRefCode("REW"),
                        "idempotencyKey" to txnId,
                        "type" to "DAILY_REWARD",
                        "amount" to reward.toDouble(),
                        "currencyLabel" to "SIM COINS",
                        "merchantName" to "Daily Login Reward",
                        "sellerId" to "",
                        "sellerName" to "VaultPlay Rewards",
                        "cardId" to "",
                        "cardSummary" to "Coin Vault",
                        "orderId" to "",
                        "status" to "SIMULATED_SUCCESS",
                        "note" to "Daily +$reward simulator coins",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(reward)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSettings(pinEnabled: Boolean, simPin: String, soundEnabled: Boolean): Result<Unit> {
        val uid = requireUserId()
        val cleanPin = simPin.filter { it.isDigit() }.take(4).padStart(4, '0')
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "pinEnabled" to pinEnabled,
                    "simPin" to cleanPin,
                    "soundEnabled" to soundEnabled,
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

    suspend fun createSupportTicket(subject: String, category: String, description: String): Result<String> {
        val uid = requireUserId()
        val cleanSub = subject.trim().take(120)
        val cleanDesc = description.trim().take(1000)
        if (cleanSub.length < 3) return Result.failure(IllegalArgumentException("Subject must be at least 3 characters."))
        if (cleanDesc.length < 5) return Result.failure(IllegalArgumentException("Please describe your issue (5+ characters)."))
        val path = "users/$uid/tickets"
        return try {
            val code = SimFormatters.generateRefCode("SUP")
            val ticketRef = db.collection("users").document(uid).collection("tickets").document()
            ticketRef.set(
                mapOf(
                    "userId" to uid,
                    "ticketCode" to code,
                    "subject" to cleanSub,
                    "description" to cleanDesc,
                    "category" to category.take(40),
                    "status" to "OPEN",
                    "resolutionNote" to "Queued for simulator support review.",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSupportTicketStatus(ticketId: String, status: String, resolutionNote: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/tickets/$ticketId"
        return try {
            db.collection("users").document(uid).collection("tickets").document(ticketId)
                .update(
                    mapOf(
                        "status" to status,
                        "resolutionNote" to resolutionNote.take(500),
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
}
