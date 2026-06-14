package com.gentlemanstore.feature.product.data.dto

data class ProductResponse(
    val id: Long,
    val sku: String,
    val name: String,
    val description: String,
    val price: Double,
    val categoryName: String,
    val sizes: List<String>,
    val imageUrls: List<String>,
    val tags: List<String>
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