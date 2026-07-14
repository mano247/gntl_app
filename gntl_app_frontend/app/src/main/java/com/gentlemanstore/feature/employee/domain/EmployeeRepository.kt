package com.gentlemanstore.feature.employee.domain

import com.gentlemanstore.BuildConfig
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

    suspend fun getAllOrders(page: Int, status: String? = null): Resource<PagedOrderResponse> {
        return try {
            val response = employeeApiService.getAllOrders(page = page, status = status)
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun updateOrderStatus(id: Long, status: String): Resource<OrderResponse> {
        return try {
            val body = status.toRequestBody("text/plain".toMediaType())
            val response = employeeApiService.updateOrderStatus(id, body)
            if (BuildConfig.DEBUG) {
                android.util.Log.d("EmployeeRepo", "updateOrderStatus response: success=${response.success}, message=${response.message}, data=${response.data}")
            }
            response.toResource()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("EmployeeRepo", "updateOrderStatus error: ${e.message}")
            }
            ErrorMapper.map(e)
        }
    }

    suspend fun getAllTickets(page: Int): Resource<PagedTicketResponse> {
        return try {
            val response = employeeApiService.getAllTickets(page)
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun updateTicketStatus(id: Long, status: String): Resource<SupportTicketResponse> {
        return try {
            val body = status.toRequestBody("text/plain".toMediaType())
            val response = employeeApiService.updateTicketStatus(id, body)
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }
}