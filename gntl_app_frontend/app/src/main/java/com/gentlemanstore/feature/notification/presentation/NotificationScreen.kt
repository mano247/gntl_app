package com.gentlemanstore.feature.notification.presentation

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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gentlemanstore.feature.notification.data.dto.NotificationResponse
import com.gentlemanstore.ui.theme.Gold500

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onShowError: (String) -> Unit = {},
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.error) {
        uiState.error?.let { onShowError(it) }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = { viewModel.loadNotifications() }
    )

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
                Spacer(modifier = Modifier.width(48.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                when {
                    uiState.isLoading && uiState.notifications.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Gold500)
                        }
                    }

                    uiState.notifications.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
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
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(uiState.notifications, key = { it.id }) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    onClick = {
                                        if (!notification.read) {
                                            viewModel.markAsRead(notification.id)
                                        }
                                    }
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
private fun NotificationCard(
    notification: NotificationResponse,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (notification.read) MaterialTheme.colorScheme.surface
                else Gold500.copy(alpha = 0.08f)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (!notification.read) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, end = 10.dp)
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Gold500)
            )
        } else {
            Spacer(modifier = Modifier.width(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}