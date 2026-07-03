package com.gentlemanstore.feature.notification.data

import com.gentlemanstore.core.network.ApiResponse
import com.gentlemanstore.feature.notification.data.dto.PagedNotificationResponse
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApiService {

    @GET("notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100,
        @Query("sort") sort: String = "createdAt,desc"
    ): ApiResponse<PagedNotificationResponse>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): ApiResponse<Int>

    @PUT("notifications/{id}/read")
    suspend fun markAsRead(
        @Path("id") id: Long
    ): ApiResponse<Unit>

    @PUT("notifications/read-all")
    suspend fun markAllAsRead(): ApiResponse<Unit>

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(
        @Path("id") id: Long
    ): ApiResponse<Unit>

    @DELETE("notifications")
    suspend fun deleteAllNotifications(): ApiResponse<Unit>
}