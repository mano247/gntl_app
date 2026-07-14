package com.gentlemanstore.feature.product.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.product.data.dto.CategoryResponse
import com.gentlemanstore.feature.product.data.dto.CreateProductRequest
import com.gentlemanstore.feature.product.data.dto.ProductResponse
import com.gentlemanstore.feature.product.data.dto.ProductSizeResponse
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
    val categoryOptions: List<CategoryResponse> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val error: String? = null,
    val currentPage: Int = 0,
    val isLastPage: Boolean = false,
    val isLoadingMore: Boolean = false,
    val sortOption: String = "DEFAULT",
    // ACTIVE / DELETED / ALL - server-side filter (staff prikaz obrisanih);
    // customer ekrani ostaju na default ACTIVE
    val statusFilter: String = "ACTIVE"
)

data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val product: ProductResponse? = null,
    val error: String? = null,
    val selectedSize: ProductSizeResponse? = null
)

// Stanje employee CRUD operacija nad proizvodima
data class ProductMutationUiState(
    val isSaving: Boolean = false,
    val deletingId: Long? = null,
    val restoringId: Long? = null,
    val error: String? = null,
    // Backend validacione greske po polju za create/edit formu
    val fieldErrors: Map<String, String> = emptyMap(),
    val saveSuccess: Boolean = false
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _listUiState = MutableStateFlow(ProductListUiState())
    val listUiState: StateFlow<ProductListUiState> = _listUiState.asStateFlow()

    private var loadProductsJob: kotlinx.coroutines.Job? = null

    private val _detailUiState = MutableStateFlow(ProductDetailUiState())
    val detailUiState: StateFlow<ProductDetailUiState> = _detailUiState.asStateFlow()

    private val _mutationUiState = MutableStateFlow(ProductMutationUiState())
    val mutationUiState: StateFlow<ProductMutationUiState> = _mutationUiState.asStateFlow()

    val sortedProducts: List<ProductResponse>
        get() = getSortedProducts(_listUiState.value.products, _listUiState.value.sortOption)

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

        // Cancel any in-flight request so a slower, older response (e.g. from a
        // previous search keystroke) can't overwrite newer results.
        loadProductsJob?.cancel()
        loadProductsJob = viewModelScope.launch {
            _listUiState.value = _listUiState.value.copy(isLoading = true)

            when (val result = productRepository.getProducts(
                page = 0,
                category = _listUiState.value.selectedCategory,
                search = _listUiState.value.searchQuery.ifBlank { null },
                // ACTIVE je backend default - ne salje se, pa customer zahtevi ostaju isti
                status = _listUiState.value.statusFilter.takeIf { it != "ACTIVE" }
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
        if (state.isLastPage || state.isLoadingMore || state.isLoading) return

        // Deli isti Job sa loadProducts - promena filtera/pretrage otkazuje i
        // load-more u letu, da stranica starog filtera ne bi usla u novu listu.
        loadProductsJob = viewModelScope.launch {
            _listUiState.value = state.copy(isLoadingMore = true)
            val nextPage = state.currentPage + 1

            when (val result = productRepository.getProducts(
                page = nextPage,
                category = state.selectedCategory,
                search = state.searchQuery.ifBlank { null },
                status = state.statusFilter.takeIf { it != "ACTIVE" }
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

    // ACTIVE / DELETED / ALL (employee prikaz) - server-side filter, radi zajedno
    // sa pretragom, kategorijom, sortom i paginacijom. Promena resetuje stranicu
    // i cisti stare rezultate; loadProducts otkazuje zahtev u letu (bez race-a).
    fun onStatusFilterChange(status: String) {
        if (_listUiState.value.statusFilter == status) return
        _listUiState.value = _listUiState.value.copy(statusFilter = status)
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

    fun selectSize(size: ProductSizeResponse) {
        _detailUiState.value = _detailUiState.value.copy(selectedSize = size)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = productRepository.getCategories()) {
                is Resource.Success -> {
                    _listUiState.value = _listUiState.value.copy(
                        categories = listOf("All") + result.data.map { it.name },
                        categoryOptions = result.data
                    )
                }
                else -> Unit
            }
        }
    }

    // ---------- Employee CRUD ----------

    fun createProduct(request: CreateProductRequest) {
        // Sprecava dupli submit dok je prethodni zahtev u toku
        if (_mutationUiState.value.isSaving) return
        viewModelScope.launch {
            _mutationUiState.value = _mutationUiState.value.copy(isSaving = true, error = null, fieldErrors = emptyMap())
            when (val result = productRepository.createProduct(request)) {
                is Resource.Success -> {
                    _mutationUiState.value = _mutationUiState.value.copy(isSaving = false, saveSuccess = true)
                    loadProducts(refresh = true)
                }
                is Resource.Error -> {
                    // Validacione greske ispod polja forme; unos ostaje sacuvan
                    _mutationUiState.value = _mutationUiState.value.copy(
                        isSaving = false,
                        error = if (result.fieldErrors.isEmpty()) result.message else null,
                        fieldErrors = result.fieldErrors
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun updateProduct(id: Long, request: CreateProductRequest) {
        if (_mutationUiState.value.isSaving) return
        viewModelScope.launch {
            _mutationUiState.value = _mutationUiState.value.copy(isSaving = true, error = null, fieldErrors = emptyMap())
            when (val result = productRepository.updateProduct(id, request)) {
                is Resource.Success -> {
                    _mutationUiState.value = _mutationUiState.value.copy(isSaving = false, saveSuccess = true)
                    loadProducts(refresh = true)
                }
                is Resource.Error -> {
                    _mutationUiState.value = _mutationUiState.value.copy(
                        isSaving = false,
                        error = if (result.fieldErrors.isEmpty()) result.message else null,
                        fieldErrors = result.fieldErrors
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    /**
     * Vraca soft-obrisan proizvod u katalog (backend restore - isti entitet,
     * SKU/slike/tagovi/velicine/relacije ostaju). U DELETED prikazu proizvod
     * nestaje iz liste; u ALL prikazu se osvezava kao aktivan.
     */
    fun restoreProduct(id: Long) {
        if (_mutationUiState.value.restoringId != null) return
        viewModelScope.launch {
            _mutationUiState.value = _mutationUiState.value.copy(restoringId = id, error = null)
            when (val result = productRepository.restoreProduct(id)) {
                is Resource.Success -> {
                    _mutationUiState.value = _mutationUiState.value.copy(restoringId = null)
                    _listUiState.value = _listUiState.value.copy(
                        products = if (_listUiState.value.statusFilter == "DELETED") {
                            _listUiState.value.products.filter { it.id != id }
                        } else {
                            _listUiState.value.products.map { if (it.id == id) result.data else it }
                        }
                    )
                }
                is Resource.Error -> {
                    _mutationUiState.value = _mutationUiState.value.copy(restoringId = null, error = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun deleteProduct(id: Long) {
        if (_mutationUiState.value.deletingId == id) return
        viewModelScope.launch {
            _mutationUiState.value = _mutationUiState.value.copy(deletingId = id, error = null)
            when (val result = productRepository.deleteProduct(id)) {
                is Resource.Success -> {
                    _mutationUiState.value = _mutationUiState.value.copy(deletingId = null)
                    _listUiState.value = _listUiState.value.copy(
                        products = _listUiState.value.products.filter { it.id != id }
                    )
                }
                is Resource.Error -> {
                    _mutationUiState.value = _mutationUiState.value.copy(deletingId = null, error = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearMutationError() {
        _mutationUiState.value = _mutationUiState.value.copy(error = null)
    }

    fun clearMutationFieldError(field: String) {
        _mutationUiState.value = _mutationUiState.value.copy(
            fieldErrors = _mutationUiState.value.fieldErrors - field
        )
    }

    fun resetSaveSuccess() {
        _mutationUiState.value = _mutationUiState.value.copy(saveSuccess = false)
    }

    fun onSortChange(sort: String) {
        _listUiState.value = _listUiState.value.copy(sortOption = sort)
    }

    private fun getSortedProducts(products: List<ProductResponse>, sortOption: String): List<ProductResponse> {
        return when (sortOption) {
            "PRICE_ASC" -> products.sortedBy { it.price }
            "PRICE_DESC" -> products.sortedByDescending { it.price }
            "NAME_ASC" -> products.sortedBy { it.name }
            "NAME_DESC" -> products.sortedByDescending { it.name }
            else -> products
        }
    }
}