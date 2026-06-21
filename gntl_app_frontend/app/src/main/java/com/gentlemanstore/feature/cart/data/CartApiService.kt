package com.gentlemanstore.feature.cart.data

import com.gentlemanstore.core.network.ApiResponse
import com.gentlemanstore.feature.cart.data.dto.AddToCartRequest
import com.gentlemanstore.feature.cart.data.dto.CartResponse
import com.gentlemanstore.feature.manager.data.dto.DiscountResponse
import com.gentlemanstore.feature.order.data.dto.CheckoutRequest
import com.gentlemanstore.feature.order.data.dto.OrderResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CartApiService {

    @GET("cart")
    suspend fun getCart(): ApiResponse<CartResponse>

    @POST("cart/add")
    suspend fun addToCart(
        @Body request: AddToCartRequest
    ): ApiResponse<CartResponse>

    @DELETE("cart/items/{cartItemId}")
    suspend fun removeFromCart(
        @Path("cartItemId") cartItemId: Long
    ): ApiResponse<CartResponse>

    @POST("cart/checkout")
    suspend fun checkout(
        @Body request: CheckoutRequest
    ): ApiResponse<OrderResponse>

    @GET("discounts/validate/{code}")
    suspend fun validatePromoCode(
        @Path("code") code: String
    ): ApiResponse<DiscountResponse>
}