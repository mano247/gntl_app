package com.gentlemanstore.feature.cart.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gentlemanstore.feature.cart.data.dto.CartItemResponse
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.gentlemanstore.core.util.CurrencyFormatter
import com.gentlemanstore.core.util.rememberCurrentCurrency
import com.gentlemanstore.ui.theme.Gold500
import androidx.compose.ui.draw.clip

@Composable
fun CartScreen(
    onNavigateToCheckout: () -> Unit,
    onShowError: (String) -> Unit = {},
    viewModel: CartViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currency = rememberCurrentCurrency()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            onShowError(it)
            viewModel.clearError()
        }
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
                    text = "CART",
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


                uiState.cart == null || uiState.cart!!.items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Your cart is empty",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    val cart = uiState.cart!!

                    LazyColumnCartItems(
                        cartItems = cart.items,
                        removingItemId = uiState.removingItemId,
                        currency = currency,
                        onRemove = { viewModel.removeFromCart(it) }
                    )

                    // Total i checkout
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Subtotal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.format(cart.totalPrice.toDouble(), currency),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (cart.loyaltyDiscount != null && cart.loyaltyDiscount > java.math.BigDecimal.ZERO) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Loyalty Discount",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                )
                                Text(
                                    text = "- ${CurrencyFormatter.format(cart.loyaltyDiscount.toDouble(), currency)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = CurrencyFormatter.format(
                                    (cart.finalPrice ?: cart.totalPrice).toDouble(), currency
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                color = Gold500,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onNavigateToCheckout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold500,
                                contentColor = MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Checkout",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.LazyColumnCartItems(
    cartItems: List<CartItemResponse>,
    removingItemId: Long?,
    currency: String,
    onRemove: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(cartItems, key = { it.id }) { item ->
            CartItemCard(
                item = item,
                isRemoving = removingItemId == item.id,
                currency = currency,
                onRemove = { onRemove(item.id) }
            )
        }
    }
}

@Composable
private fun CartItemCard(
    item: CartItemResponse,
    isRemoving: Boolean,
    currency: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            coil.compose.AsyncImage(
                model = item.imageUrl,
                contentDescription = item.productName,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Size: ${item.size} · Qty: ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.originalPrice != null && item.originalPrice != item.price) {
                    Text(
                        text = CurrencyFormatter.format(item.originalPrice.toDouble(), currency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                    )
                    Text(
                        text = CurrencyFormatter.format(item.price.toDouble(), currency),
                        style = MaterialTheme.typography.titleMedium,
                        color = androidx.compose.ui.graphics.Color(0xFFE05252),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = CurrencyFormatter.format(item.price.toDouble(), currency),
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold500
                    )
                }
            }

            IconButton(onClick = onRemove, enabled = !isRemoving) {
                if (isRemoving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}