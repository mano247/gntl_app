package com.gentlemanstore.feature.profile.data.dto

data class AddressInUser(
    val id: Long,
    val street: String,
    val apartment: String?,
    val city: String,
    val postalCode: String,
    val country: String,
    val isDefault: Boolean
)

data class UserResponse(
    val id: Long,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?,
    val createdAt: String,
    val addresses: List<AddressInUser>?
)

data class UpdateUserRequest(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?
)