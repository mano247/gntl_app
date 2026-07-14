package com.gentlemanstore.feature.address.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.address.data.dto.AddressRequest
import com.gentlemanstore.feature.address.data.dto.AddressResponse
import com.gentlemanstore.feature.address.domain.AddressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddressUiState(
    val isLoading: Boolean = false,
    val addresses: List<AddressResponse> = emptyList(),
    val selectedAddressId: Long? = null,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val isCreating: Boolean = false,
    val showCreateForm: Boolean = false
)

@HiltViewModel
class AddressViewModel @Inject constructor(
    private val addressRepository: AddressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddressUiState())
    val uiState: StateFlow<AddressUiState> = _uiState.asStateFlow()

    init {
        loadAddresses()
    }

    fun loadAddresses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = addressRepository.getAddresses()) {
                is Resource.Success -> {
                    val defaultAddress = result.data.firstOrNull { it.isDefault }
                        ?: result.data.firstOrNull()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        addresses = result.data,
                        selectedAddressId = defaultAddress?.id,
                        showCreateForm = result.data.isEmpty(),
                        error = null
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun selectAddress(addressId: Long) {
        _uiState.value = _uiState.value.copy(selectedAddressId = addressId)
    }

    fun toggleCreateForm() {
        _uiState.value = _uiState.value.copy(showCreateForm = !_uiState.value.showCreateForm)
    }

    fun createAddress(
        street: String,
        apartment: String?,
        city: String,
        postalCode: String,
        country: String,
        isDefault: Boolean
    ) {
        // Sprecava dupli submit dok je prethodni zahtev u toku
        if (_uiState.value.isCreating) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, fieldErrors = emptyMap(), error = null)

            val request = AddressRequest(
                street = street,
                apartment = apartment,
                city = city,
                postalCode = postalCode,
                country = country,
                isDefault = isDefault
            )

            when (val result = addressRepository.createAddress(request)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        showCreateForm = false,
                        addresses = _uiState.value.addresses + result.data,
                        selectedAddressId = result.data.id
                    )
                }
                is Resource.Error -> {
                    // Validacione greske idu ispod polja forme; forma ostaje
                    // otvorena i unos ostaje sacuvan (state je u ekranu).
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        error = if (result.fieldErrors.isEmpty()) result.message else null,
                        fieldErrors = result.fieldErrors
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearFieldError(field: String) {
        _uiState.value = _uiState.value.copy(fieldErrors = _uiState.value.fieldErrors - field)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}