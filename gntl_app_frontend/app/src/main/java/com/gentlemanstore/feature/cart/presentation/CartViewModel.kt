package com.gentlemanstore.feature.cart.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.cart.data.dto.CartResponse
import com.gentlemanstore.feature.cart.domain.CartRepository
import com.gentlemanstore.feature.order.data.dto.OrderResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartUiState(
    val isLoading: Boolean = false,
    val cart: CartResponse? = null,
    val error: String? = null,
    val checkoutSuccess: Boolean = false,
    val isCheckingOut: Boolean = false,
    val removingItemId: Long? = null,
    val completedOrder: OrderResponse? = null,
    val promoCode: String = "",
    val promoDiscount: Double? = null,
    val promoError: String? = null,
    val isValidatingPromo: Boolean = false
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        loadCart()
    }

    fun loadCart() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = cartRepository.getCart()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        cart = result.data,
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

    fun addToCart(productId: Long, productSizeId: Long, quantity: Int = 1) {
        viewModelScope.launch {
            when (val result = cartRepository.addToCart(productId, productSizeId, quantity)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(cart = result.data)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun removeFromCart(cartItemId: Long) {
        if (_uiState.value.removingItemId == cartItemId) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(removingItemId = cartItemId)

            when (val result = cartRepository.removeFromCart(cartItemId)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        cart = result.data,
                        removingItemId = null
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        removingItemId = null
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun onPromoCodeChange(code: String) {
        _uiState.value = _uiState.value.copy(promoCode = code, promoError = null, promoDiscount = null)
    }

    fun validatePromoCode() {
        val code = _uiState.value.promoCode.trim()
        if (code.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isValidatingPromo = true, promoError = null)
            when (val result = cartRepository.validatePromoCode(code)) {
                is Resource.Success -> {
                    val discount = result.data
                    val cart = _uiState.value.cart
                    val basePrice = cart?.finalPrice ?: cart?.totalPrice
                    val discountAmount = if (discount.discountType == "PERCENTAGE") {
                        basePrice?.toDouble()?.times(discount.value.toDouble() / 100)
                    } else {
                        discount.value.toDouble()
                    }
                    _uiState.value = _uiState.value.copy(
                        isValidatingPromo = false,
                        promoDiscount = discountAmount
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isValidatingPromo = false,
                        promoError = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun checkout(addressId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingOut = true)
            val promoCode = _uiState.value.promoCode.ifBlank { null }
            when (val result = cartRepository.checkout(addressId, promoCode)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCheckingOut = false,
                        checkoutSuccess = true,
                        completedOrder = result.data,
                        cart = null,
                        promoCode = "",
                        promoDiscount = null,
                        promoError = null
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isCheckingOut = false,
                        error = result.message
                    )
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun resetCheckoutState() {
        _uiState.value = _uiState.value.copy(checkoutSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}