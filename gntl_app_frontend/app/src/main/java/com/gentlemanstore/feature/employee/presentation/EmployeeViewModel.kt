package com.gentlemanstore.feature.employee.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.employee.domain.EmployeeRepository
import com.gentlemanstore.feature.order.data.dto.OrderResponse
import com.gentlemanstore.feature.support.data.dto.SupportTicketResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmployeeOrdersUiState(
    val isLoading: Boolean = false,
    val orders: List<OrderResponse> = emptyList(),
    val error: String? = null,
    val isLastPage: Boolean = false,
    val currentPage: Int = 0,
    val updatingOrderId: Long? = null,
    val selectedStatus: String? = null,
    val isLoadingMore: Boolean = false
)

data class EmployeeTicketsUiState(
    val isLoading: Boolean = false,
    val tickets: List<SupportTicketResponse> = emptyList(),
    val error: String? = null,
    val isLastPage: Boolean = false,
    val currentPage: Int = 0,
    val updatingTicketId: Long? = null,
    val selectedStatus: String? = null
)

@HiltViewModel
class EmployeeViewModel @Inject constructor(
    private val employeeRepository: EmployeeRepository
) : ViewModel() {

    private val _ordersUiState = MutableStateFlow(EmployeeOrdersUiState())
    val ordersUiState: StateFlow<EmployeeOrdersUiState> = _ordersUiState.asStateFlow()

    private val _ticketsUiState = MutableStateFlow(EmployeeTicketsUiState())
    val ticketsUiState: StateFlow<EmployeeTicketsUiState> = _ticketsUiState.asStateFlow()

    init {
        loadOrders()
        loadTickets()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _ordersUiState.value = _ordersUiState.value.copy(isLoading = true)
            when (val result = employeeRepository.getAllOrders(0)) {
                is Resource.Success -> {
                    _ordersUiState.value = _ordersUiState.value.copy(
                        isLoading = false,
                        orders = result.data.content,
                        currentPage = 0,
                        isLastPage = result.data.last,
                        error = null
                    )
                }
                is Resource.Error -> {
                    _ordersUiState.value = _ordersUiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun loadMoreOrders() {
        val state = _ordersUiState.value
        if (state.isLastPage || state.isLoadingMore) return

        viewModelScope.launch {
            _ordersUiState.value = state.copy(isLoadingMore = true)
            val nextPage = state.currentPage + 1

            when (val result = employeeRepository.getAllOrders(nextPage)) {
                is Resource.Success -> {
                    _ordersUiState.value = _ordersUiState.value.copy(
                        isLoadingMore = false,
                        orders = state.orders + result.data.content,
                        currentPage = nextPage,
                        isLastPage = result.data.last
                    )
                }
                is Resource.Error -> {
                    _ordersUiState.value = _ordersUiState.value.copy(
                        isLoadingMore = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun updateOrderStatus(orderId: Long, status: String) {
        if (_ordersUiState.value.updatingOrderId == orderId) return
        viewModelScope.launch {
            _ordersUiState.value = _ordersUiState.value.copy(updatingOrderId = orderId)
            when (val result = employeeRepository.updateOrderStatus(orderId, status)) {
                is Resource.Success -> {
                    _ordersUiState.value = _ordersUiState.value.copy(
                        orders = _ordersUiState.value.orders.map {
                            if (it.id == orderId) result.data else it
                        },
                        updatingOrderId = null
                    )
                }
                is Resource.Error -> {
                    _ordersUiState.value = _ordersUiState.value.copy(
                        error = result.message,
                        updatingOrderId = null
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun loadTickets() {
        viewModelScope.launch {
            _ticketsUiState.value = _ticketsUiState.value.copy(isLoading = true)
            when (val result = employeeRepository.getAllTickets(0)) {
                is Resource.Success -> {
                    _ticketsUiState.value = _ticketsUiState.value.copy(
                        isLoading = false,
                        tickets = result.data.content,
                        currentPage = 0,
                        isLastPage = result.data.last,
                        error = null
                    )
                }
                is Resource.Error -> {
                    _ticketsUiState.value = _ticketsUiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun updateTicketStatus(ticketId: Long, status: String) {
        if (_ticketsUiState.value.updatingTicketId == ticketId) return
        viewModelScope.launch {
            _ticketsUiState.value = _ticketsUiState.value.copy(updatingTicketId = ticketId)
            when (val result = employeeRepository.updateTicketStatus(ticketId, status)) {
                is Resource.Success -> {
                    val updatedTickets = if (status == "CLOSED") {
                        _ticketsUiState.value.tickets.filter { it.id != ticketId }
                    } else {
                        _ticketsUiState.value.tickets.map {
                            if (it.id == ticketId) result.data else it
                        }
                    }
                    _ticketsUiState.value = _ticketsUiState.value.copy(
                        tickets = updatedTickets,
                        updatingTicketId = null
                    )
                }
                is Resource.Error -> {
                    _ticketsUiState.value = _ticketsUiState.value.copy(
                        error = result.message,
                        updatingTicketId = null
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    val filteredTickets: List<SupportTicketResponse>
        get() = if (_ticketsUiState.value.selectedStatus == null) {
            _ticketsUiState.value.tickets
        } else {
            _ticketsUiState.value.tickets.filter { it.status == _ticketsUiState.value.selectedStatus }
        }

    fun onTicketStatusFilter(status: String?) {
        _ticketsUiState.value = _ticketsUiState.value.copy(selectedStatus = status)
    }

    val filteredOrders: List<OrderResponse>
        get() = if (_ordersUiState.value.selectedStatus == null) {
            _ordersUiState.value.orders
        } else {
            _ordersUiState.value.orders.filter { it.status == _ordersUiState.value.selectedStatus }
        }

    fun onOrderStatusFilter(status: String?) {
        _ordersUiState.value = _ordersUiState.value.copy(selectedStatus = status)
    }
}