package com.gentlemanstore.core.util

// Kategorija greske - omogucava UI-ju da razlikuje validaciju, mrezne i
// serverske greske bez string-matchinga poruka.
enum class ErrorType {
    VALIDATION,
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    SERVER,
    TIMEOUT,
    NETWORK,
    UNKNOWN
}

sealed class Resource<out T>{
    data class  Success<T>(val data: T) : Resource<T>()

    // fieldErrors: backend validacione greske po polju (npr. "email" -> poruka),
    // prikazuju se ispod odgovarajuceg input polja umesto genericke poruke.
    data class Error(
        val message: String,
        val type: ErrorType = ErrorType.UNKNOWN,
        val fieldErrors: Map<String, String> = emptyMap()
    ) : Resource<Nothing>()

    object Loading : Resource<Nothing>()
}
