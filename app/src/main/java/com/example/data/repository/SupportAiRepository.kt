package com.example.data.repository

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

class SupportAiRepository {
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are the Automated AI Support Assistant for VaultPlay Sim, a 100% fictional " +
                        "entertainment virtual dollar card, QR payment, and seller marketplace simulator. " +
                        "Key facts to remember:\n" +
                        "1. No real money, real Visa/Mastercard networks, or real banks are involved. Everything is SIM USD demo currency.\n" +
                        "2. Users can activate/deactivate virtual cards in My Cards (deactivated cards block simulated purchases until reactivated).\n" +
                        "3. Users can buy new cards in Marketplace -> Card Store using free daily/reward coins.\n" +
                        "4. Users can scan simulator QR codes in Scan & Pay (camera or gallery Photo Picker) or create a Seller Store in Seller Mode.\n" +
                        "5. Refunds can be requested from Order Tracking; for demo platform stores they resolve in-app, and for user sellers the seller approves/declines in Seller Mode, automatically restoring virtual card balance on approval.\n" +
                        "6. Always be concise, helpful, friendly, and remind users when relevant that you are an Automated AI Support Bot and they can file a Human Review Support Ticket in the Tickets tab for manual tracking."
                )
            }
        )
    }

    fun streamSupportReply(userMessage: String, recentHistory: List<Pair<String, String>>): Flow<String> = flow {
        val chat = model.startChat(
            history = recentHistory.takeLast(6).map { (role, msg) ->
                content(role) { text(msg) }
            }
        )
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(userMessage).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulated.append(part)
                    emit(accumulated.toString())
                }
            }
        }
    }
}
