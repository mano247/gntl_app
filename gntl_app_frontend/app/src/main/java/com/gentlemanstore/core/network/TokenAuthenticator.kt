package com.gentlemanstore.core.network

import com.gentlemanstore.core.util.Constants
import com.gentlemanstore.data.datastore.TokenDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

private data class RefreshAuthData(val token: String, val refreshToken: String)
private data class RefreshApiResponse(val success: Boolean, val message: String, val data: RefreshAuthData?)

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : Authenticator {

    private val refreshClient = OkHttpClient.Builder().build()
    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        synchronized(this) {
            val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val currentToken = runBlocking { tokenDataStore.token.first() }

            if (currentToken != null && currentToken != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = runBlocking { tokenDataStore.refreshToken.first() } ?: return null
            val result = performRefresh(refreshToken)
            if (result is RefreshResult.Rejected) {
                // The server definitively rejected the refresh token (revoked/expired) —
                // keeping the dead tokens would leave the app in a broken "logged in" state.
                runBlocking { tokenDataStore.clearAll() }
                return null
            }
            val refreshed = (result as? RefreshResult.Success)?.data ?: return null

            runBlocking {
                tokenDataStore.saveToken(refreshed.token)
                tokenDataStore.saveRefreshToken(refreshed.refreshToken)
            }

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshed.token}")
                .build()
        }
    }

    private sealed interface RefreshResult {
        data class Success(val data: RefreshAuthData) : RefreshResult
        // Server explicitly rejected the token (401/403) — it will never work again.
        object Rejected : RefreshResult
        // Transient failure (network error, 5xx) — the token may still be valid.
        object Failed : RefreshResult
    }

    private fun performRefresh(refreshToken: String): RefreshResult {
        return try {
            val json = gson.toJson(mapOf("refreshToken" to refreshToken))
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(Constants.BASE_URL + "auth/refresh")
                .post(body)
                .build()

            refreshClient.newCall(request).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403) return RefreshResult.Rejected
                if (!resp.isSuccessful) return RefreshResult.Failed
                val bodyString = resp.body?.string() ?: return RefreshResult.Failed
                val parsed = gson.fromJson(bodyString, RefreshApiResponse::class.java)
                val data = if (parsed.success) parsed.data else null
                if (data != null) RefreshResult.Success(data) else RefreshResult.Failed
            }
        } catch (e: Exception) {
            RefreshResult.Failed
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
