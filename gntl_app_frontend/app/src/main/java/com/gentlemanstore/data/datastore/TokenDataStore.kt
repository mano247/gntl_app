package com.gentlemanstore.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gentlemanstore.core.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.DATASTORE_NAME
)

class TokenDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
){
    private val TOKEN_KEY = stringPreferencesKey(Constants.KEY_JWT_TOKEN)
    private val REFRESH_TOKEN_KEY = stringPreferencesKey(Constants.KEY_REFRESH_TOKEN)
    private val ROLE_KEY = stringPreferencesKey(Constants.KEY_USER_ROLE)
    private val USER_ID_KEY = stringPreferencesKey(Constants.KEY_USER_ID)
    private val LANGUAGE_KEY = stringPreferencesKey(Constants.KEY_LANGUAGE)
    private val CURRENCY_KEY = stringPreferencesKey(Constants.KEY_CURRENCY)

    val token: Flow<String?> = context.dataStore.data
        .map { it[TOKEN_KEY]?.let { encrypted -> cryptoManager.decrypt(encrypted) } }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[TOKEN_KEY] = cryptoManager.encrypt(token) }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(TOKEN_KEY) }
    }

    val refreshToken: Flow<String?> = context.dataStore.data
        .map { it[REFRESH_TOKEN_KEY]?.let { encrypted -> cryptoManager.decrypt(encrypted) } }

    suspend fun saveRefreshToken(refreshToken: String) {
        context.dataStore.edit { it[REFRESH_TOKEN_KEY] = cryptoManager.encrypt(refreshToken) }
    }

    suspend fun clearRefreshToken() {
        context.dataStore.edit { it.remove(REFRESH_TOKEN_KEY) }
    }

    val userRole: Flow<String?> = context.dataStore.data
        .map { it[ROLE_KEY]?.let { encrypted -> cryptoManager.decrypt(encrypted) } }

    suspend fun saveUserRole(role: String) {
        context.dataStore.edit { it[ROLE_KEY] = cryptoManager.encrypt(role) }
    }

    val userId: Flow<String?> = context.dataStore.data
        .map { it[USER_ID_KEY]?.let { encrypted -> cryptoManager.decrypt(encrypted) } }

    suspend fun saveUserId(id: String) {
        context.dataStore.edit { it[USER_ID_KEY] = cryptoManager.encrypt(id) }
    }

    val language: Flow<String> = context.dataStore.data
        .map { it[LANGUAGE_KEY] ?: Constants.LANG_EN }

    suspend fun saveLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE_KEY] = lang }
    }

    val currency: Flow<String> = context.dataStore.data
        .map { it[CURRENCY_KEY] ?: Constants.CURRENCY_RSD }

    suspend fun saveCurrency(currency: String) {
        context.dataStore.edit { it[CURRENCY_KEY] = currency }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}