package com.gentlemanstore.feature.address.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.util.ErrorMapper
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.address.data.AddressApiService
import com.gentlemanstore.feature.address.data.dto.AddressRequest
import com.gentlemanstore.feature.address.data.dto.AddressResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressRepository @Inject constructor(
    private val addressApiService: AddressApiService
) {

    suspend fun getAddresses(): Resource<List<AddressResponse>> {
        return try {
            val response = addressApiService.getAddresses()
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun createAddress(request: AddressRequest): Resource<AddressResponse> {
        return try {
            val response = addressApiService.createAddress(request)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun updateAddress(id: Long, request: AddressRequest): Resource<AddressResponse> {
        return try {
            val response = addressApiService.updateAddress(id, request)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun deleteAddress(id: Long): Resource<Unit> {
        return try {
            val response = addressApiService.deleteAddress(id)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }
}