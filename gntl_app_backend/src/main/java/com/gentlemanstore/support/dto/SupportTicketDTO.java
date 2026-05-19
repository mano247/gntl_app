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
}
