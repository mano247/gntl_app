package com.gentlemanstore.discount.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionDTO {
    private Long id;
    private String name;
    private String description;
    private String code;
    private String discountType;
    private BigDecimal value;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private boolean active;
}
