package com.gentlemanstore.order.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private Long id;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;
    private BigDecimal loyaltyDiscount;
    private BigDecimal finalPrice;
    private BigDecimal promoDiscount;
    // Ime i email kupca za staff prikaz (Employee Orders) — namerno bez
    // celog User entiteta, samo minimalna polja.
    private String customerName;
    private String customerEmail;
}
