package com.gentlemanstore.analytics.service;

import com.gentlemanstore.analytics.dto.AnalyticsDTO;
import com.gentlemanstore.order.repository.OrderItemRepository;
import com.gentlemanstore.order.repository.OrderRepository;
import com.gentlemanstore.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    private AnalyticsService service(String fixedInstant) {
        Clock clock = Clock.fixed(Instant.parse(fixedInstant), ZoneId.of("Europe/Belgrade"));
        return new AnalyticsService(orderRepository, userRepository, orderItemRepository, clock);
    }

    @Test
    void dashboardIsScopedToCurrentMonthAndCarriesPeriod() {
        AnalyticsService service = service("2026-07-15T10:00:00Z");

        when(orderRepository.countOrdersBetween(any(), any())).thenReturn(5);
        when(orderRepository.getRevenueBetween(any(), any())).thenReturn(new BigDecimal("999.00"));
        when(orderRepository.getAverageOrderValueBetween(any(), any())).thenReturn(new BigDecimal("199.80"));
        when(userRepository.countNewUsersBetween(any(), any())).thenReturn(2);
        when(orderRepository.getMonthlyRevenue()).thenReturn(List.of());
        when(orderItemRepository.getTopProductsBetween(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{"Navy Suit", 3L}));

        AnalyticsDTO dto = service.getDashboardAnalytics();

        assertEquals(2026, dto.getYear(), "dashboard nosi period na koji se odnosi");
        assertEquals(7, dto.getMonth());
        assertEquals(new BigDecimal("999.00"), dto.getTotalRevenue());
        assertEquals(5, dto.getTotalOrders());
        assertEquals(2, dto.getNewUsers());
        assertEquals(1, dto.getTopProducts().size());

        // Opseg je tekući kalendarski mesec: [1. jul 00:00, 1. avgust 00:00)
        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).getRevenueBetween(fromCaptor.capture(), toCaptor.capture());
        assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0), fromCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 8, 1, 0, 0), toCaptor.getValue());
    }

    @Test
    void emptyCurrentMonthReturnsZeros() {
        AnalyticsService service = service("2026-07-15T10:00:00Z");

        when(orderRepository.countOrdersBetween(any(), any())).thenReturn(0);
        when(orderRepository.getRevenueBetween(any(), any())).thenReturn(null);
        when(orderRepository.getAverageOrderValueBetween(any(), any())).thenReturn(null);
        when(userRepository.countNewUsersBetween(any(), any())).thenReturn(0);
        when(orderRepository.getMonthlyRevenue()).thenReturn(List.of());
        when(orderItemRepository.getTopProductsBetween(any(), any(), any())).thenReturn(List.of());

        AnalyticsDTO dto = service.getDashboardAnalytics();

        assertEquals(BigDecimal.ZERO, dto.getTotalRevenue());
        assertEquals(BigDecimal.ZERO, dto.getAverageOrderValue());
        assertEquals(0, dto.getTotalOrders());
        assertTrue(dto.getTopProducts().isEmpty());
    }
}
