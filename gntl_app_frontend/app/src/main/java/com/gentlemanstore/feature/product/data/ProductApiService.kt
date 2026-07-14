package com.gentlemanstore.feature.product.data

import com.gentlemanstore.core.network.ApiResponse
import com.gentlemanstore.feature.product.data.dto.CategoryResponse
import com.gentlemanstore.feature.product.data.dto.CreateProductRequest
import com.gentlemanstore.feature.product.data.dto.PagedProductResponse
import com.gentlemanstore.feature.product.data.dto.ProductResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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
    suspend fun getCategories(): ApiResponse<List<CategoryResponse>>

    // Employee/Admin CRUD
    @POST("products")
    suspend fun createProduct(
        @Body request: CreateProductRequest
    ): ApiResponse<ProductResponse>

    @PUT("products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Long,
        @Body request: CreateProductRequest
    ): ApiResponse<ProductResponse>

    @DELETE("products/{id}")
    suspend fun deleteProduct(
        @Path("id") id: Long
    ): ApiResponse<Unit>
}