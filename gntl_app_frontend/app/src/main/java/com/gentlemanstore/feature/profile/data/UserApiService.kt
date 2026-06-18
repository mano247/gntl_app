package com.gentlemanstore.feature.profile.data

import com.gentlemanstore.core.network.ApiResponse
import com.gentlemanstore.feature.profile.data.dto.UpdateUserRequest
import com.gentlemanstore.feature.profile.data.dto.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface UserApiService {

    @GET("users/my")
    suspend fun getMyProfile(): ApiResponse<UserResponse>

    @PUT("users/my")
    suspend fun updateMyProfile(
        @Body request: UpdateUserRequest
    ): ApiResponse<UserResponse>
}