package com.gentlemanstore.feature.order.data.dto

import java.math.BigDecimal

data class OrderItemResponse(
    val id: Long,
    val productName: String,
    val imageUrl: String?,
    val size: String?,
    val quantity: Int,
    val price: BigDecimal,
    val originalPrice: BigDecimal?
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

data class PagedOrderResponse(
    val content: List<OrderResponse>,
    val totalPages: Int,
    val totalElements: Long,
    val last: Boolean,
    val first: Boolean,
    val number: Int,
    val size: Int,
    val numberOfElements: Int
)