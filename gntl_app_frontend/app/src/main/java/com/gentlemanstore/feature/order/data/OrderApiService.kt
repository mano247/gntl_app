package com.gentlemanstore.feature.order.data

import com.gentlemanstore.core.network.ApiResponse
import com.gentlemanstore.feature.order.data.dto.OrderResponse
import com.gentlemanstore.feature.order.data.dto.PagedOrderResponse
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface OrderApiService {
    @GET("orders/my/paged")
    suspend fun getMyOrders(
        @Query("page") page: Int,
        @Query("size") size: Int = 20,
        @Query("status") status: String? = null
    ): ApiResponse<PagedOrderResponse>

    @GET("orders/{id}")
    suspend fun getOrderById(
        @Path("id") id: Long
    ): ApiResponse<OrderResponse>

    @PUT("orders/{id}/cancel")
    suspend fun cancelOrder(
        @Path("id") id: Long
    ): ApiResponse<Unit>
}