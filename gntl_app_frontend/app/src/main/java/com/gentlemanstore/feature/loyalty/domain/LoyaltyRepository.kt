package com.gentlemanstore.feature.loyalty.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.loyalty.data.LoyaltyApiService
import com.gentlemanstore.feature.loyalty.data.dto.LoyaltyAccountResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoyaltyRepository @Inject constructor(
    private val loyaltyApiService: LoyaltyApiService
){
    suspend fun getLoyaltyAccount(): Resource<LoyaltyAccountResponse> {
        return try {
            val response = loyaltyApiService.getLoyaltyAccount()
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load loyalty account")
        }
    }
}