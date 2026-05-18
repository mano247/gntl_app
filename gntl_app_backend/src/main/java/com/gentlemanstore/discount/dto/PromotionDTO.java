package com.gentlemanstore.discount.dto;

import lombok.*;

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
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String discountCode;
}
