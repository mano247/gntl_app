package com.gentlemanstore.feature.notification.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.network.toUnitResource
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
    suspend fun getNotifications(page: Int = 0, size: Int = 100): Resource<PagedNotificationResponse> {
        return try {
            val response = notificationApiService.getNotifications(page, size, "createdAt,desc")
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun getUnreadCount(): Resource<Int> {
        return try {
            val response = notificationApiService.getUnreadCount()
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun markAsRead(id: Long): Resource<Unit> {
        return try {
            val response = notificationApiService.markAsRead(id)
            response.toUnitResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun markAllAsRead(): Resource<Unit> {
        return try {
            val response = notificationApiService.markAllAsRead()
            response.toUnitResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun deleteNotification(id: Long): Resource<Unit> {
        return try {
            val response = notificationApiService.deleteNotification(id)
            response.toUnitResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun deleteAllNotifications(): Resource<Unit> {
        return try {
            val response = notificationApiService.deleteAllNotifications()
            response.toUnitResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }
}