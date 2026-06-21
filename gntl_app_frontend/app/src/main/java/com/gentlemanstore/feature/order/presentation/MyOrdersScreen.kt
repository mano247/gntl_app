package com.gentlemanstore.feature.order.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
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
import com.gentlemanstore.core.util.CurrencyFormatter
import com.gentlemanstore.core.util.rememberCurrentCurrency
import com.gentlemanstore.feature.order.data.dto.OrderResponse
import com.gentlemanstore.ui.theme.Gold500

fun getStatusColor(status: String): Color {
    return when (status.uppercase()) {
        "PENDING" -> Color(0xFFE5A03C)
        "CONFIRMED" -> Color(0xFF4A90D9)
        "SHIPPED" -> Color(0xFF5BA3D0)
        "DELIVERED" -> Color(0xFF4CAF50)
        "CANCELLED" -> Color(0xFFE05252)
        else -> Color.Gray
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MyOrdersScreen(
    onNavigateBack: () -> Unit,
    onOrderClick: (Long) -> Unit,
    onShowError: (String) -> Unit = {},
    viewModel: MyOrdersViewModel = hiltViewModel()
) {
    val currency = rememberCurrentCurrency()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            onShowError(it)
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = { viewModel.loadOrders() }
    )

    LaunchedEffect(listState.firstVisibleItemIndex, listState.layoutInfo.totalItemsCount) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val total = listState.layoutInfo.totalItemsCount

        if (lastVisible >= total - 3
            && !uiState.isLastPage
            && !uiState.isLoadingMore
            && uiState.orders.isNotEmpty()) {
            viewModel.loadMoreOrders()
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
                    text = "MY ORDERS",
                    style = MaterialTheme.typography.titleLarge,
                    color = Gold500
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp))
            }

            val statuses = listOf("ALL", "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED")

            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(count = statuses.size, key = { statuses[it] }) { index ->
                    val status = statuses[index]
                    val isSelected = when {
                        status == "ALL" && uiState.selectedStatus == null -> true
                        status == uiState.selectedStatus -> true
                        else -> false
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.onStatusFilter(if (status == "ALL") null else status)
                        },
                        label = { Text(status) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold500,
                            selectedLabelColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                when {
                    uiState.isLoading && uiState.orders.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Gold500)
                        }
                    }

                    uiState.orders.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No orders yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(viewModel.filteredOrders, key = { it.id }) { order ->
                                OrderCard(
                                    order = order,
                                    currency = currency,
                                    onClick = { onOrderClick(order.id) }
                                )
                            }

                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = Gold500,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                PullRefreshIndicator(
                    refreshing = uiState.isLoading,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = Gold500
                )
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: OrderResponse,
    currency: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.id}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(getStatusColor(order.status).copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = order.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = getStatusColor(order.status)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (order.loyaltyDiscount != null && order.loyaltyDiscount > java.math.BigDecimal.ZERO) {
                Text(
                    text = CurrencyFormatter.format(order.totalPrice.toDouble(), currency),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                )
                Text(
                    text = CurrencyFormatter.format((order.finalPrice ?: order.totalPrice).toDouble(), currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold500,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = CurrencyFormatter.format(order.totalPrice.toDouble(), currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold500,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}