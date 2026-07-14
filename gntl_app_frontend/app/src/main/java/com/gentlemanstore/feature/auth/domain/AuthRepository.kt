package com.gentlemanstore.feature.auth.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.util.ErrorMapper
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.data.datastore.TokenDataStore
import com.gentlemanstore.feature.auth.data.AuthApiService
import com.gentlemanstore.feature.auth.data.dto.AuthResponse
import com.gentlemanstore.feature.auth.data.dto.LoginRequest
import com.gentlemanstore.feature.auth.data.dto.RefreshTokenRequest
import com.gentlemanstore.feature.auth.data.dto.RegisterRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenDataStore: TokenDataStore
) {
    suspend fun login(
        email: String,
        password: String
    ): Resource<AuthResponse>{
        return try{
            val response = authApiService.login(LoginRequest(email, password))
            val resource = response.toResource()
            if (resource is Resource.Success){
                saveUserData(resource.data)
            }
            resource
        } catch (e: Exception){
            ErrorMapper.map(e)
        }
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        phone: String? = null
    ): Resource<AuthResponse> {
        return try {
            val response = authApiService.register(
                RegisterRequest(firstName, lastName, email, password, phone)
            )
            val resource = response.toResource()
            if (resource is Resource.Success){
                saveUserData(resource.data)
            }
            resource
        }catch (e: Exception){
            ErrorMapper.map(e)
        }
    }

    suspend fun tryRefreshToken(): Boolean {
        val refreshToken = tokenDataStore.refreshToken.first() ?: return false
        return try {
            val response = authApiService.refresh(RefreshTokenRequest(refreshToken))
            val resource = response.toResource()
            if (resource is Resource.Success) {
                saveUserData(resource.data)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun logout(){
        val refreshToken = tokenDataStore.refreshToken.first()
        if (refreshToken != null) {
            try {
                authApiService.logout(RefreshTokenRequest(refreshToken))
            } catch (e: Exception) {
                // Best-effort server-side revocation; local logout must succeed regardless.
            }
        }
        tokenDataStore.clearAll()
    }

    private suspend fun saveUserData(data: AuthResponse){
        tokenDataStore.saveToken(data.token)
        tokenDataStore.saveRefreshToken(data.refreshToken)
        tokenDataStore.saveUserRole(data.role)
        tokenDataStore.saveUserId(data.userId.toString())
    }
}