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
                    _uiState.value = AuthUiState(
                        error = mapAuthError(result.message)
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
                        error = mapAuthError(result.message)
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun mapAuthError(message: String?): String {
        return when {
            message == null -> "Something went wrong. Please try again."
            message.contains("401") || message.contains("Unauthorized") || message.contains("Bad credentials") -> "Invalid email or password."
            message.contains("403") || message.contains("Forbidden") -> "Access denied."
            message.contains("404") -> "Account not found."
            message.contains("409") || message.contains("already exists") || message.contains("already taken") -> "An account with this email already exists."
            message.contains("400") || message.contains("Bad Request") -> "Invalid input. Please check your details."
            message.contains("500") || message.contains("Internal Server") -> "Server error. Please try again later."
            message.contains("timeout") || message.contains("Unable to resolve") || message.contains("failed to connect") -> "No internet connection. Please check your network."
            else -> "Something went wrong. Please try again."
        }
    }

    fun clearError(){
        _uiState.value = _uiState.value.copy(error = null)
    }
}