package com.gentlemanstore.feature.manager.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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

@Composable
fun ManagerHomeScreen(
    viewModel: ManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Analytics", "Discounts", "Promotions")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "MANAGER PANEL",
                style = MaterialTheme.typography.titleLarge,
                color = Gold500
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Gold500
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) Gold500
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> AnalyticsTab(uiState.analytics)
            1 -> DiscountsTab(
                discounts = uiState.discounts,
                categories = uiState.categories,
                isCreating = uiState.isCreatingDiscount,
                deletingId = uiState.deletingDiscountId,
                onCreate = { code, type, value, from, to, productId, categoryId ->
                    viewModel.createDiscount(code, type, value, from, to, productId, categoryId)
                },
                onDelete = { viewModel.deleteDiscount(it) }
            )
            2 -> PromotionsTab(
                promotions = uiState.promotions,
                discounts = uiState.discounts,
                isCreating = uiState.isCreatingPromotion,
                onCreate = { name, desc, from, to, discountId ->
                    viewModel.createPromotion(name, desc, from, to, discountId)
                }
            )
        }
    }
}

@Composable
private fun AnalyticsTab(analytics: com.gentlemanstore.feature.manager.data.dto.AnalyticsResponse?) {
    if (analytics == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Gold500)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsCard(modifier = Modifier.weight(1f), title = "Total Revenue", value = "${analytics.totalRevenue} din")
                AnalyticsCard(modifier = Modifier.weight(1f), title = "Total Orders", value = analytics.totalOrders.toString())
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsCard(modifier = Modifier.weight(1f), title = "New Users", value = analytics.newUsers.toString())
                AnalyticsCard(modifier = Modifier.weight(1f), title = "Avg Order", value = "${analytics.averageOrderValue} din")
            }
        }

        item {
            Text(
                text = "TOP PRODUCTS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(analytics.topProducts) { product ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = product.productName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "${product.totalSold} sold", style = MaterialTheme.typography.bodyMedium, color = Gold500, fontWeight = FontWeight.Bold)
                }
            }
        }
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
    onCreate: (String, String, BigDecimal, String, String, Long?, Long?) -> Unit,
    onDelete: (Long) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    var discountType by remember { mutableStateOf("PERCENTAGE") }
    var value by remember { mutableStateOf("") }
    var validFrom by remember { mutableStateOf("") }
    var validTo by remember { mutableStateOf("") }

    // Scope: GLOBAL, CATEGORY
    var selectedScope by remember { mutableStateOf("GLOBAL") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = { showForm = !showForm },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (showForm) "Cancel" else "+ Create Discount", color = MaterialTheme.colorScheme.background)
            }
        }

        if (showForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Code") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Discount type dropdown
                        Box {
                            OutlinedButton(
                                onClick = { showTypeMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Type: $discountType", color = Gold500)
                            }
                            DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                                listOf("PERCENTAGE", "FIXED").forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = { discountType = type; showTypeMenu = false }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            label = { Text("Value") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = validFrom,
                            onValueChange = { validFrom = it },
                            label = { Text("Valid From (2026-01-01T00:00:00)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = validTo,
                            onValueChange = { validTo = it },
                            label = { Text("Valid To (2026-12-31T23:59:59)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Scope selection
                        Text(
                            text = "Applies to:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("GLOBAL", "CATEGORY").forEach { scope ->
                                FilterChip(
                                    selected = selectedScope == scope,
                                    onClick = {
                                        selectedScope = scope
                                        if (scope == "GLOBAL") selectedCategoryId = null
                                    },
                                    label = { Text(scope) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Gold500,
                                        selectedLabelColor = MaterialTheme.colorScheme.background
                                    )
                                )
                            }
                        }

                        // Category dropdown — samo ako je CATEGORY scope
                        if (selectedScope == "CATEGORY") {
                            Box {
                                OutlinedButton(
                                    onClick = { showCategoryMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = categories.find { it.first == selectedCategoryId }?.second ?: "Select Category",
                                        color = Gold500
                                    )
                                }
                                DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                                    categories.forEach { (id, name) ->
                                        DropdownMenuItem(
                                            text = { Text(name) },
                                            onClick = { selectedCategoryId = id; showCategoryMenu = false }
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val v = value.toBigDecimalOrNull() ?: return@Button
                                val categoryId = if (selectedScope == "CATEGORY") selectedCategoryId else null
                                onCreate(code, discountType, v, validFrom, validTo, null, categoryId)
                                showForm = false
                                code = ""; value = ""; validFrom = ""; validTo = ""
                                selectedScope = "GLOBAL"; selectedCategoryId = null
                            },
                            enabled = !isCreating && code.isNotBlank() && value.isNotBlank() &&
                                    (selectedScope == "GLOBAL" || selectedCategoryId != null),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Create", color = MaterialTheme.colorScheme.background)
                            }
                        }
                    }
                }
            }
        }

        items(discounts, key = { it.id }) { discount ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = discount.code, style = MaterialTheme.typography.titleSmall, color = Gold500, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${discount.discountType} · ${discount.value}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Prikaz scope-a
                        val scopeText = when {
                            discount.categoryName != null -> "Category: ${discount.categoryName}"
                            discount.productName != null -> "Product: ${discount.productName}"
                            else -> "Global"
                        }
                        Text(text = scopeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDelete(discount.id) }, enabled = deletingId != discount.id) {
                        if (deletingId == discount.id) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                        } else {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PromotionsTab(
    promotions: List<PromotionResponse>,
    discounts: List<DiscountResponse>,
    isCreating: Boolean,
    onCreate: (String, String, String, String, Long) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var validFrom by remember { mutableStateOf("") }
    var validTo by remember { mutableStateOf("") }
    var selectedDiscountId by remember { mutableStateOf<Long?>(null) }
    var showDiscountMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = { showForm = !showForm },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (showForm) "Cancel" else "+ Create Promotion", color = MaterialTheme.colorScheme.background)
            }
        }

        if (showForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Promotion Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                        OutlinedTextField(value = validFrom, onValueChange = { validFrom = it }, label = { Text("Valid From (2026-01-01T00:00:00)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = validTo, onValueChange = { validTo = it }, label = { Text("Valid To (2026-12-31T23:59:59)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                        Box {
                            OutlinedButton(onClick = { showDiscountMenu = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                                Text(discounts.find { it.id == selectedDiscountId }?.code ?: "Select Discount Code", color = Gold500)
                            }
                            DropdownMenu(expanded = showDiscountMenu, onDismissRequest = { showDiscountMenu = false }) {
                                discounts.forEach { discount ->
                                    DropdownMenuItem(
                                        text = { Text("${discount.code} (${discount.value})") },
                                        onClick = { selectedDiscountId = discount.id; showDiscountMenu = false }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val discountId = selectedDiscountId ?: return@Button
                                onCreate(name, description, validFrom, validTo, discountId)
                                showForm = false
                                name = ""; description = ""; validFrom = ""; validTo = ""; selectedDiscountId = null
                            },
                            enabled = !isCreating && name.isNotBlank() && selectedDiscountId != null,
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
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = promotion.name, style = MaterialTheme.typography.titleSmall, color = Gold500, fontWeight = FontWeight.Bold)
                    Text(text = promotion.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    promotion.discountCode?.let {
                        Text(text = "Code: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}