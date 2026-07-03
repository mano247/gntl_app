package com.gentlemanstore.support.dto;

import lombok.*;

/**
 * Realtime badge event — broadcast na /topic/user/{userId}/unread (customer)
 * ili /topic/employee/unread (staff) kada se promeni broj nepročitanih poruka
 * na tiketu. Klijent samo ažurira lokalni unreadCount, bez REST poziva.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnreadUpdateDTO {
    private Long ticketId;
    private Long sessionId;
    private Integer unreadCount;
}
