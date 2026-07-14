package com.gentlemanstore.core.network

import com.gentlemanstore.core.util.ErrorMapper
import com.gentlemanstore.core.util.ErrorType
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
        // 2xx sa success=false - poruka je vec konkretna backend poruka;
        // validacioni format "field: poruka" se i ovde mapira po polju.
        val fieldErrors = ErrorMapper.parseFieldErrors(message)
        if (fieldErrors.isNotEmpty()) {
            Resource.Error("Please correct the highlighted fields.", ErrorType.VALIDATION, fieldErrors)
        } else {
            Resource.Error(message)
        }
    }
}

fun ApiResponse<Unit>.toUnitResource(): Resource<Unit> {
    return if (success) {
        Resource.Success(Unit)
    } else {
        Resource.Error(ErrorMapper.map(message))
    }
}