package com.gentlemanstore.feature.manager.data.dto

import java.math.BigDecimal

// Analytics
data class MonthlyRevenueResponse(
    val month: Int,
    val year: Int,
    val revenue: BigDecimal
)

data class TopProductResponse(
    val productName: String,
    val totalSold: Int
)

data class AnalyticsResponse(
    val totalRevenue: BigDecimal,
    val totalOrders: Int,
    val newUsers: Int,
    val averageOrderValue: BigDecimal,
    val monthlyRevenue: List<MonthlyRevenueResponse>,
    val topProducts: List<TopProductResponse>
)

// Discounts — automatski popusti (GLOBAL ili CATEGORY), bez promo koda
data class DiscountResponse(
    val id: Long,
    val discountType: String,
    val value: BigDecimal,
    val validFrom: String,
    val validTo: String,
    val scope: String,
    val categoryId: Long?,
    val categoryName: String?
)

data class CreateDiscountRequest(
    val discountType: String,
    val value: BigDecimal,
    val validFrom: String,
    val validTo: String,
    val scope: String,
    val categoryId: Long? = null
)

// Promotions — promo kodovi koje customer unosi na checkout-u
data class PromotionResponse(
    val id: Long,
    val name: String,
    val description: String,
    val code: String,
    val discountType: String,
    val value: BigDecimal,
    val validFrom: String,
    val validTo: String,
    val active: Boolean
)

data class CreatePromotionRequest(
    val name: String,
    val description: String,
    val code: String?,
    val discountType: String,
    val value: BigDecimal,
    val validFrom: String,
    val validTo: String
)
