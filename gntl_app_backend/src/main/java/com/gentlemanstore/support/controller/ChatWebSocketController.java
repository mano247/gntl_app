package com.gentlemanstore.support.controller;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.support.dto.SendMessageRequest;
import com.gentlemanstore.support.service.SupportService;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP kontroler za chat poruke. Klijent šalje na /app/chat/{sessionId}/send;
 * poruka se čuva kroz postojeći SupportService.sendMessage(), koji je i
 * broadcast-uje na /topic/chat/{sessionId} (isti tok kao REST slanje).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SupportService supportService;
    private final UserRepository userRepository;

    @MessageMapping("/chat/{sessionId}/send")
    public void sendMessage(@DestinationVariable Long sessionId,
                            @Payload SendMessageRequest request,
                            Principal principal) {
        if (principal == null) {
            // Handshake interceptor garantuje autentifikaciju; ovo je defanzivna provera.
            return;
        }
        // Svež lookup iz baze (kao JwtAuthenticationFilter) — principal iz handshake-a
        // može biti star koliko i konekcija.
        User user = userRepository.findByEmailAndDeletedFalse(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        supportService.sendMessage(sessionId, request.getContent(), request.getSender(), user);
    }

    @MessageExceptionHandler
    public void handleException(Exception e) {
        log.warn("WebSocket chat message failed: {}", e.getMessage());
    }
}
