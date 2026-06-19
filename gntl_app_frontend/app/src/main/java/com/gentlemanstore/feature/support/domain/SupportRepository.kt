package com.gentlemanstore.feature.support.domain

import com.gentlemanstore.core.network.toResource
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
            Resource.Error(e.message ?: "Failed to create ticket")
        }
    }

    suspend fun getMyTickets(page: Int, size: Int = 20): Resource<PagedTicketResponse> {
        return try {
            val response = supportApiService.getMyTickets(page, size)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load tickets")
        }
    }

    suspend fun getTicket(id: Long): Resource<SupportTicketResponse> {
        return try {
            val response = supportApiService.getTicket(id)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load ticket")
        }
    }

    suspend fun getMessages(sessionId: Long): Resource<List<ChatMessageResponse>> {
        return try {
            val response = supportApiService.getMessages(sessionId)
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load messages")
        }
    }

    suspend fun sendMessage(sessionId: Long, content: String): Resource<ChatMessageResponse> {
        return try {
            val response = supportApiService.sendMessage(
                sessionId = sessionId,
                request = SendMessageRequest(content = content, sender = "USER")
            )
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send message")
        }
    }

    suspend fun getBotQuestions(): Resource<List<BotQuestionResponse>> {
        return try {
            val response = supportApiService.getBotQuestions()
            response.toResource()
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load bot questions")
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
            Resource.Error(e.message ?: "Failed to save bot response")
        }
    }
}