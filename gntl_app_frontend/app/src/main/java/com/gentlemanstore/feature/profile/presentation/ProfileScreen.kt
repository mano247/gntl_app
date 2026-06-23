package com.gentlemanstore.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gentlemanstore.ui.theme.Gold500

@Composable
fun ProfileScreen(
    onNavigateToOrders: () -> Unit,
    onNavigateToLoyalty: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.logoutComplete) {
        if (uiState.logoutComplete) {
            onLoggedOut()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
        viewModel.loadUnreadCount()
        viewModel.loadUnreadSupportCount()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PROFILE",
                    style = MaterialTheme.typography.titleLarge,
                    color = Gold500
                )
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

                uiState.user != null -> {
                    val user = uiState.user!!

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Gold500, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${user.firstName.firstOrNull() ?: ""}${user.lastName.firstOrNull() ?: ""}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.background,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${user.firstName} ${user.lastName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        ProfileMenuItem(
                            icon = Icons.Default.ShoppingBag,
                            label = "My Orders",
                            onClick = onNavigateToOrders
                        )
                        ProfileMenuItem(
                            icon = Icons.Default.Star,
                            label = "Loyalty Program",
                            onClick = onNavigateToLoyalty
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onNavigateToNotifications() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Gold500,
                                    modifier = Modifier.size(22.dp)
                                )
                                if (uiState.unreadNotificationCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(MaterialTheme.colorScheme.error, CircleShape)
                                            .align(Alignment.TopEnd),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (uiState.unreadNotificationCount > 9) "9+" else uiState.unreadNotificationCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Notifications",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onNavigateToSupport() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Support,
                                    contentDescription = "Support",
                                    tint = Gold500,
                                    modifier = Modifier.size(22.dp)
                                )
                                if (uiState.unreadSupportCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(MaterialTheme.colorScheme.error, CircleShape)
                                            .align(Alignment.TopEnd),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (uiState.unreadSupportCount > 9) "9+" else uiState.unreadSupportCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Support",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        ProfileMenuItem(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            onClick = onNavigateToSettings
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ProfileMenuItem(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            label = "Logout",
                            isDestructive = true,
                            onClick = { viewModel.logout() }
                        )
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
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else Gold500,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}