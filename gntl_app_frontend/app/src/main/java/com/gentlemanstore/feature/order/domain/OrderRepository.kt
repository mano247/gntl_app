package com.gentlemanstore.feature.order.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.order.data.OrderApiService
import com.gentlemanstore.feature.order.data.dto.OrderResponse
import com.gentlemanstore.feature.order.data.dto.PagedOrderResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val orderApiService: OrderApiService
){
    suspend fun getMyOrders(page: Int, size: Int = 20): Resource<PagedOrderResponse> {
        return try {
            val response = orderApiService.getMyOrders(page, size)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load orders")
        }
    }

    suspend fun getOrderById(id: Long): Resource<OrderResponse> {
        return try {
            val response = orderApiService.getOrderById(id)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load order")
        }
    }

    suspend fun cancelOrder(id: Long): Resource<Unit> {
        return try {
            val response = orderApiService.cancelOrder(id)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to cancel order")
        }
    }
}