package com.gentlemanstore.feature.loyalty.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.loyalty.data.dto.LoyaltyAccountResponse
import com.gentlemanstore.feature.loyalty.domain.LoyaltyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoyaltyUiState(
    val isLoading: Boolean = false,
    val account: LoyaltyAccountResponse? = null,
    val error: String? = null
)

@HiltViewModel
class LoyaltyViewModel @Inject constructor(
    private val loyaltyRepository: LoyaltyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoyaltyUiState())
    val uiState: StateFlow<LoyaltyUiState> = _uiState.asStateFlow()

    init {
        loadAccount()
    }

    fun loadAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = loyaltyRepository.getLoyaltyAccount()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        account = result.data,
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
}