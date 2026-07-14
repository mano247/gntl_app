package com.gentlemanstore.feature.product.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.network.toUnitResource
import com.gentlemanstore.core.util.ErrorMapper
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.product.data.ProductApiService
import com.gentlemanstore.feature.product.data.dto.CategoryResponse
import com.gentlemanstore.feature.product.data.dto.CreateProductRequest
import com.gentlemanstore.feature.product.data.dto.PagedProductResponse
import com.gentlemanstore.feature.product.data.dto.ProductResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val productApiService: ProductApiService
) {
    suspend fun getProducts(
        page: Int = 0,
        size: Int = 20,
        category: String? = null,
        search: String? = null
    ): Resource<PagedProductResponse> {
        return try {
            val response = productApiService.getProducts(page, size, category, search)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun getProductById(id: Long): Resource<ProductResponse> {
        return try {
            val response = productApiService.getProductById(id)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun getCategories(): Resource<List<CategoryResponse>> {
        return try {
            val response = productApiService.getCategories()
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun createProduct(request: CreateProductRequest): Resource<ProductResponse> {
        return try {
            val response = productApiService.createProduct(request)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun updateProduct(id: Long, request: CreateProductRequest): Resource<ProductResponse> {
        return try {
            val response = productApiService.updateProduct(id, request)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun deleteProduct(id: Long): Resource<Unit> {
        return try {
            val response = productApiService.deleteProduct(id)
            response.toUnitResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }
}