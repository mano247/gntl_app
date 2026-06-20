package com.gentlemanstore.core.util

object ErrorMapper {
    fun map(message: String?): String {
        return when {
            message == null -> "Something went wrong. Please try again."
            message.contains("401") || message.contains("Unauthorized") -> "Session expired. Please log in again."
            message.contains("403") || message.contains("Forbidden") -> "You don't have permission to do this."
            message.contains("404") || message.contains("Not found") -> "Resource not found."
            message.contains("409") || message.contains("already exists") -> "This already exists."
            message.contains("400") || message.contains("Bad Request") -> "Invalid request. Please check your input."
            message.contains("500") || message.contains("Internal Server") -> "Server error. Please try again later."
            message.contains("timeout") || message.contains("Unable to resolve") || message.contains("failed to connect") -> "No internet connection."
            else -> message
        }
    }
}