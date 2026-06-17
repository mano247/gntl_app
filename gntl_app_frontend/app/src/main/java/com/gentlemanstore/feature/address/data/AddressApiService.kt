package com.gentlemanstore.feature.address.data

import com.gentlemanstore.core.network.ApiResponse
import com.gentlemanstore.feature.address.data.dto.AddressRequest
import com.gentlemanstore.feature.address.data.dto.AddressResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path

interface AddressApiService {

    @GET("addresses")
    suspend fun getAddresses(): ApiResponse<List<AddressResponse>>

    @POST("addresses")
    suspend fun createAddress(
        @Body request: AddressRequest
    ): ApiResponse<AddressResponse>

    @PUT("addresses/{id}")
    suspend fun updateAddress(
        @Path("id") id: Long,
        @Body request: AddressRequest
    ): ApiResponse<AddressResponse>

    @DELETE("addresses/{id}")
    suspend fun deleteAddress(
        @Path("id") id: Long
    ): ApiResponse<Unit>
}