package com.gentlemanstore.feature.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.product.data.dto.ProductResponse
import com.gentlemanstore.feature.product.domain.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SwipeUiState(
    val isLoading: Boolean = false,
    val products: List<ProductResponse> = emptyList(),
    val error: String? = null,
    val currentPage: Int = 0,
    val isLastPage: Boolean = false,
    val addedToCart: Boolean = false,
    val addedProductName: String? = null
)

@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SwipeUiState())
    val uiState: StateFlow<SwipeUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = productRepository.getProducts(page = 0, size = 20)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        products = result.data.content,
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

    fun loadMoreIfNeeded(currentIndex: Int) {
        val state = _uiState.value
        if (currentIndex >= state.products.size - 3 && !state.isLastPage) {
            viewModelScope.launch {
                val nextPage = state.currentPage + 1
                when (val result = productRepository.getProducts(page = nextPage, size = 20)) {
                    is Resource.Success -> {
                        _uiState.value = state.copy(
                            products = state.products + result.data.content,
                            currentPage = nextPage,
                            isLastPage = result.data.last
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onAddToCart(product: ProductResponse) {
        _uiState.value = _uiState.value.copy(
            addedToCart = true,
            addedProductName = product.name
        )
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _uiState.value = _uiState.value.copy(
                addedToCart = false,
                addedProductName = null
            )
        }
    }

    fun onSkip() {
        //
    }
}