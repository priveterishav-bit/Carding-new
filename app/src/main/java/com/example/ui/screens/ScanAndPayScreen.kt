package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.ParsedSimQr
import com.example.core.PaymentRail
import com.example.core.SimFormatters
import com.example.core.SimQrCodec
import com.example.data.model.SellerProfile
import com.example.data.model.UserProfile
import com.example.data.model.VirtualCard
import com.example.ui.components.DemoVerifiedBadge
import com.example.ui.components.PinConfirmDialog
import com.example.ui.components.QrCodeImage
import com.example.ui.components.SimDisclaimerBanner
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RubyError
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.Locale
import java.util.concurrent.Executors

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanAndPayScreen(
    profile: UserProfile,
    cards: List<VirtualCard>,
    sellers: List<SellerProfile>,
    autoStartCameraTrigger: Int = 0,
    onExecuteDirectPayment: (VirtualCard, Double, String, SellerProfile?, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableIntStateOf(0) } // 0 = Scan & Pay, 1 = My Demo QR
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraEnabled by remember { mutableStateOf(false) }
    var detectedQr by remember { mutableStateOf<ParsedSimQr?>(null) }
    var directNoDetailMode by remember { mutableStateOf(true) }
    var selectedRail by remember { mutableStateOf(PaymentRail.ALL_UPI) }
    var scanFeedback by remember { mutableStateOf<String?>(null) }
    var amountText by remember { mutableStateOf("100.00") }
    var selectedCardId by remember(cards) { mutableStateOf(cards.firstOrNull { it.cardActive }?.id ?: cards.firstOrNull()?.id) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pendingIdempotencyKey by remember { mutableStateOf("") }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            cameraEnabled = true
            scanFeedback = null
        } else {
            scanFeedback = "Camera permission denied. You can still upload a QR from Gallery or pay directly below."
        }
    }

    LaunchedEffect(autoStartCameraTrigger) {
        if (autoStartCameraTrigger > 0) {
            activeSubTab = 0
            if (hasCameraPermission) {
                cameraEnabled = true
            } else {
                cameraPermLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // Zero-permission Android Photo Picker for QR image upload
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
                BarcodeScanning.getClient(options).process(image)
                    .addOnSuccessListener { barcodes ->
                        val raw = barcodes.firstOrNull()?.rawValue
                        if (raw == null) {
                            scanFeedback = "No QR code found in image — you can still use Direct Pay below without details."
                        } else {
                            val parsed = SimQrCodec.parseUniversal(raw)
                            if (parsed != null) {
                                detectedQr = parsed
                                directNoDetailMode = parsed.isDirectNoDetails
                                selectedRail = parsed.suggestedRail
                                parsed.suggestedAmount?.let { amountText = it.toString() }
                                scanFeedback = null
                            }
                        }
                    }
                    .addOnFailureListener {
                        scanFeedback = "Could not analyze selected image for QR codes."
                    }
            } catch (_: Exception) {
                scanFeedback = "Failed to load selected image."
            }
        }
    }

    val matchedSeller = remember(detectedQr, sellers) {
        val qr = detectedQr ?: return@remember null
        if (qr.sellerId.isBlank()) return@remember null
        sellers.firstOrNull { it.id == qr.sellerId }
    }

    val selectedCard = cards.firstOrNull { it.id == selectedCardId } ?: cards.firstOrNull()

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = activeSubTab) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("Scan & Direct Pay") },
                modifier = Modifier.testTag("tab_scan_direct_pay")
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("My Demo QR") },
                modifier = Modifier.testTag("tab_my_demo_qr")
            )
        }

        if (activeSubTab == 1) {
            MyDemoReceiveQrView(profile = profile)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("Scan & Pay (All UPI & Global Sim)", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Direct scan & pay without needing person or bank details. Choose Paytm, PhonePe, GPay, BHIM, PayPal, or Dollar Pay.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    SimDisclaimerBanner("SIMULATOR SCANNER • NO REAL BANK OR UPI NETWORK CONNECTED")
                }

                // CameraX QR Scanner Card
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF050811)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cameraEnabled && hasCameraPermission) {
                                    CameraQrPreview(
                                        onQrDetected = { raw ->
                                            val parsed = SimQrCodec.parseUniversal(raw)
                                            if (parsed != null) {
                                                detectedQr = parsed
                                                directNoDetailMode = parsed.isDirectNoDetails
                                                selectedRail = parsed.suggestedRail
                                                parsed.suggestedAmount?.let { amountText = it.toString() }
                                                cameraEnabled = false
                                                scanFeedback = null
                                            }
                                        }
                                    )
                                } else {
                                    ScannerIdleOverlay(
                                        onStartScan = {
                                            if (hasCameraPermission) cameraEnabled = true
                                            else cameraPermLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (hasCameraPermission) {
                                            cameraEnabled = !cameraEnabled
                                        } else {
                                            cameraPermLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElectricCyan,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("toggle_camera_scan_btn")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (cameraEnabled) "Stop Camera" else "Scan QR")
                                }
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("upload_qr_gallery_btn")
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Gallery QR")
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    cameraEnabled = false
                                    detectedQr = ParsedSimQr(
                                        sellerId = "",
                                        storeName = "Direct QR Pay (No Details)",
                                        suggestedAmount = null,
                                        rawPayload = "DIRECT_QR_NO_DETAILS",
                                        suggestedRail = selectedRail,
                                        isDirectNoDetails = true
                                    )
                                    directNoDetailMode = true
                                    scanFeedback = null
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("instant_direct_qr_btn")
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Instant Direct Pay (No Person Details Needed)", color = MetallicGold)
                            }
                        }
                    }
                }

                if (scanFeedback != null) {
                    item {
                        Surface(
                            color = RubyError.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = scanFeedback!!,
                                color = RubyError,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // Direct Scan & Pay Checkout Panel (Always ready — zero personal details required)
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.2.dp, MetallicGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (matchedSeller != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(matchedSeller.storeName, style = MaterialTheme.typography.titleLarge)
                                        Text("Seller ID: ${matchedSeller.sellerCode}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    DemoVerifiedBadge()
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", matchedSeller.averageRating)} (${matchedSeller.reviewCount} reviews)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (detectedQr != null) "QR Ready • Direct Pay" else "Direct Scan & Pay",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = "No name, phone, or UPI ID details required",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldSuccess
                                        )
                                    }
                                    Surface(
                                        color = EmeraldSuccess.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "DIRECT QR",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldSuccess,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Payment System Selector (Paytm, PhonePe, GPay, BHIM, PayPal, Dollar Pay, All UPI)
                            Text("Choose Payment System (Simulated):", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(PaymentRail.entries, key = { it.name }) { rail ->
                                    val isSelected = selectedRail == rail
                                    val railColor = remember(rail.accentHex) {
                                        Color(android.graphics.Color.parseColor(rail.accentHex))
                                    }
                                    Surface(
                                        onClick = { selectedRail = rail },
                                        color = if (isSelected) railColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) railColor else MaterialTheme.colorScheme.outline
                                        ),
                                        modifier = Modifier.testTag("rail_${rail.name}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = rail.label,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSelected) railColor else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = rail.badge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Quick Amount Chips + Amount Input
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("25", "100", "500", "1000").forEach { preset ->
                                    OutlinedButton(
                                        onClick = { amountText = preset },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
                                    ) {
                                        Text("$$preset", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                label = { Text("Payment Amount (SIM USD)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("qr_amount_input")
                            )

                            Text("Select Virtual Card:", style = MaterialTheme.typography.labelLarge)
                            cards.forEach { card ->
                                val isSelected = card.id == selectedCard?.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedCardId = card.id }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (card.cardActive) card.cardTitle else "${card.cardTitle} (DEACTIVATED)",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (card.cardActive) MaterialTheme.colorScheme.onSurface else RubyError
                                        )
                                        Text("•••• ${card.cardNumber.takeLast(4)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(
                                        text = SimFormatters.formatSimUsd(card.balance, card.unlimitedBalance || profile.unlimitedMode),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (card.cardActive) EmeraldSuccess else RubyError
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val card = selectedCard ?: return@Button
                                    val amt = amountText.toDoubleOrNull() ?: 0.0
                                    if (!card.cardActive) {
                                        scanFeedback = "CARD DEACTIVATED: Reactivate ${card.cardTitle} in My Cards first."
                                        return@Button
                                    }
                                    if (amt <= 0.0) {
                                        scanFeedback = "Enter a valid simulated payment amount."
                                        return@Button
                                    }
                                    pendingIdempotencyKey = "qr_${selectedRail.name.lowercase()}_${System.currentTimeMillis()}"
                                    if (profile.pinEnabled) {
                                        showPinDialog = true
                                    } else {
                                        val label = matchedSeller?.storeName ?: "${selectedRail.label} Direct QR (Sim)"
                                        onExecuteDirectPayment(card, amt, label, matchedSeller, pendingIdempotencyKey)
                                    }
                                },
                                enabled = selectedCard != null && selectedCard.cardActive,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldSuccess,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("qr_confirm_pay_btn")
                            ) {
                                Text(
                                    text = "Pay via ${selectedRail.label} (Simulated)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // Optional Verified Demo Store Presets
                item {
                    Text("Optional Verified Demo Store QRs:", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(sellers, key = { it.id }) { seller ->
                            val isPicked = matchedSeller?.id == seller.id
                            Surface(
                                onClick = {
                                    detectedQr = SimQrCodec.parse(seller.qrPayload)
                                    directNoDetailMode = false
                                    scanFeedback = null
                                },
                                color = if (isPicked) ElectricCyan.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isPicked) ElectricCyan else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.testTag("qr_preset_${seller.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(seller.storeName, style = MaterialTheme.typography.labelLarge)
                                        Text(seller.sellerCode, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog && selectedCard != null) {
        PinConfirmDialog(
            expectedPin = profile.simPin,
            onDismiss = { showPinDialog = false },
            onConfirmed = {
                showPinDialog = false
                val amt = amountText.toDoubleOrNull() ?: 0.0
                if (amt > 0.0) {
                    val label = matchedSeller?.storeName ?: "${selectedRail.label} Direct QR (Sim)"
                    onExecuteDirectPayment(selectedCard, amt, label, matchedSeller, pendingIdempotencyKey)
                }
            }
        )
    }
}

@Composable
private fun MyDemoReceiveQrView(profile: UserProfile) {
    val qrPayload = remember(profile.customerId, profile.displayName) {
        SimQrCodec.encode("direct_${profile.customerId}", profile.displayName)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SimDisclaimerBanner("DEMO RECEIVE QR • NO REAL BANK ACCOUNT LINKED")
        }
        item {
            Surface(
                color = Color(0xFF14161B),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MetallicGold.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }

                    Box(contentAlignment = Alignment.Center) {
                        QrCodeImage(payload = qrPayload, sizeDp = 240)
                        Surface(
                            color = Color.White,
                            shape = CircleShape,
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "Vault Card QR Emblem",
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Scan to pay with any simulated UPI or Dollar Pay app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Vault Card Virtual Wallet • ${profile.customerId}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerIdleOverlay(onStartScan: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "scan_alpha"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .clickable { onStartScan() }
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .border(2.dp, ElectricCyan.copy(alpha = alpha), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Tap to open CameraX QR Scanner",
                tint = ElectricCyan,
                modifier = Modifier.size(60.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Tap 'Scan QR' to open CameraX Scanner",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraQrPreview(
    onQrDetected: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            barcodeScanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            barcodeScanner.process(input)
                                .addOnSuccessListener { codes ->
                                    codes.firstOrNull()?.rawValue?.let(onQrDetected)
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
