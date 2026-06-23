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

    suspend fun createTicket(subject: String): Resource<SupportTicketResponse> {
        return try {
            val response = supportApiService.createTicket(CreateTicketRequest(subject))
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun getMyTickets(page: Int, size: Int = 20): Resource<PagedTicketResponse> {
        return try {
            val response = supportApiService.getMyTickets(page, size)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun getTicket(id: Long): Resource<SupportTicketResponse> {
        return try {
            val response = supportApiService.getTicket(id)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun getMessages(sessionId: Long): Resource<List<ChatMessageResponse>> {
        return try {
            val response = supportApiService.getMessages(sessionId)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
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
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun getBotQuestions(): Resource<List<BotQuestionResponse>> {
        return try {
            val response = supportApiService.getBotQuestions()
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
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
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun deleteTicket(ticketId: Long): Resource<Unit> {
        return try {
            val response = supportApiService.deleteTicket(ticketId)
            response.toUnitResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun getUnreadCount(ticketId: Long): Resource<Int> {
        return try {
            val response = supportApiService.getUnreadCount(ticketId)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }

    suspend fun markMessagesAsRead(sessionId: Long): Resource<Unit> {
        return try {
            val response = supportApiService.markMessagesAsRead(sessionId)
            response.toUnitResource()
        } catch (e: Exception) {
            Resource.Error(ErrorMapper.map(e.message))
        }
    }
}