package com.gentlemanstore.feature.support.data.dto

data class SupportTicketResponse(
    val id: Long,
    val subject: String,
    val status: String,
    val createdAt: String,
    val userEmail: String,
    val sessionId: Long?,
    val unreadCount: Int = 0,
    // Ime kupca za staff chat header
    val customerName: String? = null,
    // LOW / MEDIUM / HIGH
    val urgency: String? = null,
    // Povezana porudžbina (nullable)
    val orderId: Long? = null,
    val orderStatus: String? = null
)

data class PagedTicketResponse(
    val content: List<SupportTicketResponse>,
    val totalPages: Int,
    val totalElements: Long,
    val last: Boolean,
    val first: Boolean,
    val number: Int,
    val size: Int,
    val numberOfElements: Int
)

data class CreateTicketRequest(
    val subject: String,
    // Izabrana porudžbina iz My Orders liste (null = "Not related to an order")
    val orderId: Long? = null,
    // LOW / MEDIUM / HIGH — backend odbija bilo šta drugo
    val urgency: String? = null
)

data class ChatMessageResponse(
    val id: Long,
    val content: String,
    val sender: String,
    val sentAt: String
)

data class SendMessageRequest(
    val content: String,
    val sender: String
)

// Realtime badge event sa /topic/user/{userId}/unread ili /topic/employee/unread
data class UnreadUpdateEvent(
    val ticketId: Long,
    val sessionId: Long,
    val unreadCount: Int
)

data class BotQuestionResponse(
    val id: Long,
    val question: String,
    val orderIndex: Int
)

data class BotResponseResponse(
    val id: Long,
    val response: String,
    val question: String
)

data class SaveBotResponseRequest(
    val ticketId: Long,
    val questionId: Long,
    val response: String
)