package com.gentlemanstore.feature.notification.data

import com.gentlemanstore.core.network.ApiResponse
import com.gentlemanstore.feature.notification.data.dto.NotificationResponse
import com.gentlemanstore.feature.notification.data.dto.PagedNotificationResponse
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApiService {

    @GET("notifications/my")
    suspend fun getMyNotifications(
        @Query("page") page: Int,
        @Query("size") size: Int = 20
    ): ApiResponse<PagedNotificationResponse>

    @PUT("notifications/{id}")
    suspend fun markAsRead(
        @Path("id") id: Long
    ): ApiResponse<NotificationResponse>
}