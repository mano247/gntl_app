package com.gentlemanstore.support.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketDTO {
    private Long id;
    private String subject;
    private String status;
    private LocalDateTime createdAt;
    private String userEmail;
    private Long sessionId;
    private Integer unreadCount = 0;
    // Ime kupca za staff chat header — namerno bez celog User entiteta.
    private String customerName;
    private String urgency;
    // Povezana porudžbina (nullable) — broj i status za staff prikaz.
    private Long orderId;
    private String orderStatus;
}
