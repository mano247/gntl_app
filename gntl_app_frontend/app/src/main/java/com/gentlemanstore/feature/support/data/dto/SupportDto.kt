package com.gentlemanstore.feature.support.data.dto

data class SupportTicketResponse(
    val id: Long,
    val subject: String,
    val status: String,
    val createdAt: String,
    val userEmail: String,
    val sessionId: Long?,
    val unreadCount: Int = 0
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
    val subject: String
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