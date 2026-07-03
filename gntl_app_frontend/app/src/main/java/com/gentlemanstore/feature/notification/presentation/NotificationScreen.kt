package com.gentlemanstore.feature.notification.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
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
import com.gentlemanstore.feature.notification.data.dto.NotificationResponse
import com.gentlemanstore.ui.theme.Gold500

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredNotifications by viewModel.filteredNotifications.collectAsStateWithLifecycle()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = {
            viewModel.loadNotifications()
        }
    )

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.markAllAsRead()
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
                    text = "NOTIFICATIONS",
                    style = MaterialTheme.typography.titleLarge,
                    color = Gold500
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { viewModel.deleteAllNotifications() },
                    enabled = uiState.notifications.isNotEmpty()
                ) {
                    Text(
                        text = "Clear All",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.notifications.isNotEmpty()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val types = listOf("ALL", "DISCOUNT", "ORDER_STATUS", "LOYALTY")

            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(count = types.size, key = { types[it] }) { index ->
                    val type = types[index]
                    val isSelected = when {
                        type == "ALL" && uiState.selectedType == null -> true
                        type == uiState.selectedType -> true
                        else -> false
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onTypeFilter(if (type == "ALL") null else type) },
                        label = { Text(type) },
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
                    uiState.isLoading && uiState.notifications.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Gold500)
                        }
                    }

                    uiState.notifications.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No notifications yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredNotifications, key = { it.id }) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    onClick = { viewModel.markAsRead(notification.id) }
                                )
                            }

                            if (!uiState.isLastPage) {
                                item {
                                    LaunchedEffect(Unit) { viewModel.loadMoreNotifications() }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.isLoadingMore) {
                                            CircularProgressIndicator(
                                                color = Gold500,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
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
private fun NotificationCard(
    notification: NotificationResponse,
    onClick: () -> Unit
) {
    val typeColor = when (notification.type) {
        "DISCOUNT" -> Color(0xFF4A90D9)
        "ORDER_STATUS" -> Color(0xFF4CAF50)
        "LOYALTY" -> Gold500
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val typeLabel = when (notification.type) {
        "DISCOUNT" -> "🏷 Discount"
        "ORDER_STATUS" -> "📦 Order"
        "LOYALTY" -> "⭐ Loyalty"
        else -> notification.type
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (!notification.isRead)
                    Color(0xFF1A2744)
                else
                    MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Gold500, shape = RoundedCornerShape(50))
                    .align(Alignment.CenterVertically)
            )
            // 6dp tačka + 12dp razmak = 18dp, isto kao spacer pročitanih —
            // naslovi ostaju poravnati između kartica.
            Spacer(modifier = Modifier.width(12.dp))
        } else {
            Spacer(modifier = Modifier.width(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (!notification.isRead) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(typeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.createdAt.take(10),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}