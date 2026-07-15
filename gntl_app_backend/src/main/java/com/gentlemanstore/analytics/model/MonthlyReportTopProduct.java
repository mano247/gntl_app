package com.gentlemanstore.analytics.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "monthly_report_top_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyReportTopProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monthly_report_id", nullable = false)
    private MonthlyReport monthlyReport;

    // Snapshot naziva — namerno bez FK ka products (izveštaj ostaje potpun i
    // ako proizvod kasnije bude obrisan/preimenovan).
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "total_sold", nullable = false)
    private int totalSold;

    @Column(name = "sort_order", nullable = false)
    private int position;
}
