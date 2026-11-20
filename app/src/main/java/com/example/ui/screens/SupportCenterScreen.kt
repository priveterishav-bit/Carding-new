package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.SimFormatters
import com.example.data.model.SupportChatMessage
import com.example.data.model.SupportSenderType
import com.example.data.model.SupportTicket
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RubyError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportCenterScreen(
    messages: List<SupportChatMessage>,
    streamingText: String?,
    aiError: String?,
    tickets: List<SupportTicket>,
    onSendAiMessage: (String) -> Unit,
    onRequestHumanNotice: () -> Unit,
    onCreateTicket: (String, String, String) -> Unit,
    onAdvanceTicket: (SupportTicket) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Image(
                painter = painterResource(id = R.drawable.img_support_avatar),
                contentDescription = "Support Concierge Badge",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(34.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Customer Support Center", style = MaterialTheme.typography.titleLarge)
                Text("Automated AI Chat • Tickets • FAQs", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
            }
        }

        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("AI Chat") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Tickets (${tickets.size})") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("FAQs & Help") })
        }

        when (selectedTab) {
            0 -> SupportAiChatTab(
                messages = messages,
                streamingText = streamingText,
                aiError = aiError,
                onSend = onSendAiMessage,
                onEscalate = {
                    onRequestHumanNotice()
                    selectedTab = 1
                }
            )
            1 -> SupportTicketsTab(
                tickets = tickets,
                onCreateTicket = onCreateTicket,
                onAdvanceTicket = onAdvanceTicket
            )
            else -> SupportFaqTab(onOpenTicketForm = { selectedTab = 1 })
        }
    }
}

@Composable
private fun SupportAiChatTab(
    messages: List<SupportChatMessage>,
    streamingText: String?,
    aiError: String?,
    onSend: (String) -> Unit,
    onEscalate: () -> Unit
) {
    var input by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = ElectricCyan.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Automated AI Bot (Separate from Human Agents)", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                }
                OutlinedButton(
                    onClick = onEscalate,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Human Ticket", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(msg)
            }
            if (streamingText != null) {
                item {
                    ChatBubble(
                        SupportChatMessage(
                            id = "streaming",
                            senderType = SupportSenderType.AUTOMATED_AI,
                            text = streamingText.ifEmpty { "Thinking..." }
                        )
                    )
                }
            }
            if (aiError != null) {
                item {
                    Surface(
                        color = RubyError.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(aiError, color = RubyError, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask about cards, QR pay, refunds...") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("support_chat_input")
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.isNotBlank()) {
                        onSend(input)
                        input = ""
                    }
                },
                enabled = streamingText == null,
                modifier = Modifier.size(48.dp).testTag("support_chat_send_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Support Message", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: SupportChatMessage) {
    val isUser = msg.senderType == SupportSenderType.USER
    val isHumanNotice = msg.senderType == SupportSenderType.HUMAN_QUEUE_INFO
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = when {
                isUser -> MaterialTheme.colorScheme.primary
                isHumanNotice -> AmberWarning.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = when (msg.senderType) {
                        SupportSenderType.USER -> "You"
                        SupportSenderType.AUTOMATED_AI -> "AUTOMATED AI SUPPORT"
                        SupportSenderType.HUMAN_QUEUE_INFO -> "HUMAN AGENT QUEUE"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) Color.Black.copy(alpha = 0.7f) else ElectricCyan
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.Black else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SupportTicketsTab(
    tickets: List<SupportTicket>,
    onCreateTicket: (String, String, String) -> Unit,
    onAdvanceTicket: (SupportTicket) -> Unit
) {
    val categories = listOf("Payment Issues", "Failed Virtual Txn", "Card Activation", "Refund Requests", "Order Tracking", "Report a Seller")
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MetallicGold)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Support Ticket (Human Queue)", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        categories.take(3).forEach { cat ->
                            CategoryChip(cat, selectedCategory == cat, { selectedCategory = cat }, Modifier.weight(1f))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        categories.drop(3).forEach { cat ->
                            CategoryChip(cat, selectedCategory == cat, { selectedCategory = cat }, Modifier.weight(1f))
                        }
                    }
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ticket_subject_input")
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Describe the issue or seller report") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("ticket_desc_input")
                    )
                    Button(
                        onClick = {
                            onCreateTicket(subject, selectedCategory, description)
                            subject = ""
                            description = ""
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_ticket_btn")
                    ) {
                        Text("Submit Support Ticket")
                    }
                }
            }
        }

        item {
            Text("My Support Tickets (${tickets.size})", style = MaterialTheme.typography.titleLarge)
        }

        items(tickets, key = { it.id }) { ticket ->
            val statusColor = when (ticket.status) {
                "RESOLVED" -> EmeraldSuccess
                "IN_PROGRESS" -> AmberWarning
                else -> ElectricCyan
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${ticket.ticketCode} • ${ticket.category}", style = MaterialTheme.typography.labelSmall, color = statusColor)
                        Text(ticket.status, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = statusColor)
                    }
                    Text(ticket.subject, style = MaterialTheme.typography.titleMedium)
                    Text(ticket.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Created: ${SimFormatters.formatTimestamp(ticket.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (ticket.status != "RESOLVED") {
                        OutlinedButton(onClick = { onAdvanceTicket(ticket) }) {
                            Text("Simulate Status -> ${if (ticket.status == "OPEN") "IN PROGRESS" else "RESOLVED"}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SupportFaqTab(onOpenTicketForm: () -> Unit) {
    val faqs = listOf(
        "Are these real Visa or Mastercard numbers?" to "No. All card numbers, CVVs, and Account Balances in Vault Card are 100% fictional virtual values for entertainment only.",
        "Why was my virtual payment blocked?" to "Check My Vault Cards to make sure the selected card is Active (not Deactivated) before confirming a payment.",
        "How do QR code payments work?" to "In Scan & Pay, scan any store or UPI/PayPal QR via camera, upload one from Gallery via Photo Picker, or use Direct No-Details Pay.",
        "How does Account Balance increase?" to "Every card starts with a $0.00 Account Balance and increments only after a successful Card Top-Up or payment event.",
        "How do I report a seller or order issue?" to "Switch to the Tickets tab and select 'Report a Seller' or 'Order Tracking' to create a tracked ticket ID."
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(faqs) { (q, a) ->
            var expanded by remember { mutableStateOf(false) }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(q, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                    }
                    AnimatedVisibility(visible = expanded) {
                        Text(a, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
        item {
            Button(onClick = onOpenTicketForm, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("Contact Support / Report a Seller")
            }
        }
    }
}
