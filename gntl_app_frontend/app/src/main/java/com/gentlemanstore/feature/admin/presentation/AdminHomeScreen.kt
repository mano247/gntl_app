package com.gentlemanstore.feature.admin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import com.gentlemanstore.feature.admin.data.dto.UserListResponse
import com.gentlemanstore.ui.theme.Gold500

fun getRoleColor(role: String?): Color {
    return when (role?.uppercase()) {
        "ADMIN" -> Color(0xFFE05252)
        "MANAGER" -> Color(0xFF9C27B0)
        "EMPLOYEE" -> Color(0xFF4A90D9)
        "CUSTOMER" -> Color(0xFF4CAF50)
        else -> Color.Gray
    }
}

@Composable
fun AdminHomeScreen(
    onLogout: () -> Unit,
    onShowError: (String) -> Unit = {},
    onShowSuccess: (String) -> Unit = {},
    viewModel: AdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            onShowError(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            onShowSuccess(it)
            viewModel.clearSuccess()
        }
    }

    val filteredUsers = remember(uiState.users, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) uiState.users
        else uiState.users.filter {
            it.firstName.contains(uiState.searchQuery, ignoreCase = true) ||
                    it.lastName.contains(uiState.searchQuery, ignoreCase = true) ||
                    it.email.contains(uiState.searchQuery, ignoreCase = true)
        }
    }

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
            Text(text = "ADMIN PANEL", style = MaterialTheme.typography.titleLarge, color = Gold500)
            IconButton(onClick = onLogout) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
            }
        }

        Text(
            text = "USER MANAGEMENT",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            placeholder = { Text("Search by name or email...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Gold500) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "ACTIVE", "DEACTIVATED").forEach { filter ->
                FilterChip(
                    selected = uiState.selectedFilter == filter,
                    onClick = { viewModel.onFilterChange(filter) },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Gold500,
                        selectedLabelColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Gold500)
                }
            }
            filteredUsers.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No users found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredUsers, key = { it.id }) { user ->
                        AdminUserCard(
                            user = user,
                            isUpdating = uiState.updatingUserId == user.id,
                            isDeleting = uiState.deletingUserId == user.id,
                            isReactivating = uiState.reactivatingUserId == user.id,
                            onChangeRole = { role -> viewModel.changeUserRole(user.id, role) },
                            onDelete = { viewModel.deleteUser(user.id) },
                            onReactivate = { viewModel.reactivateUser(user.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUserCard(
    user: UserListResponse,
    isUpdating: Boolean,
    isDeleting: Boolean,
    isReactivating: Boolean,
    onChangeRole: (String) -> Unit,
    onDelete: () -> Unit,
    onReactivate: () -> Unit
) {
    var showRoleMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val roles = listOf("CUSTOMER", "EMPLOYEE", "MANAGER", "ADMIN")
    val isDeactivated = user.deleted == true

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete User") },
            text = { Text("Are you sure you want to delete ${user.firstName} ${user.lastName}?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDeactivated)
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDeactivated)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        user.role?.let { role ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(getRoleColor(role).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = role,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = getRoleColor(role),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (isDeactivated) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DEACTIVATED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (isDeactivated) {
                    IconButton(onClick = onReactivate, enabled = !isReactivating) {
                        if (isReactivating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF4CAF50))
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reactivate", tint = Color(0xFF4CAF50))
                        }
                    }
                } else {
                    IconButton(onClick = { showDeleteDialog = true }, enabled = !isDeleting) {
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                        } else {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete user", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (!isDeactivated) {
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedButton(
                        onClick = { showRoleMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isUpdating
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Gold500)
                        } else {
                            Text("Change Role", color = Gold500)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Gold500)
                        }
                    }
                    DropdownMenu(expanded = showRoleMenu, onDismissRequest = { showRoleMenu = false }) {
                        roles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role) },
                                onClick = { onChangeRole(role); showRoleMenu = false }
                            )
                        }
                    }
                }
            }
        }
    }
}