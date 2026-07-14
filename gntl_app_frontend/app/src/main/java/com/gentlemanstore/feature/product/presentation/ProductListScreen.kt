package com.gentlemanstore.feature.product.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gentlemanstore.ui.theme.Gold500

@Composable
fun ProductListScreen(
    onProductClick: (Long) -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "GENTLEMAN",
                    style = MaterialTheme.typography.titleLarge,
                    color = Gold500
                )
                Text(
                    text = "STORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 3.sp
                )
            }
        }

        ProductCatalogSection(
            viewModel = viewModel,
            onProductClick = onProductClick
        )
    }
}
