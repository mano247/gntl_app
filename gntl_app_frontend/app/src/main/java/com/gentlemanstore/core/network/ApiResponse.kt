package com.gentlemanstore.core.network

import com.gentlemanstore.core.util.Resource

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

fun <T> ApiResponse<T>.toResource(): Resource<T> {
    return if (success && data != null){
        Resource.Success(data)
    } else {
        Resource.Error(message)
    }
}