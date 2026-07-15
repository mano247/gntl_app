package com.gentlemanstore.feature.support.domain

import com.gentlemanstore.core.network.toResource
import com.gentlemanstore.core.network.toUnitResource
import com.gentlemanstore.core.util.ErrorMapper
import com.gentlemanstore.core.util.Resource
import com.gentlemanstore.feature.support.data.SupportApiService
import com.gentlemanstore.feature.support.data.dto.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepository @Inject constructor(
    private val supportApiService: SupportApiService
) {

    suspend fun createTicket(
        subject: String,
        orderId: Long? = null,
        urgency: String? = null
    ): Resource<SupportTicketResponse> {
        return try {
            val response = supportApiService.createTicket(CreateTicketRequest(subject, orderId, urgency))
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun getMyTickets(page: Int, size: Int = 20): Resource<PagedTicketResponse> {
        return try {
            val response = supportApiService.getMyTickets(page, size)
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun getTicket(id: Long): Resource<SupportTicketResponse> {
        return try {
            val response = supportApiService.getTicket(id)
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun getMessages(sessionId: Long): Resource<List<ChatMessageResponse>> {
        return try {
            val response = supportApiService.getMessages(sessionId)
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun sendMessage(sessionId: Long, content: String, sender: String = "USER"): Resource<ChatMessageResponse> {
        return try {
            val response = supportApiService.sendMessage(
                sessionId = sessionId,
                request = SendMessageRequest(content = content, sender = sender)
            )
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun getBotQuestions(): Resource<List<BotQuestionResponse>> {
        return try {
            val response = supportApiService.getBotQuestions()
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun saveBotResponse(
        ticketId: Long,
        questionId: Long,
        response: String
    ): Resource<BotResponseResponse> {
        return try {
            val apiResponse = supportApiService.saveBotResponse(
                SaveBotResponseRequest(ticketId, questionId, response)
            )
            apiResponse.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun deleteTicket(ticketId: Long): Resource<Unit> {
        return try {
            val response = supportApiService.deleteTicket(ticketId)
            response.toUnitResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun getUnreadCount(ticketId: Long): Resource<Int> {
        return try {
            val response = supportApiService.getUnreadCount(ticketId)
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun markMessagesAsRead(sessionId: Long): Resource<Unit> {
        return try {
            val response = supportApiService.markMessagesAsRead(sessionId)
            response.toUnitResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun getTotalUnreadCount(): Resource<Int> {
        return try {
            val response = supportApiService.getTotalUnreadCount()
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun getMyUnreadSummary(): Resource<Map<String, Int>> {
        return try {
            val response = supportApiService.getMyUnreadSummary()
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun getStaffUnreadSummary(): Resource<Map<String, Int>> {
        return try {
            val response = supportApiService.getStaffUnreadSummary()
            response.toResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }

    suspend fun archiveTicket(ticketId: Long): Resource<Unit> {
        return try {
            val response = supportApiService.archiveTicket(ticketId)
            response.toUnitResource()
        } catch (e: Exception) {
            ErrorMapper.map(e)
        }
    }
}