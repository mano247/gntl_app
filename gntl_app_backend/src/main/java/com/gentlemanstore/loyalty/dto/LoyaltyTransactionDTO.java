package com.gentlemanstore.loyalty.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTransactionDTO {
    private Long id;
    private Integer points;
    private String description;
    private LocalDateTime createdAt;
}
