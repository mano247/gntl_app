package com.gentlemanstore.feature.manager.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gentlemanstore.feature.manager.data.dto.DiscountResponse
import com.gentlemanstore.feature.manager.data.dto.PromotionResponse
import com.gentlemanstore.ui.theme.Gold500
import java.math.BigDecimal

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ManagerHomeScreen(
    onLogout: () -> Unit,
    onShowError: (String) -> Unit = {},
    viewModel: ManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            onShowError(it)
            viewModel.clearError()
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Analytics", "Discounts", "Promotions")

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
            Text(text = "MANAGER PANEL", style = MaterialTheme.typography.titleLarge, color = Gold500)
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
            0 -> AnalyticsTab(analytics = uiState.analytics, onRefresh = { viewModel.loadDashboard() })
            1 -> DiscountsTab(
                discounts = uiState.discounts,
                categories = uiState.categories,
                isCreating = uiState.isCreatingDiscount,
                deletingId = uiState.deletingDiscountId,
                onCreate = { type, value, from, to, scope, categoryId ->
                    viewModel.createDiscount(type, value, from, to, scope, categoryId)
                },
                onDelete = { viewModel.deleteDiscount(it) }
            )
            2 -> PromotionsTab(
                promotions = uiState.promotions,
                isCreating = uiState.isCreatingPromotion,
                deletingId = uiState.deletingPromotionId,
                onCreate = { name, desc, code, type, value, from, to ->
                    viewModel.createPromotion(name, desc, code, type, value, from, to)
                },
                onDelete = { viewModel.deletePromotion(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AnalyticsTab(
    analytics: com.gentlemanstore.feature.manager.data.dto.AnalyticsResponse?,
    onRefresh: () -> Unit
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = analytics == null,
        onRefresh = onRefresh
    )

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        if (analytics == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Gold500)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AnalyticsCard(modifier = Modifier.weight(1f), title = "Total Revenue", value = "${analytics.totalRevenue} din")
                        AnalyticsCard(modifier = Modifier.weight(1f), title = "Total Orders", value = analytics.totalOrders.toString())
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AnalyticsCard(modifier = Modifier.weight(1f), title = "New Users", value = analytics.newUsers.toString())
                        AnalyticsCard(modifier = Modifier.weight(1f), title = "Avg Order", value = "${"%.2f".format(analytics.averageOrderValue)} din")
                    }
                }
                item {
                    Text(text = "TOP PRODUCTS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(analytics.topProducts) { product ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = product.productName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "${product.totalSold} sold", style = MaterialTheme.typography.bodyMedium, color = Gold500, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = analytics == null,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = Gold500
        )
    }
}

@Composable
private fun AnalyticsCard(modifier: Modifier = Modifier, title: String, value: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = Gold500, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DiscountsTab(
    discounts: List<DiscountResponse>,
    categories: List<Pair<Long, String>>,
    isCreating: Boolean,
    deletingId: Long?,
    onCreate: (String, BigDecimal, String, String, String, Long?) -> Unit,
    onDelete: (Long) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    var discountType by remember { mutableStateOf("PERCENTAGE") }
    var value by remember { mutableStateOf("") }
    var validFrom by remember { mutableStateOf("") }
    var validTo by remember { mutableStateOf("") }
    var selectedScope by remember { mutableStateOf("GLOBAL") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }

    val valueError = value.isNotBlank() && (value.toBigDecimalOrNull() == null
            || value.toBigDecimalOrNull()!! <= BigDecimal.ZERO
            || (discountType == "PERCENTAGE" && value.toBigDecimalOrNull()!! > BigDecimal(100)))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(onClick = { showForm = !showForm }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold500), shape = RoundedCornerShape(12.dp)) {
                Text(if (showForm) "Cancel" else "+ Create Discount", color = MaterialTheme.colorScheme.background)
            }
        }

        if (showForm) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box {
                            OutlinedButton(onClick = { showTypeMenu = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                                Text("Type: $discountType", color = Gold500)
                            }
                            DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                                listOf("PERCENTAGE", "FIXED").forEach { type ->
                                    DropdownMenuItem(text = { Text(type) }, onClick = { discountType = type; showTypeMenu = false })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            label = { Text("Value") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = valueError,
                            supportingText = if (valueError) {
                                { Text(if (discountType == "PERCENTAGE") "Enter a percentage between 0 and 100" else "Enter an amount greater than 0") }
                            } else null
                        )
                        OutlinedTextField(value = validFrom, onValueChange = { validFrom = it }, label = { Text("Valid From (2026-01-01T00:00:00)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = validTo, onValueChange = { validTo = it }, label = { Text("Valid To (2026-12-31T23:59:59)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                        Text(text = "Applies to:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("GLOBAL", "CATEGORY").forEach { scope ->
                                FilterChip(
                                    selected = selectedScope == scope,
                                    onClick = { selectedScope = scope; if (scope == "GLOBAL") selectedCategoryId = null },
                                    label = { Text(scope) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gold500, selectedLabelColor = MaterialTheme.colorScheme.background)
                                )
                            }
                        }

                        if (selectedScope == "CATEGORY") {
                            Box {
                                OutlinedButton(onClick = { showCategoryMenu = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                                    Text(text = categories.find { it.first == selectedCategoryId }?.second ?: "Select Category", color = Gold500)
                                }
                                DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                                    categories.forEach { (id, name) ->
                                        DropdownMenuItem(text = { Text(name) }, onClick = { selectedCategoryId = id; showCategoryMenu = false })
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val v = value.toBigDecimalOrNull() ?: return@Button
                                val categoryId = if (selectedScope == "CATEGORY") selectedCategoryId else null
                                onCreate(discountType, v, validFrom, validTo, selectedScope, categoryId)
                                showForm = false
                                value = ""; validFrom = ""; validTo = ""
                                selectedScope = "GLOBAL"; selectedCategoryId = null
                            },
                            enabled = !isCreating && value.isNotBlank() && !valueError
                                    && validFrom.isNotBlank() && validTo.isNotBlank()
                                    && (selectedScope == "GLOBAL" || selectedCategoryId != null),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Text("Create", color = MaterialTheme.colorScheme.background)
                        }
                    }
                }
            }
        }

        items(discounts, key = { it.id }) { discount ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        val valueText = if (discount.discountType == "PERCENTAGE") "${discount.value.stripTrailingZeros().toPlainString()}% OFF"
                        else "${discount.value.stripTrailingZeros().toPlainString()} RSD OFF"
                        Text(text = valueText, style = MaterialTheme.typography.titleSmall, color = Gold500, fontWeight = FontWeight.Bold)
                        val scopeText = when {
                            discount.scope == "CATEGORY" && discount.categoryName != null -> "Category: ${discount.categoryName}"
                            else -> "Global — all products"
                        }
                        Text(text = scopeText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${discount.validFrom.take(10)} → ${discount.validTo.take(10)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDelete(discount.id) }, enabled = deletingId != discount.id) {
                        if (deletingId == discount.id) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                        else Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun PromotionsTab(
    promotions: List<PromotionResponse>,
    isCreating: Boolean,
    deletingId: Long?,
    onCreate: (String, String, String?, String, BigDecimal, String, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var autoGenerateCode by remember { mutableStateOf(true) }
    var discountType by remember { mutableStateOf("PERCENTAGE") }
    var value by remember { mutableStateOf("") }
    var validFrom by remember { mutableStateOf("") }
    var validTo by remember { mutableStateOf("") }
    var showTypeMenu by remember { mutableStateOf(false) }

    val valueError = value.isNotBlank() && (value.toBigDecimalOrNull() == null
            || value.toBigDecimalOrNull()!! <= BigDecimal.ZERO
            || (discountType == "PERCENTAGE" && value.toBigDecimalOrNull()!! > BigDecimal(100)))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(onClick = { showForm = !showForm }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold500), shape = RoundedCornerShape(12.dp)) {
                Text(if (showForm) "Cancel" else "+ Create Promotion", color = MaterialTheme.colorScheme.background)
            }
        }

        if (showForm) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Promotion Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = autoGenerateCode,
                                onCheckedChange = { autoGenerateCode = it; if (it) code = "" },
                                colors = CheckboxDefaults.colors(checkedColor = Gold500)
                            )
                            Text(
                                text = "Auto-generate promo code",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!autoGenerateCode) {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it.uppercase() },
                                label = { Text("Promo Code") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Box {
                            OutlinedButton(onClick = { showTypeMenu = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                                Text("Type: $discountType", color = Gold500)
                            }
                            DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                                listOf("PERCENTAGE", "FIXED").forEach { type ->
                                    DropdownMenuItem(text = { Text(type) }, onClick = { discountType = type; showTypeMenu = false })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            label = { Text("Value") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = valueError,
                            supportingText = if (valueError) {
                                { Text(if (discountType == "PERCENTAGE") "Enter a percentage between 0 and 100" else "Enter an amount greater than 0") }
                            } else null
                        )
                        OutlinedTextField(value = validFrom, onValueChange = { validFrom = it }, label = { Text("Valid From (2026-01-01T00:00:00)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = validTo, onValueChange = { validTo = it }, label = { Text("Valid To (2026-12-31T23:59:59)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                        Button(
                            onClick = {
                                val v = value.toBigDecimalOrNull() ?: return@Button
                                onCreate(name, description, if (autoGenerateCode) null else code, discountType, v, validFrom, validTo)
                                showForm = false
                                name = ""; description = ""; code = ""; value = ""; validFrom = ""; validTo = ""
                                autoGenerateCode = true; discountType = "PERCENTAGE"
                            },
                            enabled = !isCreating && name.isNotBlank() && description.isNotBlank()
                                    && value.isNotBlank() && !valueError
                                    && validFrom.isNotBlank() && validTo.isNotBlank()
                                    && (autoGenerateCode || code.isNotBlank()),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isCreating) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Text("Create", color = MaterialTheme.colorScheme.background)
                        }
                    }
                }
            }
        }

        items(promotions, key = { it.id }) { promotion ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = promotion.name, style = MaterialTheme.typography.titleSmall, color = Gold500, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            val statusColor = if (promotion.active) androidx.compose.ui.graphics.Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            Box(
                                modifier = Modifier
                                    .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (promotion.active) "ACTIVE" else "INACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor
                                )
                            }
                        }
                        Text(text = promotion.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "Code: ${promotion.code}", style = MaterialTheme.typography.bodySmall, color = Gold500, fontWeight = FontWeight.Bold)
                        val valueText = if (promotion.discountType == "PERCENTAGE") "${promotion.value.stripTrailingZeros().toPlainString()}% OFF"
                        else "${promotion.value.stripTrailingZeros().toPlainString()} RSD OFF"
                        Text(text = valueText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${promotion.validFrom.take(10)} → ${promotion.validTo.take(10)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDelete(promotion.id) }, enabled = deletingId != promotion.id) {
                        if (deletingId == promotion.id) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                        else Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

