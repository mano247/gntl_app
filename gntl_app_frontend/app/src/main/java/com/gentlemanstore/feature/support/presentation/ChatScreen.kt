package com.gentlemanstore.feature.support.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gentlemanstore.feature.support.data.dto.ChatMessageResponse
import com.gentlemanstore.ui.theme.Gold500
import kotlinx.coroutines.delay

@Composable
fun ChatScreen(
    ticketId: Long,
    sessionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: SupportViewModel = hiltViewModel()
) {
    val uiState by viewModel.chatUiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) {
        viewModel.loadMessages(sessionId)
        viewModel.loadTicketInfo(ticketId)
        viewModel.connectWebSocket(sessionId)
        delay(1500)
        viewModel.markMessagesAsRead(sessionId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnectWebSocket()
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    viewModel.disconnectWebSocket()
                    onNavigateBack()
                }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "CHAT #$ticketId", style = MaterialTheme.typography.titleLarge, color = Gold500)
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Info header za staff: subject, customer, urgency i povezana
            // porudžbina. Customer vidi svoj postojeći minimalni prikaz.
            val isStaff = !(currentRole ?: "CUSTOMER").contains("CUSTOMER")
            if (isStaff) {
                uiState.ticketInfo?.let { info ->
                    TicketInfoHeader(info = info)
                }
            }

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Gold500)
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            ChatMessageBubble(message = message, currentRole = currentRole ?: "CUSTOMER")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.currentMessage,
                            onValueChange = { viewModel.onMessageChange(it) },
                            placeholder = { Text("Type a message...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.sendMessage(sessionId) },
                            enabled = uiState.currentMessage.isNotBlank() && !uiState.isSending,
                            modifier = Modifier.size(48.dp).background(
                                color = if (uiState.currentMessage.isNotBlank()) Gold500 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(50)
                            )
                        ) {
                            if (uiState.isSending) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Info kartica na vrhu staff chata — bez uticaja na poruke/WebSocket logiku.
@Composable
private fun TicketInfoHeader(info: com.gentlemanstore.feature.support.data.dto.SupportTicketResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = info.subject,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            val customerLine = listOfNotNull(info.customerName, info.userEmail).joinToString(" · ")
            if (customerLine.isNotBlank()) {
                Text(
                    text = customerLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                info.urgency?.let { urgency ->
                    val urgencyColor = getUrgencyColor(urgency)
                    Box(
                        modifier = Modifier
                            .background(urgencyColor.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = urgency,
                            style = MaterialTheme.typography.labelSmall,
                            color = urgencyColor,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
                if (info.orderId != null) {
                    Box(
                        modifier = Modifier
                            .background(Gold500.copy(alpha = 0.15f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Order #${info.orderId}" +
                                    (info.orderStatus?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold500
                        )
                    }
                } else {
                    Text(
                        text = "No linked order",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessageResponse, currentRole: String) {
    val isRightSide = when (message.sender.uppercase()) {
        "USER" -> currentRole.contains("CUSTOMER")
        "EMPLOYEE" -> !currentRole.contains("CUSTOMER")
        else -> false
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isRightSide) Arrangement.End else Arrangement.Start) {
        if (!isRightSide) {
            Box(modifier = Modifier.size(32.dp).background(color = Gold500.copy(alpha = 0.2f), shape = RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                Text(text = if (message.sender.uppercase() == "BOT") "🤖" else "👤", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isRightSide) Alignment.End else Alignment.Start) {
            if (!isRightSide) {
                Text(text = message.sender.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(2.dp))
            }
            Box(
                modifier = Modifier.background(
                    color = if (isRightSide) Gold500 else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = if (isRightSide) 16.dp else 4.dp, topEnd = if (isRightSide) 4.dp else 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                ).padding(12.dp).widthIn(max = 260.dp)
            ) {
                Text(text = message.content, style = MaterialTheme.typography.bodyMedium, color = if (isRightSide) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}