package com.gentlemanstore.inventory.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAlertDTO {
    private Long id;
    private String message;
    private boolean resolved;
    private LocalDateTime createdAt;
    private Long inventoryId;
}
