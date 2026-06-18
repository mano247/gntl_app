package com.gentlemanstore.feature.loyalty.data.dto

import java.math.BigDecimal

data class LoyaltyAccountResponse(
    val id: Long,
    val points: Int,
    val tierName: String,
    val discountPercentage: BigDecimal
)