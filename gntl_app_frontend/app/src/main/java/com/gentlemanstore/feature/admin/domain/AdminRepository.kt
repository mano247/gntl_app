package com.gentlemanstore.feature.admin.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.network.toUnitResource
import com.gentlemanstore.core.util.ErrorMapper
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.admin.data.AdminApiService
import com.gentlemanstore.feature.admin.data.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val adminApiService: AdminApiService
) {

    suspend fun getAllUsers(page: Int, deleted: Boolean? = null): Resource<PagedUserResponse> {
        return try {
            val response = adminApiService.getAllUsers(page, deleted = deleted)
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    /**
     * [role] je UI naziv role bez prefiksa ("ADMIN", "MANAGER", "EMPLOYEE",
     * "CUSTOMER") - backend RoleName enum zahteva "ROLE_" prefiks, a telo mora
     * biti JSON ChangeRoleRequest (raniji plain-text body je bio uzrok da
     * Change Role nikada ne radi - backend ga odbija pre kontrolera).
     */
    suspend fun changeUserRole(id: Long, role: String): Resource<UserListResponse> {
        return try {
            val roleName = if (role.startsWith("ROLE_")) role else "ROLE_${role.uppercase()}"
            val response = adminApiService.changeUserRole(id, ChangeRoleRequest(roleName))
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun deleteUser(id: Long): Resource<Unit> {
        return try {
            val response = adminApiService.deleteUser(id)
            // Backend na DELETE vraća data = null, pa toResource() (koji zahteva
            // data != null) uvek završi kao Error i lista se nikad ne osveži —
            // za Void odgovore mora toUnitResource() koji gleda samo success flag.
            response.toUnitResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun reactivateUser(id: Long): Resource<UserListResponse> {
        return try {
            val response = adminApiService.reactivateUser(id)
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }
}