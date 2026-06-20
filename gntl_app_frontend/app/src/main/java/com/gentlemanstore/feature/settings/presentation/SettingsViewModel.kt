package com.gentlemanstore.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.util.Constants
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.data.datastore.TokenDataStore
import com.gentlemanstore.feature.profile.data.dto.UpdateUserRequest
import com.gentlemanstore.feature.profile.domain.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val isUpdating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tokenDataStore: TokenDataStore,
    private val userRepository: UserRepository
) : ViewModel() {

    val currency = tokenDataStore.currency.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Constants.CURRENCY_RSD
    )

    private val _profileState = MutableStateFlow(EditProfileUiState())
    val profileState: StateFlow<EditProfileUiState> = _profileState.asStateFlow()

    init {
        loadProfile()
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            tokenDataStore.saveCurrency(currency)
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            when (val result = userRepository.getMyProfile()) {
                is Resource.Success -> {
                    _profileState.value = _profileState.value.copy(
                        firstName = result.data.firstName,
                        lastName = result.data.lastName,
                        phoneNumber = result.data.phoneNumber ?: ""
                    )
                }
                else -> Unit
            }
        }
    }

    fun onFirstNameChange(value: String) {
        _profileState.value = _profileState.value.copy(firstName = value)
    }

    fun onLastNameChange(value: String) {
        _profileState.value = _profileState.value.copy(lastName = value)
    }

    fun onPhoneNumberChange(value: String) {
        _profileState.value = _profileState.value.copy(phoneNumber = value)
    }

    fun updateProfile() {
        viewModelScope.launch {
            _profileState.value = _profileState.value.copy(isUpdating = true, error = null)
            when (val result = userRepository.updateMyProfile(
                UpdateUserRequest(
                    firstName = _profileState.value.firstName,
                    lastName = _profileState.value.lastName,
                    phoneNumber = _profileState.value.phoneNumber.ifBlank { null }
                )
            )) {
                is Resource.Success -> {
                    _profileState.value = _profileState.value.copy(
                        isUpdating = false,
                        successMessage = "Profile updated successfully!"
                    )
                }
                is Resource.Error -> {
                    _profileState.value = _profileState.value.copy(
                        isUpdating = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearMessages() {
        _profileState.value = _profileState.value.copy(error = null, successMessage = null)
    }
}