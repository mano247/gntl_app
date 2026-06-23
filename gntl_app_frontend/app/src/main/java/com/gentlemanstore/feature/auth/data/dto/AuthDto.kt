package com.gentlemanstore.feature.auth.data.dto

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val phone: String? = null
)

data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val userId: Long
)

data class RefreshTokenRequest(
    val refreshToken: String
)