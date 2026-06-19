package com.gentlemanstore.feature.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.admin.data.dto.*
import com.gentlemanstore.feature.admin.domain.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val isLoading: Boolean = false,
    val users: List<UserListResponse> = emptyList(),
    val error: String? = null,
    val isLastPage: Boolean = false,
    val currentPage: Int = 0,
    val updatingUserId: Long? = null,
    val deletingUserId: Long? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = adminRepository.getAllUsers(0)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        users = result.data.content,
                        currentPage = 0,
                        isLastPage = result.data.last,
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

    fun changeUserRole(userId: Long, role: String) {
        if (_uiState.value.updatingUserId == userId) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updatingUserId = userId)
            when (val result = adminRepository.changeUserRole(userId, role)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        users = _uiState.value.users.map {
                            if (it.id == userId) result.data else it
                        },
                        updatingUserId = null
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        updatingUserId = null
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun deleteUser(userId: Long) {
        if (_uiState.value.deletingUserId == userId) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingUserId = userId)
            when (val result = adminRepository.deleteUser(userId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        users = _uiState.value.users.filter { it.id != userId },
                        deletingUserId = null
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        deletingUserId = null
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}