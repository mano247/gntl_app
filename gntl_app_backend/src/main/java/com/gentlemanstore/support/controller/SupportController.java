package com.gentlemanstore.support.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.support.dto.*;
import com.gentlemanstore.support.service.SupportService;
import com.gentlemanstore.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<ApiResponse<SupportTicketDTO>> getTicket(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("Support ticket retrieved successfully", service.getTicket(id)));
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<SupportTicketDTO>>> getAllTickets(){
        return ResponseEntity.ok(ApiResponse.success("Support tickets retrieved successfully", service.getAllTickets()));
    }

    @GetMapping("/tickets/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<SupportTicketDTO>>> getUserTickets(@PathVariable Long userId){
        return ResponseEntity.ok(ApiResponse.success("Users support tickets retrieved successfully", service.getUserTickets(userId)));
    }

    @GetMapping("/tickets/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<SupportTicketDTO>>> getMyTickets(
            @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Tickets retrieved successfully",
                service.getUserTickets(currentUser.getId())));
    }

    @PutMapping("/tickets/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> updateTicketStatus(@PathVariable Long id, @RequestBody String status){
        return ResponseEntity.ok(ApiResponse.success("Support ticket updated successfully", service.updateTicketStatus(id, status)));
    }

    @GetMapping("/messages/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ChatMessageDTO>>> getMessages(@PathVariable Long sessionId){
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", service.getMessages(sessionId)));
    }

    @PostMapping("/messages/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ChatMessageDTO>> sendMessage(@PathVariable Long sessionId, @Valid @RequestBody SendMessageRequest request){
        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", service.sendMessage(sessionId ,request.getContent(), request.getSender())));
    }

    @GetMapping("/bot")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<BotQuestionDTO>>> getBotQuestions(){
        return ResponseEntity.ok(ApiResponse.success("Bot questions retrieved successfully", service.getBotQuestions()));
    }

    @PostMapping("/bot")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<BotResponseDTO>> saveBotResponse(@Valid @RequestBody SaveBotResponseRequest request){
        return ResponseEntity.ok(ApiResponse.success("Bot responses saved successfully", service.saveBotResponse(request.getTicketId(), request.getQuestionId(), request.getResponse())));
    }
}
