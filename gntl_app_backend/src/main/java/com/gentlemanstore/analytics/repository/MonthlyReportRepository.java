package com.gentlemanstore.analytics.repository;

import com.gentlemanstore.analytics.model.MonthlyReport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {
    Optional<MonthlyReport> findByReportYearAndReportMonth(int reportYear, int reportMonth);

    boolean existsByReportYearAndReportMonth(int reportYear, int reportMonth);

    @EntityGraph(attributePaths = {"topProducts"})
    List<MonthlyReport> findAllByOrderByReportYearDescReportMonthDesc();
}
