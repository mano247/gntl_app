package com.gentlemanstore.feature.admin.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.admin.data.AdminApiService
import com.gentlemanstore.feature.admin.data.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val adminApiService: AdminApiService
) {

    suspend fun getAllUsers(page: Int): Resource<PagedUserResponse> {
        return try {
            val response = adminApiService.getAllUsers(page)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load users")
        }
    }

    suspend fun changeUserRole(id: Long, role: String): Resource<UserListResponse> {
        return try {
            val response = adminApiService.changeUserRole(id, role)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to change user role")
        }
    }

    suspend fun deleteUser(id: Long): Resource<Unit> {
        return try {
            val response = adminApiService.deleteUser(id)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete user")
        }
    }
}