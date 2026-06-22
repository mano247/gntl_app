package com.gentlemanstore.feature.notification.data.dto

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    val id: Long,
    val title: String,
    val message: String,
    val type: String,
    @SerializedName("read")
    val isRead: Boolean,
    val createdAt: String
)

data class PagedNotificationResponse(
    val content: List<NotificationResponse>,
    val totalElements: Int,
    val totalPages: Int
)