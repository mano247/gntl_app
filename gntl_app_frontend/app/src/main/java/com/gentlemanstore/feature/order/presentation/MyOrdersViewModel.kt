package com.gentlemanstore.feature.order.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.order.data.dto.OrderResponse
import com.gentlemanstore.feature.order.domain.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyOrdersUiState(
    val isLoading: Boolean = false,
    val orders: List<OrderResponse> = emptyList(),
    val error: String? = null,
    val currentPage: Int = 0,
    val isLastPage: Boolean = false,
    val isLoadingMore: Boolean = false,
    val selectedStatus: String? = "PENDING"
)

@HiltViewModel
class MyOrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyOrdersUiState())
    val uiState: StateFlow<MyOrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    // Otkazivanjem prethodnog učitavanja sprečavamo da stariji odgovor
    // (npr. za prethodno izabrani status) prepiše rezultat novijeg zahteva.
    private var loadJob: Job? = null

    fun loadOrders() {
        loadJob?.cancel()
        // isLoading se postavlja sinhrono (pre launch-a) da bi guard u
        // loadMoreOrders važio i kad dispatcher nije Main.immediate (testovi).
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadJob = viewModelScope.launch {
            when (val result = orderRepository.getMyOrders(page = 0, status = _uiState.value.selectedStatus)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        orders = result.data.content,
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

    fun loadMoreOrders() {
        val state = _uiState.value
        // error != null blokira automatski retry sa UI trigger-a — bez toga
        // neuspešan load-more ulazi u beskonačnu request petlju (spinner item
        // menja veličinu liste, što ponovo okida load-more). Error se čisti
        // kroz refresh/promenu statusa.
        if (state.isLastPage || state.isLoadingMore || state.isLoading ||
            state.orders.isEmpty() || state.error != null
        ) return

        // Sinhroni guard: sprečava dva paralelna zahteva za istu stranicu
        // i kad se loadMoreOrders pozove više puta pre starta korutine.
        _uiState.value = state.copy(isLoadingMore = true)
        loadJob = viewModelScope.launch {
            val nextPage = state.currentPage + 1

            when (val result = orderRepository.getMyOrders(page = nextPage, status = state.selectedStatus)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        orders = state.orders + result.data.content,
                        currentPage = nextPage,
                        isLastPage = result.data.last,
                        error = null
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

    // Server već filtrira po statusu; klijentski filter je samo zaštita
    // ako neka porudžbina promeni status dok je lista učitana.
    val filteredOrders: List<OrderResponse>
        get() = if (_uiState.value.selectedStatus == null) {
            _uiState.value.orders
        } else {
            _uiState.value.orders.filter { it.status == _uiState.value.selectedStatus }
        }

    fun onStatusFilter(status: String?) {
        if (_uiState.value.selectedStatus == status) return
        // Reset paginacije i liste — status se šalje serveru, pa se lista učitava ispočetka.
        _uiState.value = _uiState.value.copy(
            selectedStatus = status,
            orders = emptyList(),
            currentPage = 0,
            isLastPage = false,
            isLoadingMore = false,
            error = null
        )
        loadOrders()
    }
}