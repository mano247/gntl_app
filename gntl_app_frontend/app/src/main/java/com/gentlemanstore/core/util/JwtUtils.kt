package com.gentlemanstore.core.util

import android.util.Base64
import org.json.JSONObject

object JwtUtils {

    fun decodeRole(token: String): String?{
        return try {
            val payload = token.split(".")[1]
            val decoded = Base64.decode(payload, Base64.URL_SAFE)
            val json = JSONObject(String(decoded))
            json.getString("role")
        } catch (e: Exception){
            null
        }
    }

    fun decodeUserId(token: String): String?{
        return try {
            val payload = token.split(".")[1]
            val decoded = Base64.decode(payload, Base64.URL_SAFE)
            val json = JSONObject(String(decoded))
            json.getString("sub")
        } catch (e: Exception){
            null
        }
    }

    fun isTokenExpired(token: String): Boolean{
        return try {
            val payload = token.split(".")[1]
            val decoded = Base64.decode(payload, Base64.URL_SAFE)
            val json = JSONObject(String(decoded))
            val exp = json.getLong("exp")
            System.currentTimeMillis() / 1000 > exp
        } catch (e: Exception){
            true
        }
    }
}