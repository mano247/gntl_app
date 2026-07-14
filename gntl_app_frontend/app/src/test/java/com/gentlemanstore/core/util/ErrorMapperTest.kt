package com.gentlemanstore.core.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class ErrorMapperTest {

    private fun httpException(code: Int, message: String?): HttpException {
        val json = if (message == null) "" else """{"success":false,"message":"$message","data":null}"""
        val body = json.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Any>(code, body))
    }

    // ---------- parsiranje backend poruke iz error body-ja ----------

    @Test
    fun `404 vraca konkretnu backend poruku umesto genericke`() {
        val error = ErrorMapper.map(httpException(404, "Support ticket not found"))

        assertEquals("Support ticket not found", error.message)
        assertEquals(ErrorType.NOT_FOUND, error.type)
    }

    @Test
    fun `409 vraca backend poruku i CONFLICT tip`() {
        val error = ErrorMapper.map(httpException(409, "Email already exists"))

        assertEquals("Email already exists", error.message)
        assertEquals(ErrorType.CONFLICT, error.type)
    }

    @Test
    fun `400 bez field formata vraca backend poruku kao opstu gresku`() {
        val error = ErrorMapper.map(httpException(400, "Invalid ticket status: XYZ"))

        assertEquals("Invalid ticket status: XYZ", error.message)
        assertEquals(ErrorType.BAD_REQUEST, error.type)
        assertTrue(error.fieldErrors.isEmpty())
    }

    @Test
    fun `401 i 403 dobijaju odgovarajuce tipove`() {
        assertEquals(ErrorType.UNAUTHORIZED, ErrorMapper.map(httpException(401, "Unauthorized")).type)
        assertEquals(ErrorType.FORBIDDEN, ErrorMapper.map(httpException(403, "Forbidden")).type)
    }

    @Test
    fun `500 ne prosledjuje interne poruke korisniku`() {
        val error = ErrorMapper.map(httpException(500, "NullPointerException at line 42"))

        assertEquals("Server error. Please try again later.", error.message)
        assertEquals(ErrorType.SERVER, error.type)
    }

    @Test
    fun `prazan error body pada na podrazumevanu poruku za status`() {
        val error = ErrorMapper.map(httpException(404, null))

        assertEquals("Resource not found.", error.message)
    }

    // ---------- validacione greske po polju ----------

    @Test
    fun `validaciona 400 poruka se mapira po poljima`() {
        val error = ErrorMapper.map(
            httpException(400, "email: must be a well-formed email address, password: Password must contain a digit")
        )

        assertEquals(ErrorType.VALIDATION, error.type)
        assertEquals("must be a well-formed email address", error.fieldErrors["email"])
        assertEquals("Password must contain a digit", error.fieldErrors["password"])
    }

    @Test
    fun `parseFieldErrors vraca praznu mapu za obicnu poruku sa dvotackom`() {
        assertTrue(ErrorMapper.parseFieldErrors("Invalid ticket status: OPENX").isEmpty())
        assertTrue(ErrorMapper.parseFieldErrors("Something went wrong").isEmpty())
    }

    @Test
    fun `parseFieldErrors radi za jedno polje`() {
        val fields = ErrorMapper.parseFieldErrors("price: must be greater than 0")

        assertEquals(1, fields.size)
        assertEquals("must be greater than 0", fields["price"])
    }

    @Test
    fun `parseFieldErrors cuva poruku polja koja sadrzi zarez`() {
        val fields = ErrorMapper.parseFieldErrors("password: must contain a digit, an uppercase letter and a symbol, email: must not be blank")

        assertEquals("must contain a digit, an uppercase letter and a symbol", fields["password"])
        assertEquals("must not be blank", fields["email"])
    }

    // ---------- mrezne greske ----------

    @Test
    fun `timeout i network exception dobijaju svoje tipove i poruke`() {
        val timeout = ErrorMapper.map(SocketTimeoutException("timeout"))
        assertEquals(ErrorType.TIMEOUT, timeout.type)
        assertEquals("Request timed out. Please try again.", timeout.message)

        val network = ErrorMapper.map(IOException("Unable to resolve host"))
        assertEquals(ErrorType.NETWORK, network.type)
        assertEquals("No internet connection. Please check your network.", network.message)
    }

    @Test
    fun `nepoznata greska daje genericku poruku bez internih detalja`() {
        val error = ErrorMapper.map(IllegalStateException("secret internal state"))

        assertEquals(ErrorType.UNKNOWN, error.type)
        assertEquals("Something went wrong. Please try again.", error.message)
    }
}
