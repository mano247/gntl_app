package com.gentlemanstore.feature.profile.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.util.ErrorMapper
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.profile.data.UserApiService
import com.gentlemanstore.feature.profile.data.dto.UpdateUserRequest
import com.gentlemanstore.feature.profile.data.dto.UserResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApiService: UserApiService
) {

    suspend fun getMyProfile(): Resource<UserResponse> {
        return try {
            val response = userApiService.getMyProfile()
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun updateMyProfile(request: UpdateUserRequest): Resource<UserResponse> {
        return try {
            val response = userApiService.updateMyProfile(request)
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }
}