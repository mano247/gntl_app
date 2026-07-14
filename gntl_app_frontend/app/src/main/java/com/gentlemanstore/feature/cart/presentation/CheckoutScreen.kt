package com.gentlemanstore.feature.cart.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gentlemanstore.feature.address.data.dto.AddressResponse
import com.gentlemanstore.feature.address.presentation.AddressViewModel
import com.gentlemanstore.feature.order.data.dto.OrderResponse
import com.gentlemanstore.ui.theme.Gold500
import androidx.compose.ui.draw.clip
import com.gentlemanstore.core.util.CurrencyFormatter
import com.gentlemanstore.core.util.rememberCurrentCurrency

enum class PaymentMethod(val label: String) {
    CASH("Cash on Delivery"),
    CARD("Credit / Debit Card")
}

@Composable
fun CheckoutScreen(
    onOrderPlaced: (OrderResponse) -> Unit,
    onNavigateBack: () -> Unit,
    onShowError: (String) -> Unit = {},
    cartViewModel: CartViewModel,
    addressViewModel: AddressViewModel = hiltViewModel()
) {
    val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val addressState by addressViewModel.uiState.collectAsStateWithLifecycle()
    val currency = rememberCurrentCurrency()

    var selectedPayment by remember { mutableStateOf(PaymentMethod.CASH) }

    LaunchedEffect(Unit) {
        cartViewModel.loadCart()
    }

    LaunchedEffect(cartState.checkoutSuccess) {
        if (cartState.checkoutSuccess && cartState.completedOrder != null) {
            onOrderPlaced(cartState.completedOrder!!)
            cartViewModel.resetCheckoutState()
        }
    }

    LaunchedEffect(cartState.error) {
        cartState.error?.let {
            onShowError(it)
            cartViewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CHECKOUT",
                    style = MaterialTheme.typography.titleLarge,
                    color = Gold500
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Text(
                        text = "ORDER SUMMARY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    cartState.cart?.items?.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            coil.compose.AsyncImage(
                                model = item.imageUrl,
                                contentDescription = item.productName,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.productName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Size: ${item.size} · Qty: ${item.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (item.originalPrice != null && item.originalPrice != item.price) {
                                    Text(
                                        text = CurrencyFormatter.format(item.originalPrice.toDouble(), currency),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(item.price.toDouble(), currency),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = androidx.compose.ui.graphics.Color(0xFFE05252),
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = CurrencyFormatter.format(item.price.toDouble(), currency),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Gold500,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                // Adresa sekcija
                item {
                    Text(
                        text = "DELIVERY ADDRESS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (addressState.isLoading) {
                    item {
                        CircularProgressIndicator(color = Gold500)
                    }
                } else {
                    items(addressState.addresses, key = { it.id }) { address ->
                        AddressCard(
                            address = address,
                            isSelected = addressState.selectedAddressId == address.id,
                            onClick = { addressViewModel.selectAddress(address.id) }
                        )
                    }

                    item {
                        if (addressState.showCreateForm) {
                            AddressForm(
                                isCreating = addressState.isCreating,
                                fieldErrors = addressState.fieldErrors,
                                onFieldEdited = { addressViewModel.clearFieldError(it) },
                                onSubmit = { street, apartment, city, postalCode, country ->
                                    addressViewModel.createAddress(
                                        street = street,
                                        apartment = apartment,
                                        city = city,
                                        postalCode = postalCode,
                                        country = country,
                                        isDefault = addressState.addresses.isEmpty()
                                    )
                                },
                                onCancel = { addressViewModel.toggleCreateForm() }
                            )
                        } else {
                            OutlinedButton(
                                onClick = { addressViewModel.toggleCreateForm() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add New Address")
                            }
                        }
                    }
                }

                // Payment metoda
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PAYMENT METHOD",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(PaymentMethod.entries.toList()) { method ->
                    PaymentMethodCard(
                        method = method,
                        isSelected = selectedPayment == method,
                        onClick = { selectedPayment = method }
                    )
                }

                // Promo kod
                // Promo kod
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PROMO CODE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = cartState.promoCode,
                            onValueChange = { cartViewModel.onPromoCodeChange(it) },
                            placeholder = { Text("Enter promo code") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            isError = cartState.promoError != null
                        )
                        Button(
                            onClick = { cartViewModel.validatePromoCode() },
                            enabled = cartState.promoCode.isNotBlank() && !cartState.isValidatingPromo,
                            colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (cartState.isValidatingPromo) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.background
                                )
                            } else {
                                Text("Apply", color = MaterialTheme.colorScheme.background)
                            }
                        }
                    }

                    cartState.promoError?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    cartState.promoDiscount?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✓ Promo code applied! Saving ${CurrencyFormatter.format(it, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                        )
                    }
                }
            }

            // Bottom - Total + Place Order
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                cartState.cart?.let { cart ->
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

                    cartState.promoDiscount?.let { promoDisc ->
                        if (promoDisc > 0.0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Promo Code",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                )
                                Text(
                                    text = "- ${CurrencyFormatter.format(promoDisc, currency)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val finalPrice = (cart.finalPrice ?: cart.totalPrice).toDouble() - (cartState.promoDiscount ?: 0.0)
                        Text(
                            text = CurrencyFormatter.format(finalPrice, currency),
                            style = MaterialTheme.typography.titleLarge,
                            color = Gold500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        addressState.selectedAddressId?.let { addressId ->
                            cartViewModel.checkout(addressId)
                        }
                    },
                    enabled = addressState.selectedAddressId != null && !cartState.isCheckingOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold500,
                        contentColor = MaterialTheme.colorScheme.background
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (cartState.isCheckingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.background
                        )
                    } else {
                        Text(
                            text = "Place Order",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressCard(
    address: AddressResponse,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Gold500 else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) Gold500 else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = address.street + (address.apartment?.let { ", $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${address.city}, ${address.postalCode}, ${address.country}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Gold500 else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) Gold500 else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = method.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AddressForm(
    isCreating: Boolean,
    onSubmit: (street: String, apartment: String?, city: String, postalCode: String, country: String) -> Unit,
    onCancel: () -> Unit,
    fieldErrors: Map<String, String> = emptyMap(),
    onFieldEdited: (String) -> Unit = {}
) {
    var street by remember { mutableStateOf("") }
    var apartment by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Serbia") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = street,
            onValueChange = { street = it; onFieldEdited("street") },
            label = { Text("Street") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = fieldErrors.containsKey("street"),
            supportingText = fieldErrors["street"]?.let { msg ->
                { Text(text = msg, color = MaterialTheme.colorScheme.error) }
            }
        )
        OutlinedTextField(
            value = apartment,
            onValueChange = { apartment = it; onFieldEdited("apartment") },
            label = { Text("Apartment (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = fieldErrors.containsKey("apartment"),
            supportingText = fieldErrors["apartment"]?.let { msg ->
                { Text(text = msg, color = MaterialTheme.colorScheme.error) }
            }
        )
        OutlinedTextField(
            value = city,
            onValueChange = { city = it; onFieldEdited("city") },
            label = { Text("City") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = fieldErrors.containsKey("city"),
            supportingText = fieldErrors["city"]?.let { msg ->
                { Text(text = msg, color = MaterialTheme.colorScheme.error) }
            }
        )
        OutlinedTextField(
            value = postalCode,
            onValueChange = { postalCode = it; onFieldEdited("postalCode") },
            label = { Text("Postal Code") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = fieldErrors.containsKey("postalCode"),
            supportingText = fieldErrors["postalCode"]?.let { msg ->
                { Text(text = msg, color = MaterialTheme.colorScheme.error) }
            }
        )
        OutlinedTextField(
            value = country,
            onValueChange = { country = it; onFieldEdited("country") },
            label = { Text("Country") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = fieldErrors.containsKey("country"),
            supportingText = fieldErrors["country"]?.let { msg ->
                { Text(text = msg, color = MaterialTheme.colorScheme.error) }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    onSubmit(
                        street,
                        apartment.ifBlank { null },
                        city,
                        postalCode,
                        country
                    )
                },
                enabled = !isCreating && street.isNotBlank() && city.isNotBlank() && postalCode.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                modifier = Modifier.weight(1f)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save", color = MaterialTheme.colorScheme.background)
                }
            }
        }
    }
}