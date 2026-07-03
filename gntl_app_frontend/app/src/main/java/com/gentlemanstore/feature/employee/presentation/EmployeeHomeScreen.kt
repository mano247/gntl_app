package com.gentlemanstore.feature.employee.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gentlemanstore.feature.order.data.dto.OrderResponse
import com.gentlemanstore.feature.order.presentation.getStatusColor
import com.gentlemanstore.feature.support.data.dto.SupportTicketResponse
import com.gentlemanstore.feature.support.presentation.getTicketStatusColor
import com.gentlemanstore.ui.theme.Gold500

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EmployeeHomeScreen(
    onOpenChat: (Long, Long) -> Unit,
    onLogout: () -> Unit,
    onShowError: (String) -> Unit = {},
    viewModel: EmployeeViewModel = hiltViewModel()
) {
    val ordersState by viewModel.ordersUiState.collectAsStateWithLifecycle()
    val ticketsState by viewModel.ticketsUiState.collectAsStateWithLifecycle()
    val filteredTickets by viewModel.filteredTickets.collectAsStateWithLifecycle()
    val filteredOrders by viewModel.filteredOrders.collectAsStateWithLifecycle()

    LaunchedEffect(ordersState.error) {
        ordersState.error?.let { onShowError(it) }
    }
    LaunchedEffect(ticketsState.error) {
        ticketsState.error?.let { onShowError(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.loadOrders()
        viewModel.loadTickets()
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Orders", "Support Tickets")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(48.dp))
            Text(text = "EMPLOYEE PANEL", style = MaterialTheme.typography.titleLarge, color = Gold500)
            IconButton(onClick = onLogout) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
            }
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface, contentColor = Gold500) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(text = title, color = if (selectedTab == index) Gold500 else MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }
        }

        when (selectedTab) {
            0 -> EmployeeOrdersTab(
                state = ordersState,
                filteredOrders = filteredOrders,
                onUpdateStatus = { orderId, status -> viewModel.updateOrderStatus(orderId, status) },
                onRefresh = { viewModel.loadOrders() },
                onStatusFilter = { viewModel.onOrderStatusFilter(it) },
                onLoadMore = { viewModel.loadMoreOrders() }
            )
            1 -> EmployeeTicketsTab(
                state = ticketsState,
                filteredTickets = filteredTickets,
                onUpdateStatus = { ticketId, status -> viewModel.updateTicketStatus(ticketId, status) },
                onOpenChat = onOpenChat,
                onRefresh = { viewModel.loadTickets() },
                onStatusFilter = { viewModel.onTicketStatusFilter(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun EmployeeOrdersTab(
    state: EmployeeOrdersUiState,
    filteredOrders: List<OrderResponse>,
    onUpdateStatus: (Long, String) -> Unit,
    onRefresh: () -> Unit,
    onStatusFilter: (String?) -> Unit,
    onLoadMore: () -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(refreshing = state.isLoading, onRefresh = onRefresh)
    val statuses = listOf("ALL", "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED")

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(count = statuses.size, key = { statuses[it] }) { index ->
                val status = statuses[index]
                val isSelected = when {
                    status == "ALL" && state.selectedStatus == null -> true
                    status == state.selectedStatus -> true
                    else -> false
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onStatusFilter(if (status == "ALL") null else status) },
                    label = { Text(status) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Gold500,
                        selectedLabelColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
            when {
                state.isLoading && state.orders.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Gold500)
                    }
                }
                filteredOrders.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No orders found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredOrders, key = { it.id }) { order ->
                            EmployeeOrderCard(
                                order = order,
                                isUpdating = state.updatingOrderId == order.id,
                                onUpdateStatus = onUpdateStatus
                            )
                        }
                        if (!state.isLastPage) {
                            item {
                                LaunchedEffect(Unit) { onLoadMore() }
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    if (state.isLoadingMore) {
                                        CircularProgressIndicator(color = Gold500, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            PullRefreshIndicator(refreshing = state.isLoading, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter), backgroundColor = MaterialTheme.colorScheme.surface, contentColor = Gold500)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun EmployeeTicketsTab(
    state: EmployeeTicketsUiState,
    filteredTickets: List<SupportTicketResponse>,
    onUpdateStatus: (Long, String) -> Unit,
    onOpenChat: (Long, Long) -> Unit,
    onRefresh: () -> Unit,
    onStatusFilter: (String?) -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(refreshing = state.isLoading, onRefresh = onRefresh)
    val statuses = listOf("ALL", "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED")

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(count = statuses.size, key = { statuses[it] }) { index ->
                val status = statuses[index]
                val isSelected = when {
                    status == "ALL" && state.selectedStatus == null -> true
                    status == state.selectedStatus -> true
                    else -> false
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onStatusFilter(if (status == "ALL") null else status) },
                    label = { Text(status) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Gold500,
                        selectedLabelColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
            when {
                state.isLoading && state.tickets.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Gold500)
                    }
                }
                filteredTickets.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tickets found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTickets, key = { it.id }) { ticket ->
                            EmployeeTicketCard(
                                ticket = ticket,
                                isUpdating = state.updatingTicketId == ticket.id,
                                onUpdateStatus = onUpdateStatus,
                                onOpenChat = onOpenChat
                            )
                        }
                    }
                }
            }
            PullRefreshIndicator(refreshing = state.isLoading, state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter), backgroundColor = MaterialTheme.colorScheme.surface, contentColor = Gold500)
        }
    }
}

@Composable
private fun EmployeeOrderCard(
    order: OrderResponse,
    isUpdating: Boolean,
    onUpdateStatus: (Long, String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val orderStatuses = listOf("PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Order #${order.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(getStatusColor(order.status).copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(text = order.status, style = MaterialTheme.typography.labelSmall, color = getStatusColor(order.status))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Ordered: ${order.createdAt.take(10)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "${order.items.size} items · ${order.finalPrice ?: order.totalPrice} din", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                Text(text = if (expanded) "Hide details ▲" else "Show details ▼", color = Gold500, style = MaterialTheme.typography.labelMedium)
            }

            if (expanded) {
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.productName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            Text(text = "Size: ${item.size ?: "N/A"} · Qty: ${item.quantity}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(text = "${item.price} din", style = MaterialTheme.typography.bodySmall, color = Gold500, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(4.dp))

                if (order.loyaltyDiscount != null && order.loyaltyDiscount > java.math.BigDecimal.ZERO) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Loyalty Discount", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                        Text(text = "- ${order.loyaltyDiscount} din", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    }
                }

                if (order.promoDiscount != null && order.promoDiscount > java.math.BigDecimal.ZERO) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Promo Discount", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                        Text(text = "- ${order.promoDiscount} din", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Total", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "${order.finalPrice ?: order.totalPrice} din", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Gold500)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box {
                OutlinedButton(onClick = { showMenu = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), enabled = !isUpdating) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Gold500)
                    } else {
                        Text("Change Status", color = Gold500)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Gold500)
                    }
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    orderStatuses.forEach { status ->
                        DropdownMenuItem(text = { Text(status) }, onClick = { onUpdateStatus(order.id, status); showMenu = false })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeTicketCard(
    ticket: SupportTicketResponse,
    isUpdating: Boolean,
    onUpdateStatus: (Long, String) -> Unit,
    onOpenChat: (Long, Long) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val ticketStatuses = listOf("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED")

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Ticket #${ticket.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ticket.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(MaterialTheme.colorScheme.error, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (ticket.unreadCount > 9) "9+" else ticket.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(getTicketStatusColor(ticket.status).copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = ticket.status, style = MaterialTheme.typography.labelSmall, color = getTicketStatusColor(ticket.status))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = ticket.subject, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = ticket.userEmail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { showMenu = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), enabled = !isUpdating) {
                        if (isUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Gold500)
                        } else {
                            Text("Status", color = Gold500)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Gold500)
                        }
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        ticketStatuses.forEach { status ->
                            DropdownMenuItem(text = { Text(status) }, onClick = { onUpdateStatus(ticket.id, status); showMenu = false })
                        }
                    }
                }
                Button(
                    onClick = { ticket.sessionId?.let { sessionId -> onOpenChat(ticket.id, sessionId) } },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat", color = MaterialTheme.colorScheme.background)
                }
            }
        }
    }
}