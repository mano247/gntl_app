package com.gentlemanstore.discount.model;

import com.gentlemanstore.product.model.Category;
import com.gentlemanstore.product.model.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "discounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validTo;

    @Column(nullable = false)
    private boolean deleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

}
