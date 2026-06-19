package com.gentlemanstore.discount.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountDTO {
    private Long id;
    private String code;
    private String discountType;
    private BigDecimal value;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Long productId;
    private String productName;
    private Long categoryId;
    private String categoryName;
}
