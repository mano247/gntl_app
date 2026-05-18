package com.gentlemanstore.inventory.model;

import com.gentlemanstore.product.model.ProductSize;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer minQuantity;

    @Column(nullable = false)
    private boolean deleted = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_size_id", nullable = false)
    private ProductSize productSize;
}
