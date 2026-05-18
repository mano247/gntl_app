package com.gentlemanstore.loyalty.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyAccountDTO {
    private Long id;
    private Integer points;
    private String tierName;
    private BigDecimal discountPercentage;
}
