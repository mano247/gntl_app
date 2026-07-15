package com.gentlemanstore.analytics.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyReportDTO {
    private Long id;
    private int year;
    private int month;
    private BigDecimal totalRevenue;
    private int totalOrders;
    private int newUsers;
    private BigDecimal averageOrderValue;
    private List<TopProductDTO> topProducts;
    private LocalDateTime createdAt;
}
