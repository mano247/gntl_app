package com.gentlemanstore.feature.manager.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.manager.data.dto.*
import com.gentlemanstore.feature.manager.domain.ManagerRepository
import com.gentlemanstore.feature.product.domain.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManagerUiState(
    val isLoading: Boolean = false,
    val analytics: AnalyticsResponse? = null,
    // Istorija mesečnih izveštaja (najnoviji prvi)
    val monthlyReports: List<MonthlyReportResponse> = emptyList(),
    val isLoadingReports: Boolean = false,
    val reportsError: String? = null,
    val discounts: List<DiscountResponse> = emptyList(),
    val promotions: List<PromotionResponse> = emptyList(),
    val categories: List<Pair<Long, String>> = emptyList(),
    val error: String? = null,
    val isCreatingDiscount: Boolean = false,
    val isCreatingPromotion: Boolean = false,
    val deletingDiscountId: Long? = null,
    val deletingPromotionId: Long? = null,
    // Backend validacione greske po polju za create forme
    val discountFieldErrors: Map<String, String> = emptyMap(),
    val promotionFieldErrors: Map<String, String> = emptyMap(),
    // One-shot signali uspeha - forma se zatvara/resetuje tek na uspeh,
    // na gresku unos ostaje sacuvan (isti obrazac kao ProductViewModel.saveSuccess)
    val discountCreated: Boolean = false,
    val promotionCreated: Boolean = false
)

@HiltViewModel
class ManagerViewModel @Inject constructor(
    private val managerRepository: ManagerRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManagerUiState())
    val uiState: StateFlow<ManagerUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        loadMonthlyReports()
        loadDiscounts()
        loadPromotions()
        loadCategories()
    }

    fun loadMonthlyReports() {
        if (_uiState.value.isLoadingReports) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingReports = true, reportsError = null)
            when (val result = managerRepository.getMonthlyReports()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingReports = false,
                        monthlyReports = result.data
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingReports = false,
                        reportsError = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            when (val result = productRepository.getCategories()) {
                is Resource.Success -> {
                    val categoryPairs = result.data.map { it.id to it.name }
                    _uiState.value = _uiState.value.copy(categories = categoryPairs)
                }
                is Resource.Error -> Unit
                is Resource.Loading -> Unit
            }
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            when (val result = managerRepository.getDashboard()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(analytics = result.data)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun loadDiscounts() {
        viewModelScope.launch {
            when (val result = managerRepository.getAllDiscounts()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(discounts = result.data)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun loadPromotions() {
        viewModelScope.launch {
            when (val result = managerRepository.getAllPromotions()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(promotions = result.data)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun createDiscount(
        discountType: String,
        value: java.math.BigDecimal,
        validFrom: String,
        validTo: String,
        scope: String,
        categoryId: Long? = null
    ) {
        // Sprecava dupli submit dok je prethodni zahtev u toku
        if (_uiState.value.isCreatingDiscount) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingDiscount = true, discountFieldErrors = emptyMap(), error = null)
            val request = CreateDiscountRequest(
                discountType = discountType,
                value = value,
                validFrom = validFrom,
                validTo = validTo,
                scope = scope,
                categoryId = categoryId
            )
            when (val result = managerRepository.createDiscount(request)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCreatingDiscount = false,
                        discounts = _uiState.value.discounts + result.data,
                        discountCreated = true
                    )
                }
                is Resource.Error -> {
                    // Validacione greske ispod polja forme, ostalo kao opsta poruka
                    _uiState.value = _uiState.value.copy(
                        isCreatingDiscount = false,
                        error = if (result.fieldErrors.isEmpty()) result.message else null,
                        discountFieldErrors = result.fieldErrors
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun deleteDiscount(id: Long) {
        if (_uiState.value.deletingDiscountId == id) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingDiscountId = id)
            when (val result = managerRepository.deleteDiscount(id)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        discounts = _uiState.value.discounts.filter { it.id != id },
                        deletingDiscountId = null
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        deletingDiscountId = null
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun createPromotion(
        name: String,
        description: String,
        code: String?,
        discountType: String,
        value: java.math.BigDecimal,
        validFrom: String,
        validTo: String
    ) {
        // Sprecava dupli submit dok je prethodni zahtev u toku
        if (_uiState.value.isCreatingPromotion) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingPromotion = true, promotionFieldErrors = emptyMap(), error = null)
            val request = CreatePromotionRequest(
                name = name,
                description = description,
                code = code?.trim()?.takeIf { it.isNotBlank() },
                discountType = discountType,
                value = value,
                validFrom = validFrom,
                validTo = validTo
            )
            when (val result = managerRepository.createPromotion(request)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCreatingPromotion = false,
                        promotions = _uiState.value.promotions + result.data,
                        promotionCreated = true
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isCreatingPromotion = false,
                        error = if (result.fieldErrors.isEmpty()) result.message else null,
                        promotionFieldErrors = result.fieldErrors
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun deletePromotion(id: Long) {
        if (_uiState.value.deletingPromotionId == id) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingPromotionId = id)
            when (val result = managerRepository.deletePromotion(id)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        promotions = _uiState.value.promotions.filter { it.id != id },
                        deletingPromotionId = null
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        deletingPromotionId = null
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun consumeDiscountCreated() {
        _uiState.value = _uiState.value.copy(discountCreated = false)
    }

    fun consumePromotionCreated() {
        _uiState.value = _uiState.value.copy(promotionCreated = false)
    }
}