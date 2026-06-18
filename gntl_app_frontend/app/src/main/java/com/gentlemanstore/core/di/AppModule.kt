package com.gentlemanstore.core.di

import com.gentlemanstore.feature.address.data.AddressApiService
import com.gentlemanstore.feature.address.domain.AddressRepository
import com.gentlemanstore.feature.auth.data.AuthApiService
import com.gentlemanstore.feature.cart.data.CartApiService
import com.gentlemanstore.feature.cart.domain.CartRepository
import com.gentlemanstore.feature.loyalty.data.LoyaltyApiService
import com.gentlemanstore.feature.loyalty.domain.LoyaltyRepository
import com.gentlemanstore.feature.notification.data.NotificationApiService
import com.gentlemanstore.feature.notification.domain.NotificationRepository
import com.gentlemanstore.feature.order.data.OrderApiService
import com.gentlemanstore.feature.order.domain.OrderRepository
import com.gentlemanstore.feature.product.data.ProductApiService
import com.gentlemanstore.feature.product.domain.ProductRepository
import com.gentlemanstore.feature.profile.data.UserApiService
import com.gentlemanstore.feature.profile.domain.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideProductApiService(retrofit: Retrofit): ProductApiService {
        return retrofit.create(ProductApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideProductRepository(productApiService: ProductApiService): ProductRepository {
        return ProductRepository(productApiService)
    }

    @Provides
    @Singleton
    fun provideCartApiService(retrofit: Retrofit): CartApiService {
        return retrofit.create(CartApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCartRepository(cartApiService: CartApiService): CartRepository {
        return CartRepository(cartApiService)
    }

    @Provides
    @Singleton
    fun provideAddressApiService(retrofit: Retrofit): AddressApiService {
        return retrofit.create(AddressApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAddressRepository(addressApiService: AddressApiService): AddressRepository {
        return AddressRepository(addressApiService)
    }

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserRepository(userApiService: UserApiService): UserRepository {
        return UserRepository(userApiService)
    }

    @Provides
    @Singleton
    fun provideLoyaltyApiService(retrofit: Retrofit): LoyaltyApiService {
        return retrofit.create(LoyaltyApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideLoyaltyRepository(loyaltyApiService: LoyaltyApiService): LoyaltyRepository {
        return LoyaltyRepository(loyaltyApiService)
    }

    @Provides
    @Singleton
    fun provideOrderApiService(retrofit: Retrofit): OrderApiService {
        return retrofit.create(OrderApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOrderRepository(orderApiService: OrderApiService): OrderRepository {
        return OrderRepository(orderApiService)
    }

    @Provides
    @Singleton
    fun provideNotificationApiService(retrofit: Retrofit): NotificationApiService {
        return retrofit.create(NotificationApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(notificationApiService: NotificationApiService): NotificationRepository {
        return NotificationRepository(notificationApiService)
    }
}