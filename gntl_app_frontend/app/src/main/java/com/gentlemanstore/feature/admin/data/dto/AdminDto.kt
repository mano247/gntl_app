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