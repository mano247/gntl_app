package com.gentlemanstore.feature.loyalty.data

import com.gentlemanstore.core.network.ApiResponse
import com.gentlemanstore.feature.loyalty.data.dto.LoyaltyAccountResponse
import retrofit2.http.GET

interface LoyaltyApiService {

    @GET("loyalty")
    suspend fun getLoyaltyAccount(): ApiResponse<LoyaltyAccountResponse>
}