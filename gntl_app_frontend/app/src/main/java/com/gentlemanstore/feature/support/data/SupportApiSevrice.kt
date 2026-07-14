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

    @DELETE("support/tickets/{id}")
    suspend fun deleteTicket(
        @Path("id") id: Long
    ): ApiResponse<Unit>

    @GET("support/tickets/{ticketId}/unread-count")
    suspend fun getUnreadCount(
        @Path("ticketId") ticketId: Long
    ): ApiResponse<Int>

    @PUT("support/messages/{sessionId}/read")
    suspend fun markMessagesAsRead(
        @Path("sessionId") sessionId: Long
    ): ApiResponse<Unit>

    @GET("support/tickets/unread-total")
    suspend fun getTotalUnreadCount(): ApiResponse<Int>

    // Unread brojaci po statusu tiketa (racunati nad SVIM tiketima na backendu,
    // ne nad jednom paginiranom stranicom) - za badge na status filter chipovima.
    @GET("support/tickets/my/unread-summary")
    suspend fun getMyUnreadSummary(): ApiResponse<Map<String, Int>>

    @GET("support/tickets/unread-summary")
    suspend fun getStaffUnreadSummary(): ApiResponse<Map<String, Int>>

    // Staff uklanja tiket iz staff liste (arhiviranje) - customer zadrzava istoriju
    @PUT("support/tickets/{id}/archive")
    suspend fun archiveTicket(
        @Path("id") id: Long
    ): ApiResponse<Unit>
}