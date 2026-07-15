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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final Clock clock;

    /**
     * Dashboard metrike se odnose na TEKUĆI kalendarski mesec (year/month u
     * odgovoru) — istorija završenih meseci živi u mesečnim izveštajima
     * (MonthlyReportService). monthlyRevenue ostaje istorijska serija po
     * mesecima za grafikon.
     */
    @Transactional(readOnly = true)
    public AnalyticsDTO getDashboardAnalytics() {
        YearMonth currentMonth = YearMonth.now(clock);
        LocalDateTime from = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime to = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        Integer totalOrders = orderRepository.countOrdersBetween(from, to);

        BigDecimal totalRevenue = orderRepository.getRevenueBetween(from, to);
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        BigDecimal averageOrderValue = orderRepository.getAverageOrderValueBetween(from, to);
        if (averageOrderValue == null) {
            averageOrderValue = BigDecimal.ZERO;
        }

        Integer newUsers = userRepository.countNewUsersBetween(from, to);

        List<MonthlyRevenueDTO> monthlyRevenue = orderRepository.getMonthlyRevenue()
                .stream()
                .map(row -> MonthlyRevenueDTO.builder()
                        .month(((Number) row[0]).intValue())
                        .year(((Number) row[1]).intValue())
                        .revenue((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());

        List<TopProductDTO> topProducts = orderItemRepository
                .getTopProductsBetween(from, to, PageRequest.of(0, 5))
                .stream()
                .map(row -> TopProductDTO.builder()
                        .productName((String) row[0])
                        .totalSold(((Number) row[1]).intValue())
                        .build())
                .collect(Collectors.toList());

        return AnalyticsDTO.builder()
                .year(currentMonth.getYear())
                .month(currentMonth.getMonthValue())
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders == null ? 0 : totalOrders)
                .newUsers(newUsers == null ? 0 : newUsers)
                .averageOrderValue(averageOrderValue)
                .monthlyRevenue(monthlyRevenue)
                .topProducts(topProducts)
                .build();
    }
}
