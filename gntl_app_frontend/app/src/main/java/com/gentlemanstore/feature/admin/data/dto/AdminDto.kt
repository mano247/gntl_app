package com.gentlemanstore.feature.admin.data.dto

data class UserListResponse(
    val id: Long,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?,
    val createdAt: String,
    val addresses: List<Any>?,
    val role: String? = null,
    val deleted: Boolean? = false
)

// Telo za PUT /users/{id}/role - backend ocekuje JSON {"role": "ROLE_ADMIN"}
// (ChangeRoleRequest sa RoleName enumom), NE plain-text string.
data class ChangeRoleRequest(
    val role: String
)

data class PagedUserResponse(
    val content: List<UserListResponse>,
    val totalPages: Int,
    val totalElements: Long,
    val last: Boolean,
    val first: Boolean,
    val number: Int,
    val size: Int,
    val numberOfElements: Int
)