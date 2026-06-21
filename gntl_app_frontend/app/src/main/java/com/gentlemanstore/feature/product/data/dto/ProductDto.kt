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
    val discountPercentage: Double?
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