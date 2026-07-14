package com.gentlemanstore.core.util

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Centralni prevodilac gresaka za ceo frontend.
 *
 * Backend uvek vraca `{ success, message, data }` (ApiResponse envelope) i za
 * greske - `message` je konkretna, korisnicki upotrebljiva poruka
 * (GlobalExceptionHandler). Retrofit za ne-2xx odgovore baca [HttpException],
 * a pravo telo greske je u `errorBody()` - ovde se parsira i mapira u
 * [Resource.Error] sa tipom greske i (za validacione greske) mapom
 * field -> poruka, koju forme prikazuju ispod odgovarajuceg polja.
 *
 * Backend validacione poruke imaju format "field: poruka, field2: poruka2"
 * (MethodArgumentNotValidException handler).
 */
object ErrorMapper {

    private val gson = Gson()

    // Telo backend greske - koristi se samo message deo envelope-a
    private data class BackendErrorBody(val success: Boolean?, val message: String?)

    // "field: poruka" segment na pocetku stringa ili posle ", " -
    // field je jedna rec (Java property naziv), sto iskljucuje obicne
    // recenice koje sadrze dvotacku.
    private val fieldSegmentRegex = Regex("(^|, )([a-zA-Z][a-zA-Z0-9_.]*): ")

    /**
     * Glavna ulazna tacka - poziva se iz repository catch blokova.
     * CancellationException se prosledjuje dalje da otkazivanje coroutine
     * (npr. pri promeni filtera/pretrage) ne bi zavrsilo kao lazna greska.
     */
    fun map(e: Throwable): Resource.Error {
        if (e is CancellationException) throw e
        return when (e) {
            is HttpException -> mapHttpException(e)
            is SocketTimeoutException -> Resource.Error(
                message = "Request timed out. Please try again.",
                type = ErrorType.TIMEOUT
            )
            is IOException -> Resource.Error(
                message = "No internet connection. Please check your network.",
                type = ErrorType.NETWORK
            )
            else -> Resource.Error(
                message = "Something went wrong. Please try again.",
                type = ErrorType.UNKNOWN
            )
        }
    }

    private fun mapHttpException(e: HttpException): Resource.Error {
        val backendMessage = readBackendMessage(e)
        val type = when (e.code()) {
            400 -> ErrorType.BAD_REQUEST
            401 -> ErrorType.UNAUTHORIZED
            403 -> ErrorType.FORBIDDEN
            404 -> ErrorType.NOT_FOUND
            409 -> ErrorType.CONFLICT
            in 500..599 -> ErrorType.SERVER
            else -> ErrorType.UNKNOWN
        }

        // Validacione greske po polju backend vraca kao 400 sa
        // "field: poruka, ..." - prikazuju se ispod polja, ne u snackbar-u.
        if (type == ErrorType.BAD_REQUEST && backendMessage != null) {
            val fieldErrors = parseFieldErrors(backendMessage)
            if (fieldErrors.isNotEmpty()) {
                return Resource.Error(
                    message = "Please correct the highlighted fields.",
                    type = ErrorType.VALIDATION,
                    fieldErrors = fieldErrors
                )
            }
        }

        val message = when {
            // Server 5xx poruke sa backenda su vec genericke ("An unexpected
            // error occurred") - ne prikazujemo interne detalje ni stack trace.
            type == ErrorType.SERVER -> "Server error. Please try again later."
            backendMessage != null -> backendMessage
            else -> defaultMessageFor(type)
        }
        return Resource.Error(message = message, type = type)
    }

    private fun readBackendMessage(e: HttpException): String? {
        return try {
            val body = e.response()?.errorBody()?.string()
            if (body.isNullOrBlank()) null
            else gson.fromJson(body, BackendErrorBody::class.java)?.message?.takeIf { it.isNotBlank() }
        } catch (_: JsonSyntaxException) {
            null
        } catch (_: IOException) {
            null
        }
    }

    /**
     * Parsira "email: must not be blank, password: too short" u mapu
     * {email -> "must not be blank", password -> "too short"}. Vraca praznu
     * mapu ako poruka ne pocinje field segmentom (obicna poruka).
     */
    fun parseFieldErrors(message: String): Map<String, String> {
        val matches = fieldSegmentRegex.findAll(message).toList()
        if (matches.isEmpty() || matches.first().range.first != 0) return emptyMap()

        val result = LinkedHashMap<String, String>()
        matches.forEachIndexed { index, match ->
            val field = match.groupValues[2]
            val valueStart = match.range.last + 1
            val valueEnd = if (index < matches.lastIndex) matches[index + 1].range.first else message.length
            val value = message.substring(valueStart, valueEnd).trim().trimEnd(',')
            if (value.isNotBlank()) result[field] = value
        }
        return result
    }

    private fun defaultMessageFor(type: ErrorType): String = when (type) {
        ErrorType.UNAUTHORIZED -> "Session expired. Please log in again."
        ErrorType.FORBIDDEN -> "You don't have permission to do this."
        ErrorType.NOT_FOUND -> "Resource not found."
        ErrorType.CONFLICT -> "This already exists."
        ErrorType.BAD_REQUEST -> "Invalid request. Please check your input."
        ErrorType.SERVER -> "Server error. Please try again later."
        ErrorType.TIMEOUT -> "Request timed out. Please try again."
        ErrorType.NETWORK -> "No internet connection. Please check your network."
        else -> "Something went wrong. Please try again."
    }

    /**
     * Zadrzano za pozivaoce koji imaju samo string poruku (2xx odgovor sa
     * success=false - poruka tada vec dolazi iz backend envelope-a).
     */
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
