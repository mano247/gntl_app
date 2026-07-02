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
    val discounts: List<DiscountResponse> = emptyList(),
    val promotions: List<PromotionResponse> = emptyList(),
    val categories: List<Pair<Long, String>> = emptyList(),
    val error: String? = null,
    val isCreatingDiscount: Boolean = false,
    val isCreatingPromotion: Boolean = false,
    val deletingDiscountId: Long? = null,
    // Loyalty
    val loyaltyUserId: String = "",
    val loyaltyAccount: LoyaltyAccountResponse? = null,
    val isLoadingLoyalty: Boolean = false,
    val isAddingPoints: Boolean = false,
    val loyaltyError: String? = null,
    val loyaltySuccess: String? = null
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
        loadDiscounts()
        loadPromotions()
        loadCategories()
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
            when (val result = managerRepository.getActivePromotions()) {
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
        code: String,
        discountType: String,
        value: java.math.BigDecimal,
        validFrom: String,
        validTo: String,
        productId: Long? = null,
        categoryId: Long? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingDiscount = true)
            val request = CreateDiscountRequest(
                code = code,
                discountType = discountType,
                value = value,
                validFrom = validFrom,
                validTo = validTo,
                productId = productId,
                categoryId = categoryId
            )
            when (val result = managerRepository.createDiscount(request)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCreatingDiscount = false,
                        discounts = _uiState.value.discounts + result.data
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isCreatingDiscount = false,
                        error = result.message
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
        validFrom: String,
        validTo: String,
        discountId: Long
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingPromotion = true)
            val request = CreatePromotionRequest(name, description, validFrom, validTo, discountId)
            when (val result = managerRepository.createPromotion(request)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCreatingPromotion = false,
                        promotions = _uiState.value.promotions + result.data
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isCreatingPromotion = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun onLoyaltyUserIdChange(id: String) {
        _uiState.value = _uiState.value.copy(loyaltyUserId = id, loyaltyAccount = null, loyaltyError = null)
    }

    fun loadUserLoyaltyAccount() {
        val userId = _uiState.value.loyaltyUserId.toLongOrNull() ?: run {
            _uiState.value = _uiState.value.copy(loyaltyError = "Invalid user ID")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLoyalty = true, loyaltyError = null)
            when (val result = managerRepository.getUserLoyaltyAccount(userId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingLoyalty = false,
                        loyaltyAccount = result.data
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingLoyalty = false,
                        loyaltyError = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun addPointsToUser(points: Int, description: String) {
        // Points must go to the user the manager looked up, not to the manager's own account.
        val targetUserId = _uiState.value.loyaltyUserId.toLongOrNull() ?: run {
            _uiState.value = _uiState.value.copy(loyaltyError = "Invalid user ID")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingPoints = true, loyaltyError = null, loyaltySuccess = null)
            when (val result = managerRepository.addPoints(points, description, targetUserId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isAddingPoints = false,
                        loyaltyAccount = result.data,
                        loyaltySuccess = "Points added successfully!"
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isAddingPoints = false,
                        loyaltyError = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearLoyaltyMessages() {
        _uiState.value = _uiState.value.copy(loyaltyError = null, loyaltySuccess = null)
    }
}