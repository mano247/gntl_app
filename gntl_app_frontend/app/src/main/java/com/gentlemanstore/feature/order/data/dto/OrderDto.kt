package com.gentlemanstore.feature.order.data.dto

import java.math.BigDecimal

data class OrderItemResponse(
    val id: Long,
    val productName: String,
    val quantity: Int,
    val price: BigDecimal
)

data class OrderResponse(
    val id: Long,
    val totalPrice: BigDecimal,
    val status: String,
    val createdAt: String,
    val items: List<OrderItemResponse>
)

data class CheckoutRequest(
    val addressId: Long
)