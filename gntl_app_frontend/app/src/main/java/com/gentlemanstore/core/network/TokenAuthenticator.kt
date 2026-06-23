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
            val refreshed = performRefresh(refreshToken) ?: return null

            runBlocking {
                tokenDataStore.saveToken(refreshed.token)
                tokenDataStore.saveRefreshToken(refreshed.refreshToken)
            }

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshed.token}")
                .build()
        }
    }

    private fun performRefresh(refreshToken: String): RefreshAuthData? {
        return try {
            val json = gson.toJson(mapOf("refreshToken" to refreshToken))
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(Constants.BASE_URL + "auth/refresh")
                .post(body)
                .build()

            refreshClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bodyString = resp.body?.string() ?: return null
                val parsed = gson.fromJson(bodyString, RefreshApiResponse::class.java)
                if (parsed.success) parsed.data else null
            }
        } catch (e: Exception) {
            null
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
