package com.gentlemanstore.feature.product.data.dto

data class ProductSizeResponse(
    val id: Long,
    val size: String,
    val quantity: Int
)

data class ProductResponse(
    val id: Long,
    val sku: String,
    val name: String,
    val description: String,
    val price: Double,
    val categoryName: String,
    val sizes: List<ProductSizeResponse>,
    val imageUrls: List<String>,
    val tags: List<String>,
    val discountPercentage: Double?,
    // Staff DELETED/ALL prikaz - obrisan proizvod se oznacava i dobija Restore
    val deleted: Boolean = false
)

data class PagedProductResponse(
    val content: List<ProductResponse>,
    val totalPages: Int,
    val totalElements: Long,
    val last: Boolean,
    val first: Boolean,
    val number: Int,
    val size: Int,
    val numberOfElements: Int
)

data class CategoryResponse(
    val id: Long,
    val name: String,
    val description: String?
)

// Employee CRUD — odgovara backend CreateProductRequest (create i update)
data class SizeRequest(
    val size: String,
    val quantity: Int
)

data class CreateProductRequest(
    val sku: String,
    val name: String,
    val description: String,
    val price: Double,
    val categoryId: Long,
    val sizes: List<SizeRequest>,
    val imageUrls: List<String>,
    val tags: List<String>
)