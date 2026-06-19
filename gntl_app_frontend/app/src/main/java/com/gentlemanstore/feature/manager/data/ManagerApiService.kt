package com.gentlemanstore.feature.manager.data

import com.gentlemanstore.core.network.ApiResponse
import com.gentlemanstore.feature.manager.data.dto.*
import retrofit2.http.*

interface ManagerApiService {

    @GET("analytics/dashboard")
    suspend fun getDashboard(): ApiResponse<AnalyticsResponse>

    @GET("discounts")
    suspend fun getAllDiscounts(): ApiResponse<List<DiscountResponse>>

    @POST("discounts")
    suspend fun createDiscount(
        @Body request: CreateDiscountRequest
    ): ApiResponse<DiscountResponse>

    @DELETE("discounts/{id}")
    suspend fun deleteDiscount(
        @Path("id") id: Long
    ): ApiResponse<Unit>

    @GET("discounts/promotions/active")
    suspend fun getActivePromotions(): ApiResponse<List<PromotionResponse>>

    @POST("discounts/promotions")
    suspend fun createPromotion(
        @Body request: CreatePromotionRequest
    ): ApiResponse<PromotionResponse>

    @GET("loyalty/user/{userId}")
    suspend fun getUserLoyaltyAccount(
        @Path("userId") userId: Long
    ): ApiResponse<LoyaltyAccountResponse>

    @PUT("loyalty/points")
    suspend fun addPoints(
        @Body request: AddPointsRequest
    ): ApiResponse<LoyaltyAccountResponse>
}