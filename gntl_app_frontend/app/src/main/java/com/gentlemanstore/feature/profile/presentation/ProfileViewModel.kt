package com.gentlemanstore.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.data.datastore.TokenDataStore
import com.gentlemanstore.feature.auth.domain.AuthRepository
import com.gentlemanstore.feature.notification.domain.NotificationRepository
import com.gentlemanstore.feature.profile.data.dto.UserResponse
import com.gentlemanstore.feature.profile.domain.UserRepository
import com.gentlemanstore.feature.support.domain.SupportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: UserResponse? = null,
    val error: String? = null,
    val logoutComplete: Boolean = false,
    val unreadNotificationCount: Int = 0,
    val unreadSupportCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val tokenDataStore: TokenDataStore,
    private val notificationRepository: NotificationRepository,
    private val supportRepository: SupportRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadUnreadCount()
        loadUnreadSupportCount()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = userRepository.getMyProfile()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = result.data,
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

    fun loadUnreadCount() {
        viewModelScope.launch {
            when (val result = notificationRepository.getUnreadCount()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(unreadNotificationCount = result.data)
                }
                else -> Unit
            }
        }
    }

    fun loadUnreadSupportCount() {
        viewModelScope.launch {
            when (val result = supportRepository.getTotalUnreadCount()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(unreadSupportCount = result.data)
                }
                else -> Unit
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Revokes the refresh token server-side and clears local storage.
            authRepository.logout()
            _uiState.value = _uiState.value.copy(logoutComplete = true)
        }
    }
}