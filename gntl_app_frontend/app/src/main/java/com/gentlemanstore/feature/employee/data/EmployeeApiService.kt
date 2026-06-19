package com.gentlemanstore.feature.employee.data

import com.gentlemanstore.core.network.ApiResponse
import com.gentlemanstore.feature.order.data.dto.OrderResponse
import com.gentlemanstore.feature.order.data.dto.PagedOrderResponse
import com.gentlemanstore.feature.support.data.dto.PagedTicketResponse
import com.gentlemanstore.feature.support.data.dto.SupportTicketResponse
import retrofit2.http.*

interface EmployeeApiService {

    @GET("orders/paged")
    suspend fun getAllOrders(
        @Query("page") page: Int,
        @Query("size") size: Int = 20
    ): ApiResponse<PagedOrderResponse>

    @PUT("orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") id: Long,
        @Body status: okhttp3.RequestBody
    ): ApiResponse<OrderResponse>

    @GET("support/tickets")
    suspend fun getAllTickets(
        @Query("page") page: Int,
        @Query("size") size: Int = 20
    ): ApiResponse<PagedTicketResponse>

    @PUT("support/tickets/{id}")
    suspend fun updateTicketStatus(
        @Path("id") id: Long,
        @Body status: okhttp3.RequestBody
    ): ApiResponse<SupportTicketResponse>
}