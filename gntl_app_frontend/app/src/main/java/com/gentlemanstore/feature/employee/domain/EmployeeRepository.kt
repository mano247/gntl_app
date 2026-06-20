package com.gentlemanstore.feature.employee.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.util.ErrorMapper
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.employee.data.EmployeeApiService
import com.gentlemanstore.feature.order.data.dto.OrderResponse
import com.gentlemanstore.feature.order.data.dto.PagedOrderResponse
import com.gentlemanstore.feature.support.data.dto.PagedTicketResponse
import com.gentlemanstore.feature.support.data.dto.SupportTicketResponse
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType


@Singleton
class EmployeeRepository @Inject constructor(
    private val employeeApiService: EmployeeApiService
) {

    suspend fun getAllOrders(page: Int): Resource<PagedOrderResponse> {
        return try {
            val response = employeeApiService.getAllOrders(page)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun updateOrderStatus(id: Long, status: String): Resource<OrderResponse> {
        return try {
            val body = status.toRequestBody("text/plain".toMediaType())
            val response = employeeApiService.updateOrderStatus(id, body)
            android.util.Log.d("EmployeeRepo", "updateOrderStatus response: success=${response.success}, message=${response.message}, data=${response.data}")
            response.toResource()
        } catch (e: Exception) {
            android.util.Log.e("EmployeeRepo", "updateOrderStatus error: ${e.message}")
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun getAllTickets(page: Int): Resource<PagedTicketResponse> {
        return try {
            val response = employeeApiService.getAllTickets(page)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun updateTicketStatus(id: Long, status: String): Resource<SupportTicketResponse> {
        return try {
            val body = status.toRequestBody("text/plain".toMediaType())
            val response = employeeApiService.updateTicketStatus(id, body)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }
}