package com.gentlemanstore.feature.address.data.dto

data class AddressResponse(
    val id: Long,
    val street: String,
    val apartment: String?,
    val city: String,
    val postalCode: String,
    val country: String,
    val isDefault: Boolean
)

data class AddressRequest(
    val street: String,
    val apartment: String?,
    val city: String,
    val postalCode: String,
    val country: String,
    val isDefault: Boolean
)