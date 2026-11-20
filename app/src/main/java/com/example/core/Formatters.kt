package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.core.content.FileProvider
import com.google.firebase.Timestamp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object SimFormatters {
    private val usdFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US)
    private val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun formatSimUsd(amount: Double, unlimited: Boolean = false): String {
        return if (unlimited) "UNLIMITED SIM $" else usdFormat.format(amount)
    }

    fun formatCompactUsd(amount: Double): String {
        return when {
            amount >= 1_000_000 -> String.format(Locale.US, "$%.2fM", amount / 1_000_000.0)
            amount >= 1_000 -> String.format(Locale.US, "$%.1fK", amount / 1_000.0)
            else -> usdFormat.format(amount)
        }
    }

    fun formatTimestamp(ts: Timestamp?): String {
        val date = ts?.toDate() ?: Date()
        return dateTimeFormat.format(date)
    }

    fun formatMillis(millis: Long): String {
        return dateTimeFormat.format(Date(millis))
    }

    fun todayKey(): String = dayKeyFormat.format(Date())

    fun generateDemoCardNumber(binPrefix: String = "9901"): String {
        val g2 = Random.nextInt(1000, 9999)
        val g3 = Random.nextInt(1000, 9999)
        val g4 = Random.nextInt(1000, 9999)
        return "$binPrefix $g2 $g3 $g4"
    }

    fun generateDemoCvv(): String = Random.nextInt(100, 999).toString()

    fun generateDemoExpiry(): String {
        val month = Random.nextInt(1, 13)
        val year = Random.nextInt(27, 34)
        return String.format(Locale.US, "%02d/%02d", month, year)
    }

    fun generateRefCode(prefix: String): String {
        val num = Random.nextInt(100000, 999999)
        return "$prefix-$num"
    }
}

enum class PaymentRail(val label: String, val badge: String, val accentHex: String) {
    DOLLAR_PAY("Dollar Pay", "DOLLAR SIM", "#F5C451"),
    ALL_UPI("All UPI", "ALL UPI SIM", "#38BDF8"),
    GPAY("GPay", "GPAY SIM", "#4285F4"),
    PHONEPE("PhonePe", "PHONEPE SIM", "#A855F7"),
    PAYTM("Paytm", "PAYTM SIM", "#00BAF2"),
    BHIM_UPI("BHIM Pay", "BHIM SIM", "#F97316"),
    PAYPAL("PayPal", "PAYPAL SIM", "#3B82F6")
}

data class ParsedSimQr(
    val sellerId: String,
    val storeName: String,
    val suggestedAmount: Double?,
    val rawPayload: String = "",
    val suggestedRail: PaymentRail = PaymentRail.DOLLAR_PAY,
    val isDirectNoDetails: Boolean = false
)

object SimQrCodec {
    private const val PREFIX = "VAULTPLAY_QR"

    fun encode(sellerId: String, storeName: String, suggestedAmount: Double? = null): String {
        val cleanName = storeName.replace("|", " ").trim()
        return if (suggestedAmount != null && suggestedAmount > 0.0) {
            "$PREFIX|$sellerId|$cleanName|$suggestedAmount"
        } else {
            "$PREFIX|$sellerId|$cleanName"
        }
    }

    fun parse(raw: String): ParsedSimQr? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("$PREFIX|")) return null
        val parts = trimmed.split("|")
        if (parts.size < 3) return null
        val sellerId = parts[1].trim()
        val storeName = parts[2].trim()
        if (sellerId.isEmpty() || storeName.isEmpty()) return null
        val amount = parts.getOrNull(3)?.toDoubleOrNull()?.takeIf { it > 0.0 }
        return ParsedSimQr(
            sellerId = sellerId,
            storeName = storeName,
            suggestedAmount = amount,
            rawPayload = trimmed,
            suggestedRail = PaymentRail.DOLLAR_PAY,
            isDirectNoDetails = false
        )
    }

    fun parseUniversal(raw: String): ParsedSimQr? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        // 1. Native VaultPlay seller QR
        parse(trimmed)?.let { return it }

        // 2. UPI QR payload (e.g. upi://pay?pa=...&pn=...&am=...) — never requires user details
        if (trimmed.startsWith("upi://", ignoreCase = true)) {
            val query = trimmed.substringAfter("?", "")
            val params = query.split("&").mapNotNull { kv ->
                val idx = kv.indexOf('=')
                if (idx > 0) kv.substring(0, idx).lowercase() to android.net.Uri.decode(kv.substring(idx + 1)) else null
            }.toMap()
            val amount = params["am"]?.toDoubleOrNull()?.takeIf { it > 0.0 }
            val rail = when {
                trimmed.contains("paytm", ignoreCase = true) -> PaymentRail.PAYTM
                trimmed.contains("ybl", ignoreCase = true) || trimmed.contains("phonepe", ignoreCase = true) -> PaymentRail.PHONEPE
                trimmed.contains("ok", ignoreCase = true) || trimmed.contains("gpay", ignoreCase = true) -> PaymentRail.GPAY
                else -> PaymentRail.ALL_UPI
            }
            return ParsedSimQr(
                sellerId = "",
                storeName = "Direct UPI QR (No Details)",
                suggestedAmount = amount,
                rawPayload = trimmed,
                suggestedRail = rail,
                isDirectNoDetails = true
            )
        }

        // 3. PayPal / any other QR payload — direct pay with zero personal info required
        val isPayPal = trimmed.contains("paypal", ignoreCase = true)
        return ParsedSimQr(
            sellerId = "",
            storeName = if (isPayPal) "Direct PayPal QR (No Details)" else "Direct QR Pay (No Details)",
            suggestedAmount = null,
            rawPayload = trimmed,
            suggestedRail = if (isPayPal) PaymentRail.PAYPAL else PaymentRail.ALL_UPI,
            isDirectNoDetails = true
        )
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}

object SimSoundEffects {
    fun playSuccessChime(enabled: Boolean) {
        if (!enabled) return
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 180)
        } catch (_: Exception) {
        }
    }

    fun playTapTone(enabled: Boolean) {
        if (!enabled) return
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
        } catch (_: Exception) {
        }
    }
}

fun shareDigitalReceipt(context: Context, title: String, receiptText: String) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, receiptText)
        }
        val chooser = Intent.createChooser(sendIntent, "Export Vault Card Receipt")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (_: Exception) {
    }
}

fun saveReceiptToAppFiles(context: Context, fileName: String, content: String): File? {
    return try {
        val dir = File(context.filesDir, "sim_receipts").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content)
        file
    } catch (_: Exception) {
        null
    }
}
