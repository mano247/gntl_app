package com.gentlemanstore.analytics.service;

import com.gentlemanstore.analytics.dto.MonthlyReportDTO;
import com.gentlemanstore.analytics.dto.TopProductDTO;
import com.gentlemanstore.analytics.model.MonthlyReport;
import com.gentlemanstore.analytics.model.MonthlyReportTopProduct;
import com.gentlemanstore.analytics.repository.MonthlyReportRepository;
import com.gentlemanstore.order.repository.OrderItemRepository;
import com.gentlemanstore.order.repository.OrderRepository;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generisanje trajnih mesečnih analytics izveštaja.
 *
 * Determinističko i idempotentno: izveštaj za mesec se računa iz podataka u
 * bazi za taj kalendarski mesec i pravi se najviše jednom (unique constraint
 * nad year/month + provera postojanja). Ponovno pokretanje ne pravi duplikate.
 *
 * Pokretanje:
 *  - @Scheduled cron svakog 1. u mesecu u 00:00 (server timezone) zatvara
 *    prethodni mesec;
 *  - catch-up pri startu aplikacije pokriva slučaj kada backend nije bio
 *    pokrenut tačno u ponoć — nedostajući izveštaji se naprave naknadno.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyReportService {

    private final MonthlyReportRepository monthlyReportRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final Clock clock;

    private static final int TOP_PRODUCTS_LIMIT = 5;

    @Scheduled(cron = "0 0 0 1 * *")
    public void closeCompletedMonths() {
        generateMissingReports();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            generateMissingReports();
        } catch (Exception e) {
            // Startup ne sme pasti zbog izveštaja — sledeći cron pokušava ponovo.
            log.warn("Monthly report catch-up failed on startup: {}", e.getMessage());
        }
    }

    /**
     * Pravi izveštaje za sve završene mesece koji ih još nemaju, od prvog
     * meseca sa aktivnošću (porudžbina ili registracija) do prethodnog meseca.
     */
    @Transactional
    public void generateMissingReports() {
        YearMonth firstMonth = findEarliestActivityMonth();
        if (firstMonth == null) {
            return;
        }
        YearMonth lastCompleted = YearMonth.now(clock).minusMonths(1);
        for (YearMonth month = firstMonth; !month.isAfter(lastCompleted); month = month.plusMonths(1)) {
            generateReportForMonth(month);
        }
    }

    /**
     * Idempotentno pravi (ili vraća postojeći) izveštaj za dati mesec.
     * Prazan mesec je validan — snapshot sa nulama.
     */
    @Transactional
    public MonthlyReport generateReportForMonth(YearMonth month) {
        return monthlyReportRepository
                .findByReportYearAndReportMonth(month.getYear(), month.getMonthValue())
                .orElseGet(() -> createReport(month));
    }

    private MonthlyReport createReport(YearMonth month) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();

        MonthlyReport report = MonthlyReport.builder()
                .reportYear(month.getYear())
                .reportMonth(month.getMonthValue())
                .totalRevenue(nvl(orderRepository.getRevenueBetween(from, to)))
                .totalOrders(nvl(orderRepository.countOrdersBetween(from, to)))
                .newUsers(nvl(userRepository.countNewUsersBetween(from, to)))
                .averageOrderValue(nvl(orderRepository.getAverageOrderValueBetween(from, to)))
                .build();

        List<Object[]> topRows = orderItemRepository.getTopProductsBetween(
                from, to, PageRequest.of(0, TOP_PRODUCTS_LIMIT));
        for (int i = 0; i < topRows.size(); i++) {
            Object[] row = topRows.get(i);
            report.getTopProducts().add(MonthlyReportTopProduct.builder()
                    .monthlyReport(report)
                    .productName((String) row[0])
                    .totalSold(((Number) row[1]).intValue())
                    .position(i + 1)
                    .build());
        }

        try {
            MonthlyReport saved = monthlyReportRepository.save(report);
            log.info("Monthly report created for {}: revenue={}, orders={}",
                    month, saved.getTotalRevenue(), saved.getTotalOrders());
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Paralelno pokretanje (dve instance / cron + startup) — unique
            // constraint je presudio, pročitaj pobednika.
            log.info("Monthly report for {} already created concurrently", month);
            return monthlyReportRepository
                    .findByReportYearAndReportMonth(month.getYear(), month.getMonthValue())
                    .orElseThrow(() -> e);
        }
    }

    @Transactional(readOnly = true)
    public List<MonthlyReportDTO> getAllReports() {
        return monthlyReportRepository.findAllByOrderByReportYearDescReportMonthDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private MonthlyReportDTO toDTO(MonthlyReport report) {
        return MonthlyReportDTO.builder()
                .id(report.getId())
                .year(report.getReportYear())
                .month(report.getReportMonth())
                .totalRevenue(report.getTotalRevenue())
                .totalOrders(report.getTotalOrders())
                .newUsers(report.getNewUsers())
                .averageOrderValue(report.getAverageOrderValue())
                .createdAt(report.getCreatedAt())
                .topProducts(report.getTopProducts().stream()
                        .map(tp -> TopProductDTO.builder()
                                .productName(tp.getProductName())
                                .totalSold(tp.getTotalSold())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    // Prvi mesec sa bilo kakvom aktivnošću — od njega kreće backfill.
    private YearMonth findEarliestActivityMonth() {
        LocalDateTime earliestOrder = orderRepository.getEarliestOrderDate();
        LocalDateTime earliestUser = userRepository.getEarliestUserDate();
        LocalDateTime earliest = min(earliestOrder, earliestUser);
        return earliest == null ? null : YearMonth.from(earliest);
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
