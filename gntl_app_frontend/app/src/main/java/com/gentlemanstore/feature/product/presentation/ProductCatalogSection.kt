package com.gentlemanstore.feature.product.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gentlemanstore.core.util.rememberCurrentCurrency
import com.gentlemanstore.feature.product.data.dto.ProductResponse
import com.gentlemanstore.ui.theme.Gold500

/**
 * Deljivi katalog proizvoda (pretraga, sort, kategorija-filter, infinite-scroll grid,
 * pull-to-refresh) — koristi ga customer ProductListScreen i Employee Products tab.
 * [productActions] je opcioni slot ispod svake kartice (npr. Edit/Delete za employee).
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProductCatalogSection(
    viewModel: ProductViewModel,
    onProductClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    productActions: (@Composable (ProductResponse) -> Unit)? = null
) {
    val uiState by viewModel.listUiState.collectAsStateWithLifecycle()
    val currency = rememberCurrentCurrency()
    val gridState = rememberLazyGridState()
    var showSortMenu by remember { mutableStateOf(false) }

    val sortOptions = mapOf(
        "DEFAULT" to "Default",
        "PRICE_ASC" to "Price ↑",
        "PRICE_DESC" to "Price ↓",
        "NAME_ASC" to "A → Z",
        "NAME_DESC" to "Z → A"
    )

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = { viewModel.loadProducts(refresh = true) }
    )

    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.layoutInfo.totalItemsCount) {
        val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val totalItems = gridState.layoutInfo.totalItemsCount

        if (lastVisibleItem >= totalItems - 4
            && !uiState.isLastPage
            && !uiState.isLoadingMore
            && uiState.products.isNotEmpty()) {
            viewModel.loadMoreProducts()
        }
    }

    LaunchedEffect(uiState.sortOption) {
        gridState.scrollToItem(0)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search + Sort row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search products...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Gold500)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            Box {
                OutlinedButton(
                    onClick = { showSortMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp)
                ) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Gold500)
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    sortOptions.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { viewModel.onSortChange(key); showSortMenu = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.categories.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(
                    count = uiState.categories.size,
                    key = { index -> uiState.categories[index] }
                ) { index ->
                    val category = uiState.categories[index]
                    val isSelected = when {
                        category == "All" && uiState.selectedCategory == null -> true
                        category == uiState.selectedCategory -> true
                        else -> false
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.selectCategory(
                                if (category == "All") null else category
                            )
                        },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold500,
                            selectedLabelColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            when {
                uiState.isLoading && uiState.products.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Gold500)
                    }
                }

                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadProducts(refresh = true) },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold500)
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = viewModel.sortedProducts,
                            key = { it.id }
                        ) { product ->
                            Column {
                                ProductCard(
                                    product = product,
                                    currency = currency,
                                    onClick = { onProductClick(product.id) }
                                )
                                productActions?.invoke(product)
                            }
                        }

                        if (uiState.isLoadingMore) {
                            item(span = { GridItemSpan(2) }) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Gold500, modifier = Modifier.size(24.dp))
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
