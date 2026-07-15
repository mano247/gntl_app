package com.gentlemanstore.analytics.service;

import com.gentlemanstore.analytics.dto.MonthlyReportDTO;
import com.gentlemanstore.analytics.model.MonthlyReport;
import com.gentlemanstore.analytics.model.MonthlyReportTopProduct;
import com.gentlemanstore.analytics.repository.MonthlyReportRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MonthlyReportServiceTest {

    @Mock
    private MonthlyReportRepository monthlyReportRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    // Fiksiran "danas" = 2026-07-15 u Europe/Belgrade — testovi ne zavise od
    // stvarnog datuma.
    private static final ZoneId ZONE = ZoneId.of("Europe/Belgrade");

    private MonthlyReportService service(String fixedInstant) {
        Clock clock = Clock.fixed(Instant.parse(fixedInstant), ZONE);
        return new MonthlyReportService(
                monthlyReportRepository, orderRepository, userRepository, orderItemRepository, clock);
    }

    // ---------- snapshot završenog meseca ----------

    @Test
    void generateReportComputesSnapshotForCompletedMonth() {
        MonthlyReportService service = service("2026-07-15T10:00:00Z");
        java.time.YearMonth june = java.time.YearMonth.of(2026, 6);

        when(monthlyReportRepository.findByReportYearAndReportMonth(2026, 6))
                .thenReturn(Optional.empty());
        when(orderRepository.getRevenueBetween(any(), any())).thenReturn(new BigDecimal("12500.00"));
        when(orderRepository.countOrdersBetween(any(), any())).thenReturn(42);
        when(orderRepository.getAverageOrderValueBetween(any(), any())).thenReturn(new BigDecimal("297.62"));
        when(userRepository.countNewUsersBetween(any(), any())).thenReturn(7);
        when(orderItemRepository.getTopProductsBetween(any(), any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"Navy Suit", 11L},
                        new Object[]{"White Shirt", 9L}));
        when(monthlyReportRepository.save(any(MonthlyReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MonthlyReport report = service.generateReportForMonth(june);

        assertEquals(2026, report.getReportYear());
        assertEquals(6, report.getReportMonth());
        assertEquals(new BigDecimal("12500.00"), report.getTotalRevenue());
        assertEquals(42, report.getTotalOrders());
        assertEquals(7, report.getNewUsers());
        assertEquals(new BigDecimal("297.62"), report.getAverageOrderValue());
        assertEquals(2, report.getTopProducts().size());
        assertEquals("Navy Suit", report.getTopProducts().get(0).getProductName());
        assertEquals(1, report.getTopProducts().get(0).getPosition());
        assertEquals(11, report.getTopProducts().get(0).getTotalSold());
    }

    @Test
    void generateReportUsesExactMonthBoundaries() {
        MonthlyReportService service = service("2026-07-15T10:00:00Z");

        when(monthlyReportRepository.findByReportYearAndReportMonth(2026, 6))
                .thenReturn(Optional.empty());
        when(orderRepository.getRevenueBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.countOrdersBetween(any(), any())).thenReturn(0);
        when(orderRepository.getAverageOrderValueBetween(any(), any())).thenReturn(null);
        when(userRepository.countNewUsersBetween(any(), any())).thenReturn(0);
        when(orderItemRepository.getTopProductsBetween(any(), any(), any())).thenReturn(List.of());
        when(monthlyReportRepository.save(any(MonthlyReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.generateReportForMonth(java.time.YearMonth.of(2026, 6));

        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).getRevenueBetween(fromCaptor.capture(), toCaptor.capture());
        // Granice meseca: [1. jun 00:00, 1. jul 00:00) — poluotvoren interval,
        // porudžbina tačno u ponoć 1. jula pripada julu, ne junu.
        assertEquals(LocalDateTime.of(2026, 6, 1, 0, 0), fromCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0), toCaptor.getValue());
    }

    @Test
    void emptyMonthProducesZeroSnapshot() {
        MonthlyReportService service = service("2026-07-15T10:00:00Z");

        when(monthlyReportRepository.findByReportYearAndReportMonth(2026, 5))
                .thenReturn(Optional.empty());
        when(orderRepository.getRevenueBetween(any(), any())).thenReturn(null);
        when(orderRepository.countOrdersBetween(any(), any())).thenReturn(null);
        when(orderRepository.getAverageOrderValueBetween(any(), any())).thenReturn(null);
        when(userRepository.countNewUsersBetween(any(), any())).thenReturn(null);
        when(orderItemRepository.getTopProductsBetween(any(), any(), any())).thenReturn(List.of());
        when(monthlyReportRepository.save(any(MonthlyReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MonthlyReport report = service.generateReportForMonth(java.time.YearMonth.of(2026, 5));

        assertEquals(BigDecimal.ZERO, report.getTotalRevenue());
        assertEquals(0, report.getTotalOrders());
        assertEquals(0, report.getNewUsers());
        assertEquals(BigDecimal.ZERO, report.getAverageOrderValue());
        assertTrue(report.getTopProducts().isEmpty(), "prazan mesec = prazan top products snapshot");
    }

    // ---------- idempotentnost ----------

    @Test
    void existingReportIsNotDuplicated() {
        MonthlyReportService service = service("2026-07-15T10:00:00Z");
        MonthlyReport existing = MonthlyReport.builder()
                .reportYear(2026).reportMonth(6)
                .totalRevenue(BigDecimal.TEN).totalOrders(1).newUsers(0)
                .averageOrderValue(BigDecimal.TEN)
                .build();
        when(monthlyReportRepository.findByReportYearAndReportMonth(2026, 6))
                .thenReturn(Optional.of(existing));

        MonthlyReport report = service.generateReportForMonth(java.time.YearMonth.of(2026, 6));

        assertSame(existing, report, "isti mesec vraća postojeći izveštaj");
        verify(monthlyReportRepository, never()).save(any());
        verifyNoInteractions(orderRepository, userRepository, orderItemRepository);
    }

    // ---------- backfill nedostajućih meseci ----------

    @Test
    void missingReportsAreGeneratedUpToLastCompletedMonth() {
        // "Danas" je 15. jul 2026; prva aktivnost u aprilu 2026 →
        // izveštaji za april, maj i jun (jul je tekući i NE zatvara se).
        MonthlyReportService service = service("2026-07-15T10:00:00Z");

        when(orderRepository.getEarliestOrderDate())
                .thenReturn(LocalDateTime.of(2026, 4, 12, 9, 30));
        when(userRepository.getEarliestUserDate())
                .thenReturn(LocalDateTime.of(2026, 4, 20, 9, 30));
        // maj već ima izveštaj — preskače se
        when(monthlyReportRepository.findByReportYearAndReportMonth(anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    int month = invocation.getArgument(1);
                    if (month == 5) {
                        return Optional.of(MonthlyReport.builder()
                                .reportYear(2026).reportMonth(5)
                                .totalRevenue(BigDecimal.ZERO).totalOrders(0).newUsers(0)
                                .averageOrderValue(BigDecimal.ZERO)
                                .build());
                    }
                    return Optional.empty();
                });
        when(orderRepository.getRevenueBetween(any(), any())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.countOrdersBetween(any(), any())).thenReturn(0);
        when(orderRepository.getAverageOrderValueBetween(any(), any())).thenReturn(null);
        when(userRepository.countNewUsersBetween(any(), any())).thenReturn(0);
        when(orderItemRepository.getTopProductsBetween(any(), any(), any())).thenReturn(List.of());
        when(monthlyReportRepository.save(any(MonthlyReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.generateMissingReports();

        ArgumentCaptor<MonthlyReport> captor = ArgumentCaptor.forClass(MonthlyReport.class);
        verify(monthlyReportRepository, times(2)).save(captor.capture());
        List<Integer> savedMonths = captor.getAllValues().stream()
                .map(MonthlyReport::getReportMonth)
                .toList();
        assertEquals(List.of(4, 6), savedMonths,
                "generišu se samo april i jun (maj postoji, jul je tekući mesec)");
    }

    @Test
    void noActivityMeansNoReports() {
        MonthlyReportService service = service("2026-07-15T10:00:00Z");
        when(orderRepository.getEarliestOrderDate()).thenReturn(null);
        when(userRepository.getEarliestUserDate()).thenReturn(null);

        service.generateMissingReports();

        verify(monthlyReportRepository, never()).save(any());
    }

    // ---------- istorija za manager-a ----------

    @Test
    void getAllReportsMapsEntitiesToDTOs() {
        MonthlyReportService service = service("2026-07-15T10:00:00Z");
        MonthlyReport report = MonthlyReport.builder()
                .id(1L).reportYear(2026).reportMonth(6)
                .totalRevenue(new BigDecimal("100.00")).totalOrders(3).newUsers(2)
                .averageOrderValue(new BigDecimal("33.33"))
                .createdAt(LocalDateTime.of(2026, 7, 1, 0, 0))
                .build();
        report.getTopProducts().add(MonthlyReportTopProduct.builder()
                .monthlyReport(report).productName("Navy Suit").totalSold(2).position(1)
                .build());
        when(monthlyReportRepository.findAllByOrderByReportYearDescReportMonthDesc())
                .thenReturn(List.of(report));

        List<MonthlyReportDTO> dtos = service.getAllReports();

        assertEquals(1, dtos.size());
        MonthlyReportDTO dto = dtos.get(0);
        assertEquals(2026, dto.getYear());
        assertEquals(6, dto.getMonth());
        assertEquals(new BigDecimal("100.00"), dto.getTotalRevenue());
        assertEquals(1, dto.getTopProducts().size());
        assertEquals("Navy Suit", dto.getTopProducts().get(0).getProductName());
    }
}
