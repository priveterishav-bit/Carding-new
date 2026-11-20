package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.SimFormatters
import com.example.data.model.VirtualCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RubyError
import kotlinx.coroutines.delay

/**
 * Custom Canvas-drawn fictional simulator network emblems (strictly non-trademarked demo marks).
 */
@Composable
fun VaultNetworkLogo(
    tierKey: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val monogram = remember(tierKey) {
        when (tierKey) {
            "VISA_STYLE" -> "AZURE WAVE"
            "MC_STYLE" -> "NOVA DUAL"
            "SILVER" -> "SILVER NET"
            "GOLD" -> "AURELIA"
            "PLATINUM" -> "TITANIUM"
            "DIAMOND" -> "PRISM NET"
            "BLACK_ELITE" -> "OBSIDIAN"
            "INFINITY" -> "QUANTUM"
            else -> "VAULT NET"
        }
    }

    Surface(
        color = Color.Black.copy(alpha = 0.42f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.9.dp, accentColor.copy(alpha = 0.65f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 4.dp else 5.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(width = if (compact) 22.dp else 28.dp, height = if (compact) 14.dp else 18.dp)) {
                val w = size.width
                val h = size.height
                when (tierKey) {
                    "MC_STYLE" -> {
                        val r = h * 0.46f
                        drawCircle(
                            color = Color(0xFFFB923C).copy(alpha = 0.9f),
                            radius = r,
                            center = Offset(w * 0.36f, h * 0.5f)
                        )
                        drawCircle(
                            color = MetallicGold.copy(alpha = 0.88f),
                            radius = r,
                            center = Offset(w * 0.64f, h * 0.5f)
                        )
                    }

                    "VISA_STYLE" -> {
                        val path1 = Path().apply {
                            moveTo(w * 0.08f, h * 0.78f)
                            lineTo(w * 0.38f, h * 0.18f)
                            lineTo(w * 0.62f, h * 0.18f)
                            lineTo(w * 0.32f, h * 0.78f)
                            close()
                        }
                        val path2 = Path().apply {
                            moveTo(w * 0.46f, h * 0.82f)
                            lineTo(w * 0.72f, h * 0.22f)
                            lineTo(w * 0.92f, h * 0.22f)
                            lineTo(w * 0.66f, h * 0.82f)
                            close()
                        }
                        drawPath(path1, color = ElectricCyan)
                        drawPath(path2, color = accentColor)
                    }

                    "DIAMOND", "PLATINUM" -> {
                        val diamond = Path().apply {
                            moveTo(w * 0.5f, h * 0.06f)
                            lineTo(w * 0.88f, h * 0.5f)
                            lineTo(w * 0.5f, h * 0.94f)
                            lineTo(w * 0.12f, h * 0.5f)
                            close()
                        }
                        drawPath(diamond, color = accentColor.copy(alpha = 0.28f))
                        drawPath(diamond, color = accentColor, style = Stroke(width = 2.2f))
                        drawLine(
                            color = Color.White.copy(alpha = 0.75f),
                            start = Offset(w * 0.24f, h * 0.5f),
                            end = Offset(w * 0.76f, h * 0.5f),
                            strokeWidth = 1.6f
                        )
                    }

                    "GOLD", "BLACK_ELITE" -> {
                        val hex = Path().apply {
                            moveTo(w * 0.28f, h * 0.12f)
                            lineTo(w * 0.72f, h * 0.12f)
                            lineTo(w * 0.92f, h * 0.5f)
                            lineTo(w * 0.72f, h * 0.88f)
                            lineTo(w * 0.28f, h * 0.88f)
                            lineTo(w * 0.08f, h * 0.5f)
                            close()
                        }
                        drawPath(hex, color = MetallicGold.copy(alpha = 0.25f))
                        drawPath(hex, color = MetallicGold, style = Stroke(width = 2.2f))
                        drawCircle(color = MetallicGold, radius = h * 0.16f, center = Offset(w * 0.5f, h * 0.5f))
                    }

                    "INFINITY" -> {
                        val r = h * 0.34f
                        drawCircle(
                            color = Color(0xFFC084FC),
                            radius = r,
                            center = Offset(w * 0.34f, h * 0.5f),
                            style = Stroke(width = 2.6f, cap = StrokeCap.Round)
                        )
                        drawCircle(
                            color = ElectricCyan,
                            radius = r,
                            center = Offset(w * 0.66f, h * 0.5f),
                            style = Stroke(width = 2.6f, cap = StrokeCap.Round)
                        )
                    }

                    else -> {
                        val chev1 = Path().apply {
                            moveTo(w * 0.15f, h * 0.2f)
                            lineTo(w * 0.48f, h * 0.5f)
                            lineTo(w * 0.15f, h * 0.8f)
                        }
                        val chev2 = Path().apply {
                            moveTo(w * 0.50f, h * 0.2f)
                            lineTo(w * 0.84f, h * 0.5f)
                            lineTo(w * 0.50f, h * 0.8f)
                        }
                        drawPath(chev1, color = accentColor, style = Stroke(width = 2.6f, cap = StrokeCap.Round))
                        drawPath(chev2, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = 2.6f, cap = StrokeCap.Round))
                    }
                }
            }
            if (!compact) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = monogram,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accentColor,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultCardDetailsBottomSheet(
    card: VirtualCard,
    globalUnlimitedMode: Boolean,
    onDismiss: () -> Unit,
    onPayClick: (VirtualCard) -> Unit,
    onTopUpClick: (VirtualCard) -> Unit,
    onToggleActiveClick: (VirtualCard) -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: ((VirtualCard) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var revealSensitive by remember(card.id) { mutableStateOf(true) }
    var copiedLabel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiedLabel) {
        if (copiedLabel != null) {
            delay(1800)
            copiedLabel = null
        }
    }

    val accentColor = remember(card.accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(card.accentColorHex))
        } catch (_: Exception) {
            MetallicGold
        }
    }

    val (bgStart, bgEnd) = remember(card.tierKey) {
        when (card.tierKey) {
            "STARTER", "VISA_STYLE" -> Color(0xFF0A1931) to Color(0xFF1E3A8A)
            "MC_STYLE" -> Color(0xFF1F1124) to Color(0xFF7C2D12)
            "SILVER" -> Color(0xFF1E293B) to Color(0xFF475569)
            "GOLD" -> Color(0xFF271C06) to Color(0xFF78510B)
            "PLATINUM" -> Color(0xFF171E2E) to Color(0xFF334155)
            "DIAMOND" -> Color(0xFF082032) to Color(0xFF0E7490)
            "BLACK_ELITE" -> Color(0xFF05070D) to Color(0xFF18181B)
            "INFINITY" -> Color(0xFF1A0B2E) to Color(0xFF311042)
            else -> Color(0xFF0F172A) to Color(0xFF1E293B)
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
        copiedLabel = "$label copied"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .testTag("card_details_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Network Logo + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.cardTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${card.cardNetworkLabel} • Level ${card.upgradeLevel}/10",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                VaultNetworkLogo(
                    tierKey = card.tierKey,
                    accentColor = accentColor
                )
            }

            SimDisclaimerBanner("VIRTUAL VAULT CARD • NOT REAL MONEY • NOT FOR REAL PAYMENTS")

            // Luxury Balance & Status Strip
            Surface(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(bgStart, bgEnd)))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Account Balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = SimFormatters.formatSimUsd(
                                    card.balance,
                                    unlimited = card.unlimitedBalance || globalUnlimitedMode
                                ),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                            Text(
                                text = if (card.animatedStyle) "Holographic Shimmer Enabled" else "Standard Metallic Finish",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                        Surface(
                            color = (if (card.cardActive) EmeraldSuccess else RubyError).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, if (card.cardActive) EmeraldSuccess else RubyError)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (card.cardActive) Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = if (card.cardActive) "Card active" else "Card deactivated",
                                    tint = if (card.cardActive) EmeraldSuccess else RubyError,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (card.cardActive) "ACTIVE" else "LOCKED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (card.cardActive) EmeraldSuccess else RubyError
                                )
                            }
                        }
                    }
                }
            }

            // Detailed Vault Card Credentials Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Virtual Vault Credentials",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        IconButton(
                            onClick = { revealSensitive = !revealSensitive },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("sheet_toggle_reveal_btn")
                        ) {
                            Icon(
                                imageVector = if (revealSensitive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (revealSensitive) "Mask vault card details" else "Show vault card details",
                                tint = ElectricCyan
                            )
                        }
                    }

                    val displayedNumber = if (revealSensitive) {
                        card.cardNumber
                    } else {
                        "•••• •••• •••• ${card.cardNumber.takeLast(4)}"
                    }
                    val displayedCvv = if (revealSensitive) card.cvvDisplay else "•••"

                    CredentialCopyRow(
                        label = "Vault Card Number",
                        value = displayedNumber,
                        mono = true,
                        copyTestTag = "sheet_copy_number_btn",
                        onCopy = { copyToClipboard("Vault card number", card.cardNumber) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    CredentialCopyRow(
                        label = "Cardholder Name",
                        value = card.cardholderName,
                        mono = false,
                        copyTestTag = "sheet_copy_holder_btn",
                        onCopy = { copyToClipboard("Cardholder name", card.cardholderName) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CredentialCopyRow(
                                label = "Vault Expiry",
                                value = card.expiryDisplay,
                                mono = true,
                                copyTestTag = "sheet_copy_expiry_btn",
                                onCopy = { copyToClipboard("Vault expiry", card.expiryDisplay) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CredentialCopyRow(
                                label = "Vault CVV",
                                value = displayedCvv,
                                mono = true,
                                copyTestTag = "sheet_copy_cvv_btn",
                                onCopy = { copyToClipboard("Vault CVV", card.cvvDisplay) }
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Simulated Network & BIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${card.cardNetworkLabel} • BIN ${card.cardNumber.take(4)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                        VaultNetworkLogo(
                            tierKey = card.tierKey,
                            accentColor = accentColor,
                            compact = true
                        )
                    }
                }
            }

            AnimatedVisibility(visible = copiedLabel != null) {
                Surface(
                    color = EmeraldSuccess.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${copiedLabel ?: ""} (for in-app virtual use only)",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldSuccess
                        )
                    }
                }
            }

            // Primary Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onPayClick(card) },
                    enabled = card.cardActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sheet_pay_btn")
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pay", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { onTopUpClick(card) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sheet_topup_btn")
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Top Up", maxLines = 1)
                }
            }

            // Secondary Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onToggleActiveClick(card) },
                    border = BorderStroke(
                        1.dp,
                        if (card.cardActive) RubyError.copy(alpha = 0.7f) else EmeraldSuccess.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sheet_toggle_active_btn")
                ) {
                    Icon(
                        imageVector = if (card.cardActive) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (card.cardActive) RubyError else EmeraldSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (card.cardActive) "Deactivate" else "Activate",
                        color = if (card.cardActive) RubyError else EmeraldSuccess,
                        maxLines = 1
                    )
                }

                if (secondaryActionLabel != null && onSecondaryActionClick != null) {
                    OutlinedButton(
                        onClick = { onSecondaryActionClick(card) },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sheet_secondary_action_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(secondaryActionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialCopyRow(
    label: String,
    value: String,
    mono: Boolean,
    copyTestTag: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = if (mono) {
                    MaterialTheme.typography.titleSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onCopy,
            modifier = Modifier
                .size(48.dp)
                .testTag(copyTestTag)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy $label",
                tint = MetallicGold,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
