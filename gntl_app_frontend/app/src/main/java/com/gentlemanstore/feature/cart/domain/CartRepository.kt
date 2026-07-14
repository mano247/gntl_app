package com.gentlemanstore.feature.cart.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.util.ErrorMapper
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.cart.data.CartApiService
import com.gentlemanstore.feature.cart.data.dto.AddToCartRequest
import com.gentlemanstore.feature.cart.data.dto.CartResponse
import com.gentlemanstore.feature.manager.data.dto.PromotionResponse
import com.gentlemanstore.feature.order.data.dto.CheckoutRequest
import com.gentlemanstore.feature.order.data.dto.OrderResponse
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
            Resource.Error(ErrorMapper.map(e.message))
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
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun removeFromCart(cartItemId: Long): Resource<CartResponse> {
        return try {
            val response = cartApiService.removeFromCart(cartItemId)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun checkout(addressId: Long, promoCode: String? = null): Resource<OrderResponse> {
        return try {
            val response = cartApiService.checkout(CheckoutRequest(addressId, promoCode))
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun validatePromoCode(code: String): Resource<PromotionResponse> {
        return try {
            val response = cartApiService.validatePromoCode(code)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }
}