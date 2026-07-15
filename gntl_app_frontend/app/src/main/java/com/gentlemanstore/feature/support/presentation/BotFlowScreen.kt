package com.gentlemanstore.feature.support.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gentlemanstore.ui.theme.Gold500

@Composable
fun BotFlowScreen(
    onNavigateBack: () -> Unit,
    onFlowComplete: (ticketId: Long, sessionId: Long) -> Unit,
    viewModel: SupportViewModel = hiltViewModel()
) {
    val uiState by viewModel.botFlowUiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.startBotFlow()
    }

    LaunchedEffect(uiState.flowComplete) {
        if (uiState.flowComplete) {
            val ticket = uiState.ticket
            if (ticket != null && ticket.sessionId != null) {
                onFlowComplete(ticket.id, ticket.sessionId)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "SUPPORT",
                    style = MaterialTheme.typography.titleLarge,
                    color = Gold500
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp))
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Gold500)
                    }
                }

                uiState.isCreatingTicket -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Gold500)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Creating your support request...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                uiState.questions.isNotEmpty() -> {
                    val currentQuestion = uiState.questions.getOrNull(uiState.currentQuestionIndex)
                    val progress = (uiState.currentQuestionIndex + 1).toFloat() / uiState.questions.size

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Progress bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = Gold500,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Question ${uiState.currentQuestionIndex + 1} of ${uiState.questions.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Bot poruka
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 16.dp,
                                            bottomStart = 16.dp,
                                            bottomEnd = 16.dp
                                        )
                                    )
                                    .padding(12.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = currentQuestion?.question ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tip unosa zavisi od redosleda pitanja u bot upitniku:
                        // 1 = slobodan opis, 2 = izbor porudžbine, 3 = hitnost.
                        when (currentQuestion?.orderIndex) {
                            2 -> OrderPickerStep(
                                orders = uiState.myOrders,
                                isLoading = uiState.isLoadingOrders,
                                selectedOrderId = uiState.selectedOrderId,
                                choiceMade = uiState.orderChoiceMade,
                                onOrderChoice = { viewModel.onOrderChoice(it) },
                                modifier = Modifier.weight(1f)
                            )
                            3 -> UrgencyPickerStep(
                                selectedUrgency = uiState.selectedUrgency,
                                onUrgencySelected = { viewModel.onUrgencySelected(it) },
                                modifier = Modifier.weight(1f)
                            )
                            else -> {
                                Spacer(modifier = Modifier.weight(1f))
                                OutlinedTextField(
                                    value = uiState.currentAnswer,
                                    onValueChange = { viewModel.onBotAnswerChange(it) },
                                    placeholder = { Text("Type your answer...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 4
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.submitBotAnswer() },
                            enabled = uiState.currentAnswer.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            val isLast = uiState.currentQuestionIndex >= uiState.questions.size - 1
                            Text(
                                text = if (isLast) "Submit" else "Next",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.background
                            )
                        }
                    }
                }
            }
        }
    }
}

// Korak 2: izbor porudžbine iz sopstvene My Orders liste — bez ručnog unosa
// proizvoljnog order ID-a. "Not related to an order" uvek dostupno.
@Composable
private fun OrderPickerStep(
    orders: List<com.gentlemanstore.feature.order.data.dto.OrderResponse>,
    isLoading: Boolean,
    selectedOrderId: Long?,
    choiceMade: Boolean,
    onOrderChoice: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SelectableOptionCard(
                selected = choiceMade && selectedOrderId == null,
                onClick = { onOrderChoice(null) }
            ) {
                Text(
                    text = "Not related to an order",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Gold500, modifier = Modifier.size(24.dp))
                }
            }
        }

        items(orders.size, key = { orders[it].id }) { index ->
            val order = orders[index]
            SelectableOptionCard(
                selected = selectedOrderId == order.id,
                onClick = { onOrderChoice(order.id) }
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Order #${order.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = order.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = com.gentlemanstore.feature.order.presentation.getStatusColor(order.status)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${order.createdAt.take(10)} · ${(order.finalPrice ?: order.totalPrice)} din",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Korak 3: strukturisana hitnost — samo tri dozvoljene vrednosti.
@Composable
private fun UrgencyPickerStep(
    selectedUrgency: String?,
    onUrgencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        Triple("LOW", "Not urgent", "General question or minor issue"),
        Triple("MEDIUM", "Moderately urgent", "Affects my order or account"),
        Triple("HIGH", "Urgent", "Needs immediate attention")
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label, description) ->
            SelectableOptionCard(
                selected = selectedUrgency == value,
                onClick = { onUrgencySelected(value) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedUrgency == value,
                        onClick = { onUrgencySelected(value) },
                        colors = RadioButtonDefaults.colors(selectedColor = Gold500)
                    )
                    Column {
                        Text(
                            text = "$value — $label",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableOptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) Modifier.border(
                    width = 1.5.dp,
                    color = Gold500,
                    shape = RoundedCornerShape(12.dp)
                ) else Modifier
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Gold500.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}