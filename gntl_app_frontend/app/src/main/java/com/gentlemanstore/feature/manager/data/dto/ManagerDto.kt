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

// Discounts
data class DiscountResponse(
    val id: Long,
    val code: String,
    val discountType: String,
    val value: BigDecimal,
    val validFrom: String,
    val validTo: String,
    val productId: Long?,
    val productName: String?,
    val categoryId: Long?,
    val categoryName: String?
)

data class CreateDiscountRequest(
    val code: String,
    val discountType: String,
    val value: BigDecimal,
    val validFrom: String,
    val validTo: String,
    val productId: Long? = null,
    val categoryId: Long? = null
)

// Promotions
data class PromotionResponse(
    val id: Long,
    val name: String,
    val description: String,
    val validFrom: String,
    val validTo: String,
    val discountCode: String?
)

data class CreatePromotionRequest(
    val name: String,
    val description: String,
    val validFrom: String,
    val validTo: String,
    val discountId: Long
)

data class LoyaltyAccountResponse(
    val id: Long,
    val points: Int,
    val tierName: String,
    val discountPercentage: Double
)

data class AddPointsRequest(
    val points: Int,
    val description: String,
    val userId: Long? = null
)