package com.gentlemanstore.feature.cart.data.dto

import java.math.BigDecimal

data class CartItemResponse(
    val id: Long,
    val productName: String,
    val productId: Long,
    val size: String,
    val quantity: Int,
    val price: BigDecimal
)

data class CartResponse(
    val id: Long,
    val items: List<CartItemResponse>,
    val totalPrice: BigDecimal
)

data class AddToCartRequest(
    val productId: Long,
    val productSizeId: Long,
    val quantity: Int
)