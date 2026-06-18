package com.gentlemanstore.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlemanstore.data.datastore.TokenDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    val currency = tokenDataStore.currency.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Constants.CURRENCY_RSD
    )

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            tokenDataStore.saveCurrency(currency)
        }
    }
}

@Composable
fun rememberCurrentCurrency(viewModel: CurrencyViewModel = hiltViewModel()): String {
    val currency by viewModel.currency.collectAsState()
    return currency
}

@Composable
fun formatPrice(amountRsd: Double, viewModel: CurrencyViewModel = hiltViewModel()): String {
    val currency = rememberCurrentCurrency(viewModel)
    return CurrencyFormatter.format(amountRsd, currency)
}