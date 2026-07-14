package com.gentlemanstore.feature.employee.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gentlemanstore.feature.product.data.dto.CategoryResponse
import com.gentlemanstore.feature.product.data.dto.CreateProductRequest
import com.gentlemanstore.feature.product.data.dto.ProductResponse
import com.gentlemanstore.feature.product.data.dto.SizeRequest
import com.gentlemanstore.feature.product.presentation.ProductCatalogSection
import com.gentlemanstore.feature.product.presentation.ProductViewModel
import com.gentlemanstore.ui.theme.Gold500

/**
 * Employee upravljanje proizvodima: isti katalog kao customer deo
 * (ProductCatalogSection) + kompletan CRUD sa validacijom i potvrdom brisanja.
 */
@Composable
fun EmployeeProductsTab(
    onProductClick: (Long) -> Unit,
    onShowError: (String) -> Unit,
    viewModel: ProductViewModel
) {
    val listState by viewModel.listUiState.collectAsStateWithLifecycle()
    val mutationState by viewModel.mutationUiState.collectAsStateWithLifecycle()

    var showForm by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductResponse?>(null) }
    var deleteTarget by remember { mutableStateOf<ProductResponse?>(null) }

    LaunchedEffect(mutationState.error) {
        mutationState.error?.let {
            onShowError(it)
            viewModel.clearMutationError()
        }
    }
    LaunchedEffect(mutationState.saveSuccess) {
        if (mutationState.saveSuccess) {
            showForm = false
            editingProduct = null
            viewModel.resetSaveSuccess()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = {
                if (showForm) {
                    showForm = false
                    editingProduct = null
                } else {
                    editingProduct = null
                    showForm = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold500),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (showForm) "Cancel" else "+ Create Product", color = MaterialTheme.colorScheme.background)
        }

        if (showForm) {
            EmployeeProductForm(
                initial = editingProduct,
                categories = listState.categoryOptions,
                isSaving = mutationState.isSaving,
                onSubmit = { request ->
                    val target = editingProduct
                    if (target == null) viewModel.createProduct(request)
                    else viewModel.updateProduct(target.id, request)
                }
            )
        } else {
            ProductCatalogSection(
                viewModel = viewModel,
                onProductClick = onProductClick,
                productActions = { product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editingProduct = product; showForm = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Gold500, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", color = Gold500, style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = { deleteTarget = product },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            enabled = mutationState.deletingId != product.id
                        ) {
                            if (mutationState.deletingId == product.id) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                            } else {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            )
        }
    }

    deleteTarget?.let { product ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete \"${product.name}\"? This action cannot be undone from the app.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProduct(product.id)
                    deleteTarget = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = Gold500)
                }
            }
        )
    }
}

@Composable
private fun EmployeeProductForm(
    initial: ProductResponse?,
    categories: List<CategoryResponse>,
    isSaving: Boolean,
    onSubmit: (CreateProductRequest) -> Unit
) {
    val isEdit = initial != null

    var sku by remember(initial) { mutableStateOf(initial?.sku ?: "") }
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
    var price by remember(initial) { mutableStateOf(initial?.price?.toString() ?: "") }
    var selectedCategoryId by remember(initial, categories) {
        mutableStateOf(categories.find { it.name == initial?.categoryName }?.id)
    }
    var showCategoryMenu by remember { mutableStateOf(false) }
    val sizes = remember(initial) {
        mutableStateListOf<Pair<String, String>>().apply {
            initial?.sizes?.forEach { add(it.size to it.quantity.toString()) }
        }
    }
    val imageUrls = remember(initial) {
        mutableStateListOf<String>().apply {
            initial?.imageUrls?.forEach { add(it) }
        }
    }
    var tags by remember(initial) { mutableStateOf(initial?.tags?.joinToString(", ") ?: "") }

    val priceValue = price.toDoubleOrNull()
    val priceError = price.isNotBlank() && (priceValue == null || priceValue <= 0.0)
    val sizesValid = sizes.all { it.first.isNotBlank() && (it.second.toIntOrNull() ?: -1) >= 0 }
    val formValid = sku.isNotBlank() && name.isNotBlank() && description.isNotBlank()
            && priceValue != null && priceValue > 0.0
            && selectedCategoryId != null && sizesValid

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isEdit) "Edit Product" else "New Product",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold500
                    )

                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isEdit,
                        supportingText = if (isEdit) {
                            { Text("SKU cannot be changed") }
                        } else null
                    )
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price (RSD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = priceError,
                        supportingText = if (priceError) {
                            { Text("Enter a price greater than 0") }
                        } else null
                    )

                    Box {
                        OutlinedButton(onClick = { showCategoryMenu = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = categories.find { it.id == selectedCategoryId }?.name ?: "Select Category",
                                color = Gold500
                            )
                        }
                        DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = { selectedCategoryId = category.id; showCategoryMenu = false }
                                )
                            }
                        }
                    }

                    // Velicine i stanje zaliha
                    Text(text = "Sizes & stock", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    sizes.forEachIndexed { index, (sizeLabel, quantity) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = sizeLabel,
                                onValueChange = { sizes[index] = it to quantity },
                                label = { Text("Size") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { sizes[index] = sizeLabel to it },
                                label = { Text("Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                isError = quantity.isNotBlank() && (quantity.toIntOrNull() ?: -1) < 0
                            )
                            IconButton(onClick = { sizes.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove size", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { sizes.add("" to "0") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Gold500, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add size", color = Gold500)
                    }

                    // Slike (URL-ovi, kao i u postojecem create toku)
                    Text(text = "Image URLs", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    imageUrls.forEachIndexed { index, url ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = url,
                                onValueChange = { imageUrls[index] = it },
                                label = { Text("Image URL") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(onClick = { imageUrls.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove image", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { imageUrls.add("") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Gold500, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add image", color = Gold500)
                    }

                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Tags (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val request = CreateProductRequest(
                                sku = sku.trim(),
                                name = name.trim(),
                                description = description.trim(),
                                price = priceValue ?: return@Button,
                                categoryId = selectedCategoryId ?: return@Button,
                                sizes = sizes
                                    .filter { it.first.isNotBlank() }
                                    .map { SizeRequest(it.first.trim(), it.second.toIntOrNull() ?: 0) },
                                imageUrls = imageUrls.map { it.trim() }.filter { it.isNotBlank() },
                                tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            )
                            onSubmit(request)
                        },
                        enabled = !isSaving && formValid,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold500),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text(if (isEdit) "Save Changes" else "Create", color = MaterialTheme.colorScheme.background)
                    }
                }
            }
        }
    }
}
