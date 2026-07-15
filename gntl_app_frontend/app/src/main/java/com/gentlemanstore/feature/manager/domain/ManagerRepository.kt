package com.gentlemanstore.feature.manager.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.network.toUnitResource
import com.gentlemanstore.core.util.ErrorMapper
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.manager.data.ManagerApiService
import com.gentlemanstore.feature.manager.data.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagerRepository @Inject constructor(
    private val managerApiService: ManagerApiService
) {

    suspend fun getDashboard(): Resource<AnalyticsResponse> {
        return try {
            val response = managerApiService.getDashboard()
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun getMonthlyReports(): Resource<List<MonthlyReportResponse>> {
        return try {
            val response = managerApiService.getMonthlyReports()
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun getAllDiscounts(): Resource<List<DiscountResponse>> {
        return try {
            val response = managerApiService.getAllDiscounts()
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun createDiscount(request: CreateDiscountRequest): Resource<DiscountResponse> {
        return try {
            val response = managerApiService.createDiscount(request)
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun deleteDiscount(id: Long): Resource<Unit> {
        return try {
            val response = managerApiService.deleteDiscount(id)
            response.toUnitResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun getAllPromotions(): Resource<List<PromotionResponse>> {
        return try {
            val response = managerApiService.getAllPromotions()
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun createPromotion(request: CreatePromotionRequest): Resource<PromotionResponse> {
        return try {
            val response = managerApiService.createPromotion(request)
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun deletePromotion(id: Long): Resource<Unit> {
        return try {
            val response = managerApiService.deletePromotion(id)
            response.toUnitResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

}