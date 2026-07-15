package com.gentlemanstore.analytics.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Trajni snapshot analitike za jedan završen kalendarski mesec.
 * Unique constraint nad (report_year, report_month) garantuje da isti mesec
 * ne može dobiti dva izveštaja ni pri paralelnom pokretanju job-a.
 */
@Entity
@Table(name = "monthly_reports",
        uniqueConstraints = @UniqueConstraint(name = "uq_monthly_reports_year_month",
                columnNames = {"report_year", "report_month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_year", nullable = false)
    private int reportYear;

    @Column(name = "report_month", nullable = false)
    private int reportMonth;

    @Column(name = "total_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "total_orders", nullable = false)
    private int totalOrders;

    @Column(name = "new_users", nullable = false)
    private int newUsers;

    @Column(name = "average_order_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal averageOrderValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Snapshot top proizvoda za taj mesec — čuva se trajno, nezavisno od
    // kasnijih promena kataloga/porudžbina.
    @OneToMany(mappedBy = "monthlyReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<MonthlyReportTopProduct> topProducts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
