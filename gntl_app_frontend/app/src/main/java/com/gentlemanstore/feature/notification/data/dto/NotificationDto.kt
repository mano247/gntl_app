package com.gentlemanstore.feature.notification.data.dto

data class NotificationResponse(
    val id: Long,
    val title: String,
    val message: String,
    val notificationType: String,
    val read: Boolean,
    val createdAt: String
)

data class PagedNotificationResponse(
    val content: List<NotificationResponse>,
    val totalPages: Int,
    val totalElements: Long,
    val last: Boolean,
    val first: Boolean,
    val number: Int,
    val size: Int,
    val numberOfElements: Int
)