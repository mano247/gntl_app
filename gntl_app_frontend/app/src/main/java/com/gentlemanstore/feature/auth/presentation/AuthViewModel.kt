package com.gentlemanstore.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val userRole: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)

            when (val result = authRepository.login(email, password)) {
                is Resource.Success -> {
                    _uiState.value = AuthUiState(
                        isSuccess = true,
                        userRole = result.data.role
                    )
                }
                is Resource.Error -> {
                    // Konkretna backend poruka (npr. "Invalid email or password")
                    // ili validacione greske po polju - bez generickog teksta.
                    _uiState.value = AuthUiState(
                        error = if (result.fieldErrors.isEmpty()) result.message else null,
                        fieldErrors = result.fieldErrors
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        phone: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)

            when (val result = authRepository.register(
                firstName, lastName, email, password, phone
            )) {
                is Resource.Success -> {
                    _uiState.value = AuthUiState(
                        isSuccess = true,
                        userRole = result.data.role
                    )
                }
                is Resource.Error -> {
                    _uiState.value = AuthUiState(
                        error = if (result.fieldErrors.isEmpty()) result.message else null,
                        fieldErrors = result.fieldErrors
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearError(){
        _uiState.value = _uiState.value.copy(error = null, fieldErrors = emptyMap())
    }
}