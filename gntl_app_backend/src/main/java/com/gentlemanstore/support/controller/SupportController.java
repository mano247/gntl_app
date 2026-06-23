package com.gentlemanstore.support.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.support.dto.*;
import com.gentlemanstore.support.service.SupportService;
import com.gentlemanstore.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService service;

    @PostMapping("/tickets")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> createTicket(@Valid @RequestBody CreateTicketRequest request, @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Support ticket created successfully", service.createTicket(currentUser.getId(), request)));
    }

    @GetMapping("/tickets/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> getTicket(@PathVariable Long id, @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Support ticket retrieved successfully", service.getTicket(id, currentUser)));
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Page<SupportTicketDTO>>> getAllTickets(Pageable pageable){
        return ResponseEntity.ok(ApiResponse.success("Support tickets retrieved successfully", service.getAllTickets(pageable)));
    }

    @GetMapping("/tickets/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<SupportTicketDTO>>> getMyTickets(
            @AuthenticationPrincipal User currentUser,
            Pageable pageable){
        return ResponseEntity.ok(ApiResponse.success("Tickets retrieved successfully",
                service.getUserTickets(currentUser.getId(), pageable)));
    }

    @GetMapping("/tickets/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<SupportTicketDTO>>> getUserTickets(
            @PathVariable Long userId,
            Pageable pageable){
        return ResponseEntity.ok(ApiResponse.success("Tickets retrieved successfully",
                service.getUserTickets(userId, pageable)));
    }

    @PutMapping("/tickets/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> updateTicketStatus(@PathVariable Long id, @RequestBody String status){
        return ResponseEntity.ok(ApiResponse.success("Support ticket updated successfully", service.updateTicketStatus(id, status)));
    }

    @GetMapping("/messages/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ChatMessageDTO>>> getMessages(@PathVariable Long sessionId, @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", service.getMessages(sessionId, currentUser)));
    }

    @PostMapping("/messages/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ChatMessageDTO>> sendMessage(@PathVariable Long sessionId, @Valid @RequestBody SendMessageRequest request, @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", service.sendMessage(sessionId ,request.getContent(), request.getSender(), currentUser)));
    }

    @GetMapping("/bot")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<BotQuestionDTO>>> getBotQuestions(){
        return ResponseEntity.ok(ApiResponse.success("Bot questions retrieved successfully", service.getBotQuestions()));
    }

    @PostMapping("/bot")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<BotResponseDTO>> saveBotResponse(@Valid @RequestBody SaveBotResponseRequest request, @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Bot responses saved successfully", service.saveBotResponse(request.getTicketId(), request.getQuestionId(), request.getResponse(), currentUser)));
    }

    @DeleteMapping("/tickets/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        service.deleteTicket(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Ticket deleted successfully", null));
    }

    @GetMapping("/tickets/{ticketId}/unread-count")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved",
                service.getUnreadCount(ticketId, currentUser.getId())));
    }

    @PutMapping("/messages/{sessionId}/read")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markMessagesAsRead(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User currentUser) {
        service.markMessagesAsRead(sessionId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Messages marked as read", null));
    }

    @GetMapping("/tickets/unread-total")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> getTotalUnreadCount(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success("Total unread count retrieved",
                service.getTotalUnreadCount(currentUser.getId())));
    }
}
