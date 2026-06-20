package com.gentlemanstore.feature.admin.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.util.ErrorMapper
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.admin.data.AdminApiService
import com.gentlemanstore.feature.admin.data.dto.*
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

@Singleton
class AdminRepository @Inject constructor(
    private val adminApiService: AdminApiService
) {

    suspend fun getAllUsers(page: Int, deleted: Boolean? = null): Resource<PagedUserResponse> {
        return try {
            val response = adminApiService.getAllUsers(page, deleted = deleted)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun changeUserRole(id: Long, role: String): Resource<UserListResponse> {
        return try {
            val body = role.toRequestBody("text/plain".toMediaType())
            val response = adminApiService.changeUserRole(id, body)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun deleteUser(id: Long): Resource<Unit> {
        return try {
            val response = adminApiService.deleteUser(id)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun reactivateUser(id: Long): Resource<UserListResponse> {
        return try {
            val response = adminApiService.reactivateUser(id)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }
}