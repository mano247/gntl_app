package com.gentlemanstore.feature.admin.data

import com.gentlemanstore.core.network.ApiResponse
import com.gentlemanstore.feature.admin.data.dto.*
import retrofit2.http.*

interface AdminApiService {

    @GET("users/paged")
    suspend fun getAllUsers(
        @Query("page") page: Int,
        @Query("size") size: Int = 20
    ): ApiResponse<PagedUserResponse>

    @PUT("users/{id}/role")
    suspend fun changeUserRole(
        @Path("id") id: Long,
        @Body role: String
    ): ApiResponse<UserListResponse>

    @DELETE("users/{id}")
    suspend fun deleteUser(
        @Path("id") id: Long
    ): ApiResponse<Unit>
}