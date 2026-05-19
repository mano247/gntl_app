package com.gentlemanstore.analytics.service;

import com.gentlemanstore.analytics.dto.AnalyticsDTO;
import com.gentlemanstore.analytics.dto.MonthlyRevenueDTO;
import com.gentlemanstore.analytics.dto.TopProductDTO;
import com.gentlemanstore.order.repository.OrderItemRepository;
import com.gentlemanstore.order.repository.OrderRepository;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public AnalyticsDTO getDashboardAnalytics() {
        Integer totalOrders = orderRepository.countTotalOrders();

        BigDecimal averageOrderValue = orderRepository.getAverageOrderValue();

        Integer newUsers = userRepository.countNewUsersThisMonth();

        List<MonthlyRevenueDTO> monthlyRevenue = orderRepository.getMonthlyRevenue()
                .stream()
                .map(row -> MonthlyRevenueDTO.builder()
                        .month(((Number) row[0]).intValue())
                        .year(((Number) row[1]).intValue())
                        .revenue((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());

        List<TopProductDTO> topProducts = orderItemRepository.getTopProducts(PageRequest.of(0, 5))
                .stream()
                .map(row -> TopProductDTO.builder()
                        .productName((String) row[0])
                        .totalSold(((Number) row[1]).intValue())
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalRevenue = monthlyRevenue.stream()
                .map(MonthlyRevenueDTO::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AnalyticsDTO.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .newUsers(newUsers)
                .averageOrderValue(averageOrderValue)
                .monthlyRevenue(monthlyRevenue)
                .topProducts(topProducts)
                .build();
    }
}
