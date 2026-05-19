package com.gentlemanstore.analytics.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyRevenueDTO {
    private Integer month;
    private Integer year;
    private BigDecimal revenue;

}
