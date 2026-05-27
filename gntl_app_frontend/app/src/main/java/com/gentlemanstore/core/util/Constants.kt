package com.gentlemanstore.core.util

object Constants {
    // Network
    const val BASE_URL = "http://10.0.2.2:8080/api/"
    const val TIMEOUT_SECONDS = 30L

    // DataStore
    const val DATASTORE_NAME = "gentleman_store_prefs"
    const val KEY_JWT_TOKEN = "jwt_token"
    const val KEY_USER_ROLE = "user_role"
    const val KEY_USER_ID = "user_id"
    const val KEY_LANGUAGE = "language"
    const val KEY_CURRENCY = "currency"

    // Pagination
    const val PAGE_SIZE = 20
    const val FIRST_PAGE = 0

    // Currency
    const val CURRENCY_RSD = "RSD"
    const val CURRENCY_EUR = "EUR"
    const val CURRENCY_USD = "USD"

    // Language
    const val LANG_SR = "sr"
    const val LANG_EN = "en"

    // Roles
    const val ROLE_CUSTOMER = "ROLE_CUSTOMER"
    const val ROLE_EMPLOYEE = "ROLE_EMPLOYEE"
    const val ROLE_MANAGER = "ROLE_MANAGER"
    const val ROLE_ADMIN = "ROLE_ADMIN"
}