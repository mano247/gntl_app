package com.gentlemanstore.analytics.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsDTO {
    private BigDecimal totalRevenue;
    private Integer totalOrders;
    private Integer newUsers;
    private BigDecimal averageOrderValue;
    private List<MonthlyRevenueDTO> monthlyRevenue;
    private List<TopProductDTO> topProducts;
}
