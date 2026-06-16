package com.gentlemanstore.feature.product.presentation

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

data class ProductListUiState(
    val isLoading: Boolean = false,
    val products: List<ProductResponse> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val error: String? = null,
    val currentPage: Int = 0,
    val isLastPage: Boolean = false,
    val isLoadingMore: Boolean = false
)

data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val product: ProductResponse? = null,
    val error: String? = null,
    val selectedSize: String? = null
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _listUiState = MutableStateFlow(ProductListUiState())
    val listUiState: StateFlow<ProductListUiState> = _listUiState.asStateFlow()

    private val _detailUiState = MutableStateFlow(ProductDetailUiState())
    val detailUiState: StateFlow<ProductDetailUiState> = _detailUiState.asStateFlow()

    init {
        loadProducts()
        loadCategories()
    }

    fun loadProducts(refresh: Boolean = false){
        if (refresh){
            _listUiState.value = _listUiState.value.copy(
                currentPage = 0,
                products = emptyList(),
                isLastPage = false
            )
        }

        viewModelScope.launch {
            _listUiState.value = _listUiState.value.copy(isLoading = true)

            when (val result = productRepository.getProducts(
                page = 0,
                category = _listUiState.value.selectedCategory,
                search = _listUiState.value.searchQuery.ifBlank { null }
            )) {
                is Resource.Success -> {
                    _listUiState.value = _listUiState.value.copy(
                        isLoading = false,
                        products = result.data.content,
                        currentPage = 0,
                        isLastPage = result.data.last,
                        error = null
                    )
                }
                is Resource.Error -> {
                    _listUiState.value = _listUiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun loadMoreProducts() {
        val state = _listUiState.value
        if (state.isLastPage || state.isLoadingMore) return

        viewModelScope.launch {
            _listUiState.value = state.copy(isLoadingMore = true)
            val nextPage = state.currentPage + 1

            when (val result = productRepository.getProducts(
                page = nextPage,
                category = state.selectedCategory,
                search = state.searchQuery.ifBlank { null }
            )) {
                is Resource.Success -> {
                    _listUiState.value = _listUiState.value.copy(
                        isLoadingMore = false,
                        products = state.products + result.data.content,
                        currentPage = nextPage,
                        isLastPage = result.data.last
                    )
                }
                is Resource.Error -> {
                    _listUiState.value = _listUiState.value.copy(
                        isLoadingMore = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun selectCategory(category: String?){
        _listUiState.value = _listUiState.value.copy(
            selectedCategory = category
        )
        loadProducts(refresh = true)
    }

    fun onSearchQueryChange(query: String){
        _listUiState.value = _listUiState.value.copy(searchQuery = query)
        loadProducts(refresh = true)
    }

    fun loadProductById(id: Long) {
        viewModelScope.launch {
            _detailUiState.value = ProductDetailUiState(isLoading = true)

            when (val result = productRepository.getProductById(id)) {
                is Resource.Success -> {
                    _detailUiState.value = ProductDetailUiState(
                        product = result.data,
                        selectedSize = result.data.sizes.firstOrNull()
                    )
                }
                is Resource.Error -> {
                    _detailUiState.value = ProductDetailUiState(
                        error = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun selectSize(size: String) {
        _detailUiState.value = _detailUiState.value.copy(selectedSize = size)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = productRepository.getCategories()) {
                is Resource.Success -> {
                    _listUiState.value = _listUiState.value.copy(
                        categories = listOf("All") + result.data
                    )
                }
                else -> Unit
            }
        }
    }
}