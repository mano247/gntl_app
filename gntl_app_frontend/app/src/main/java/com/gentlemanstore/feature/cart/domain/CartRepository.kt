package com.gentlemanstore.feature.cart.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.cart.data.CartApiService
import com.gentlemanstore.feature.cart.data.dto.AddToCartRequest
import com.gentlemanstore.feature.cart.data.dto.CartResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepository @Inject constructor(
    private val cartApiService: CartApiService
) {
    suspend fun getCart(): Resource<CartResponse> {
        return try {
            val response = cartApiService.getCart()
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load cart")
        }
    }

    suspend fun addToCart(
        productId: Long,
        productSizeId: Long,
        quantity: Int
    ): Resource<CartResponse> {
        return try {
            val response = cartApiService.addToCart(
                AddToCartRequest(productId, productSizeId, quantity)
            )
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add to cart")
        }
    }

    suspend fun removeFromCart(cartItemId: Long): Resource<CartResponse> {
        return try {
            val response = cartApiService.removeFromCart(cartItemId)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to remove item")
        }
    }

    suspend fun checkout(): Resource<Unit> {
        return try {
            val response = cartApiService.checkout()
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Checkout failed")
        }
    }
}