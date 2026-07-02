package com.gentlemanstore.feature.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.notification.data.dto.NotificationResponse
import com.gentlemanstore.feature.notification.domain.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val isLoading: Boolean = false,
    val notifications: List<NotificationResponse> = emptyList(),
    val unreadCount: Int = 0,
    val error: String? = null,
    val currentPage: Int = 0,
    val isLastPage: Boolean = false,
    val isLoadingMore: Boolean = false,
    val selectedType: String? = null
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    val filteredNotifications: StateFlow<List<NotificationResponse>> = _uiState
        .map { state ->
            val sorted = state.notifications
                .sortedWith(
                    compareByDescending<NotificationResponse> { !it.isRead }
                        .thenByDescending { it.createdAt }
                )
            if (state.selectedType == null) sorted
            else sorted.filter { it.type == state.selectedType }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onTypeFilter(type: String?) {
        _uiState.value = _uiState.value.copy(selectedType = type)
    }

    init {
        loadNotifications()
        loadUnreadCount()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = notificationRepository.getNotifications(page = 0, size = 100)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        notifications = result.data.content,
                        currentPage = 0,
                        isLastPage = result.data.totalPages <= 1,
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
                    _uiState.value = _uiState.value.copy(unreadCount = result.data)
                }
                else -> Unit
            }
        }
    }

    fun markAsRead(id: Long) {
        val wasUnread = _uiState.value.notifications.any { it.id == id && !it.isRead }
        if (!wasUnread) return

        viewModelScope.launch {
            notificationRepository.markAsRead(id)
            _uiState.value = _uiState.value.copy(
                notifications = _uiState.value.notifications.map {
                    if (it.id == id) it.copy(isRead = true) else it
                },
                unreadCount = maxOf(0, _uiState.value.unreadCount - 1)
            )
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
            _uiState.value = _uiState.value.copy(
                notifications = _uiState.value.notifications.map { it.copy(isRead = true) },
                unreadCount = 0
            )
        }
    }

    fun loadMoreNotifications() {
        val state = _uiState.value
        if (state.isLastPage || state.isLoadingMore) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            val nextPage = state.currentPage + 1
            when (val result = notificationRepository.getNotifications(page = nextPage, size = 100)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        notifications = state.notifications + result.data.content,
                        currentPage = nextPage,
                        isLastPage = result.data.totalPages <= nextPage + 1
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }
}