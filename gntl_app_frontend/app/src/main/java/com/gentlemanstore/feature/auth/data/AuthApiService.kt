package com.gentlemanstore.feature.auth.data

import com.gentlemanstore.core.network.ApiResponse
import com.gentlemanstore.feature.auth.data.dto.AuthResponse
import com.gentlemanstore.feature.auth.data.dto.LoginRequest
import com.gentlemanstore.feature.auth.data.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): ApiResponse<AuthResponse>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): ApiResponse<AuthResponse>
}
