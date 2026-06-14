package com.gentlemanstore.feature.product.data

import com.gentlemanstore.core.network.ApiResponse
import com.gentlemanstore.feature.product.data.dto.PagedProductResponse
import com.gentlemanstore.feature.product.data.dto.ProductResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApiService {
    @GET("products/paged")
    suspend fun getProducts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("category") category: String? = null,
        @Query("search") search: String? = null
    ): ApiResponse<PagedProductResponse>

    @GET("products/{id}")
    suspend fun getProductById(
        @Path("id") id: Long
    ): ApiResponse<ProductResponse>

    @GET("products/categories")
    suspend fun getCategories(): ApiResponse<List<String>>
}