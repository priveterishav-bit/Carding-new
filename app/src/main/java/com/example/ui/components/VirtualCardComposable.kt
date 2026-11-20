package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.SimFormatters
import com.example.data.model.VirtualCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RubyError

import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InteractiveVirtualCard(
    card: VirtualCard,
    globalUnlimitedMode: Boolean = false,
    modifier: Modifier = Modifier,
    externalFlipSignal: Int = 0,
    onCardClick: (() -> Unit)? = null,
    onReactivateClick: (() -> Unit)? = null
) {
    var flipped by remember(card.id) { mutableStateOf(false) }
    LaunchedEffect(externalFlipSignal) {
        if (externalFlipSignal > 0) {
            flipped = !flipped
        }
    }
    val rotationY by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 280f),
        label = "card_flip"
    )
    val density = LocalDensity.current.density

    val infiniteTransition = rememberInfiniteTransition(label = "card_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    val lockPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lock_pulse"
    )

    val (bgStart, bgEnd, accentColor) = remember(card.tierKey, card.accentColorHex) {
        val parsedAccent = try {
            Color(android.graphics.Color.parseColor(card.accentColorHex))
        } catch (_: Exception) {
            MetallicGold
        }
        when (card.tierKey) {
            "STARTER", "VISA_STYLE" -> Triple(Color(0xFF0A1931), Color(0xFF1E3A8A), parsedAccent)
            "MC_STYLE" -> Triple(Color(0xFF1F1124), Color(0xFF7C2D12), parsedAccent)
            "SILVER" -> Triple(Color(0xFF1E293B), Color(0xFF475569), Color(0xFFE2E8F0))
            "GOLD" -> Triple(Color(0xFF271C06), Color(0xFF78510B), MetallicGold)
            "PLATINUM" -> Triple(Color(0xFF171E2E), Color(0xFF334155), Color(0xFFF1F5F9))
            "DIAMOND" -> Triple(Color(0xFF082032), Color(0xFF0E7490), Color(0xFF67E8F9))
            "BLACK_ELITE" -> Triple(Color(0xFF05070D), Color(0xFF18181B), MetallicGold)
            "INFINITY" -> Triple(Color(0xFF1A0B2E), Color(0xFF311042), Color(0xFFC084FC))
            else -> Triple(Color(0xFF0F172A), Color(0xFF1E293B), parsedAccent)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .graphicsLayer {
                this.rotationY = rotationY
                this.cameraDistance = 14f * density
            }
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                onClick = { flipped = !flipped },
                onLongClick = { flipped = !flipped }
            )
            .testTag("virtual_card_${card.id}")
    ) {
        if (rotationY <= 90f) {
            // Card Front
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(bgStart, bgEnd)))
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                accentColor.copy(alpha = 0.7f),
                                Color.White.copy(alpha = 0.15f),
                                accentColor.copy(alpha = 0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                // Optional animated metallic sheen
                if (card.animatedStyle && card.cardActive) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val xStart = size.width * shimmerOffset
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    accentColor.copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                start = Offset(xStart - 180f, 0f),
                                end = Offset(xStart + 180f, size.height)
                            )
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.cardTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = card.cardNetworkLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VaultNetworkLogo(
                                tierKey = card.tierKey,
                                accentColor = accentColor,
                                compact = true
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = Color.Black.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "VAULT • LV${card.upgradeLevel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Metallic EMV-style decorative chip
                            Box(
                                modifier = Modifier
                                    .size(width = 40.dp, height = 28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFDE68A), Color(0xFFD97706))
                                        )
                                    )
                                    .border(0.8.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Contactless,
                                contentDescription = "Simulated contactless icon",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Account Balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                            Text(
                                text = SimFormatters.formatSimUsd(
                                    card.balance,
                                    unlimited = card.unlimitedBalance || globalUnlimitedMode
                                ),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }
                    }

                    Column {
                        Text(
                            text = card.cardNumber,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = card.cardholderName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "TAP OR LONG-PRESS TO FLIP • VIRTUAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                            }
                            if (onCardClick != null) {
                                Surface(
                                    onClick = onCardClick,
                                    color = Color.Black.copy(alpha = 0.42f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                        .testTag("card_details_pill_${card.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "View card details",
                                            tint = accentColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Details",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "EXPIRES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                                Text(
                                    text = card.expiryDisplay,
                                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Deactivated Lock Overlay
                if (!card.cardActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A0D16).copy(alpha = 0.86f))
                            .border(1.5.dp, RubyError.copy(alpha = 0.8f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                color = RubyError.copy(alpha = 0.2f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, RubyError),
                                modifier = Modifier
                                    .size(54.dp)
                                    .graphicsLayer {
                                        scaleX = lockPulse
                                        scaleY = lockPulse
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Card Deactivated",
                                        tint = RubyError,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "CARD DEACTIVATED",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = RubyError
                            )
                            Text(
                                text = "Virtual purchases disabled • Tap reactivate",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                            if (onReactivateClick != null) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    onClick = onReactivateClick,
                                    color = accentColor,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = "Reactivate Card",
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Reactivate Card",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Card Back (rotated 180° inside graphicsLayer so text reads forwards)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.rotationY = 180f }
                    .background(Brush.linearGradient(listOf(bgEnd, bgStart)))
                    .border(1.2.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .background(Color.Black.copy(alpha = 0.85f))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "VIRTUAL CVV",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = card.cvvDisplay,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "EXPIRY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = card.expiryDisplay,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                        VaultNetworkLogo(
                            tierKey = card.tierKey,
                            accentColor = accentColor
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "VIRTUAL VAULT CARD • NO REAL MONEY",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor
                            )
                            Text(
                                text = "Not for real payments. Tap or long-press to flip front.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                        if (onCardClick != null) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                onClick = onCardClick,
                                color = Color.Black.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = "Details",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
