package com.gentlemanstore.feature.notification.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.util.ErrorMapper
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.notification.data.NotificationApiService
import com.gentlemanstore.feature.notification.data.dto.NotificationResponse
import com.gentlemanstore.feature.notification.data.dto.PagedNotificationResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationApiService: NotificationApiService
) {

    suspend fun getMyNotifications(page: Int, size: Int = 20): Resource<PagedNotificationResponse> {
        return try {
            val response = notificationApiService.getMyNotifications(page, size)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun markAsRead(id: Long): Resource<NotificationResponse> {
        return try {
            val response = notificationApiService.markAsRead(id)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }
}