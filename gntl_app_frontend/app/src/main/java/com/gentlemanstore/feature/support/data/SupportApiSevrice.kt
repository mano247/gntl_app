package com.gentlemanstore.feature.support.data

import com.gentlemanstore.core.network.ApiResponse
import com.gentlemanstore.feature.support.data.dto.*
import retrofit2.http.*

interface SupportApiService {

    @POST("support/tickets")
    suspend fun createTicket(
        @Body request: CreateTicketRequest
    ): ApiResponse<SupportTicketResponse>

    @GET("support/tickets/my")
    suspend fun getMyTickets(
        @Query("page") page: Int,
        @Query("size") size: Int = 20
    ): ApiResponse<PagedTicketResponse>

    @GET("support/tickets/{id}")
    suspend fun getTicket(
        @Path("id") id: Long
    ): ApiResponse<SupportTicketResponse>

    @GET("support/messages/{sessionId}")
    suspend fun getMessages(
        @Path("sessionId") sessionId: Long
    ): ApiResponse<List<ChatMessageResponse>>

    @POST("support/messages/{sessionId}")
    suspend fun sendMessage(
        @Path("sessionId") sessionId: Long,
        @Body request: SendMessageRequest
    ): ApiResponse<ChatMessageResponse>

    @GET("support/bot")
    suspend fun getBotQuestions(): ApiResponse<List<BotQuestionResponse>>

    @POST("support/bot")
    suspend fun saveBotResponse(
        @Body request: SaveBotResponseRequest
    ): ApiResponse<BotResponseResponse>
}